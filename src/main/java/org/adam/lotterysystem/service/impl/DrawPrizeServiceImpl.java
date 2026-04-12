package org.adam.lotterysystem.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import lombok.Data;
import org.adam.lotterysystem.common.errorcode.ServiceErrorCodeConstants;
import org.adam.lotterysystem.common.exception.ServiceException;
import org.adam.lotterysystem.common.utils.JacksonUtil;
import org.adam.lotterysystem.common.utils.MailUtil;
import org.adam.lotterysystem.common.utils.RedisUtil;
import org.adam.lotterysystem.controller.param.DrawPrizeParam;
import org.adam.lotterysystem.controller.param.ExecuteDrawPrizeParam;
import org.adam.lotterysystem.controller.param.ShowWinningRecordsParam;
import org.adam.lotterysystem.dao.dataobject.*;
import org.adam.lotterysystem.dao.mapper.*;
import org.adam.lotterysystem.service.ActivityService;
import org.adam.lotterysystem.service.DrawPrizeService;
import org.adam.lotterysystem.service.activitystatus.ActivityStatusManager;
import org.adam.lotterysystem.service.dto.DrawPrizeDTO;
import org.adam.lotterysystem.service.dto.ConvertActivityStatusDTO;
import org.adam.lotterysystem.service.dto.WinningRecordDTO;
import org.adam.lotterysystem.service.enums.ActivityPrizeStatusEnum;
import org.adam.lotterysystem.service.enums.ActivityPrizeTiersEnum;
import org.adam.lotterysystem.service.enums.ActivityStatusEnum;
import org.adam.lotterysystem.service.enums.ActivityUSerStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

import static org.adam.lotterysystem.common.config.DirectRabbitConfig.EXCHANGE_NAME;
import static org.adam.lotterysystem.common.config.DirectRabbitConfig.ROUTING;

@Service
public class DrawPrizeServiceImpl implements DrawPrizeService {

    private static final Logger logger = LoggerFactory.getLogger(DrawPrizeServiceImpl.class);
    private static final String DRAW_PROCESSING_KEY_PREFIX = "DrawPrizeExecuting_";
    private static final Long DRAW_PROCESSING_LOCK_EXPIRE_SECONDS = 30L;
    private final String WINNING_RECORDS_CACHE_KEY_PREFIX = "WinningRecords_"; // 中奖记录缓存 key 前缀
    private final Long WINNING_RECORD_CACHE_EXPIRE = 60L * 60L * 24L * 2L; // 中奖记录缓存过期时间，单位：秒
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private ActivityPrizeMapper activityPrizeMapper;
    @Autowired
    private ActivityUserMapper activityUserMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PrizeMapper prizeMapper;
    @Autowired
    private WinningRecordMapper winningRecordMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private ActivityStatusManager activityStatusManager;
    @Autowired
    private ActivityService activityService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    @Qualifier("asyncServiceExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Autowired
    private MailUtil mailUtil;

    @Override
    public void drawPrize(DrawPrizeParam param) {

        Map<String, String> map = new HashMap<>();
        map.put("messageId", String.valueOf(UUID.randomUUID()));
        map.put("messageData", JacksonUtil.writeValueAsString(param));
        // 发送消息 : 交换机，绑定的key，消息体
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING, map);
        logger.info("抽奖消息发送成功: {}", JacksonUtil.writeValueAsString(map));
    }

    @Override
    public DrawPrizeDTO executeDraw(ExecuteDrawPrizeParam param) {
        String lockKey = buildDrawProcessingKey(param.getActivityId(), param.getPrizeId());
        String lockValue = UUID.randomUUID().toString();
        DrawPrizeParam[] rollbackParamHolder = new DrawPrizeParam[1];
        if (!redisUtil.setIfAbsent(lockKey, lockValue, DRAW_PROCESSING_LOCK_EXPIRE_SECONDS)) {
            throw new ServiceException(ServiceErrorCodeConstants.DRAW_PRIZE_PROCESSING);
        }
        try {
            DrawExecutionContext executionContext = transactionTemplate.execute(status -> {
                ActivityDO activityDO = activityMapper.selectById(param.getActivityId());
                ActivityPrizeDO activityPrizeDO = activityPrizeMapper
                        .selectByActivityPrizeIdForUpdate(param.getActivityId(), param.getPrizeId());
                validateExecuteDrawRequest(activityDO, activityPrizeDO);

                List<ActivityUserDO> candidates = activityUserMapper
                        .selectByActivityIdAndStatusForUpdate(param.getActivityId(), ActivityUSerStatusEnum.INIT.name());
                if (candidates.size() < activityPrizeDO.getPrizeAmount()) {
                    throw new ServiceException(ServiceErrorCodeConstants.DRAW_PRIZE_CANDIDATE_NOT_ENOUGH);
                }

                DrawPrizeParam drawPrizeParam = buildServerSelectedDrawParam(param, activityPrizeDO, candidates);
                rollbackParamHolder[0] = drawPrizeParam;
                List<WinningRecordDO> winningRecordDOS = finalizeDrawInTransaction(drawPrizeParam);

                DrawExecutionContext context = new DrawExecutionContext();
                context.setDrawPrizeParam(drawPrizeParam);
                context.setWinningRecordDOS(winningRecordDOS);
                return context;
            });
            if (executionContext == null) {
                throw new IllegalStateException("draw execution context should not be null");
            }
            notifyWinnersAsync(executionContext.getWinningRecordDOS());
            return buildDrawPrizeDTO(executionContext.getDrawPrizeParam(), executionContext.getWinningRecordDOS());
        } catch (RuntimeException ex) {
            cleanupAfterDrawFailure(rollbackParamHolder[0], param.getActivityId(), param.getPrizeId());
            throw ex;
        } finally {
            redisUtil.delIfMatch(lockKey, lockValue);
        }
    }

    @Override
    public List<WinningRecordDO> finalizeDraw(DrawPrizeParam param) {
        try {
            List<WinningRecordDO> winningRecordDOS = transactionTemplate.execute(status -> finalizeDrawInTransaction(param));
            if (winningRecordDOS == null) {
                throw new IllegalStateException("winning record list should not be null");
            }
            notifyWinnersAsync(winningRecordDOS);
            return winningRecordDOS;
        } catch (RuntimeException ex) {
            cleanupAfterDrawFailure(param, param.getActivityId(), param.getPrizeId());
            throw ex;
        }
    }

    @Override
    public Boolean checkDrawPrizeStatus(DrawPrizeParam param) {

        ActivityDO activityDO = activityMapper.selectById(param.getActivityId());
        // 奖品是否存在可以在 activity_prize 表中查询(在保存activity做了本地事务保证了奖品和活动的关系，所以这里不需要单独查询奖品表了)
        ActivityPrizeDO activityPrizeDO = activityPrizeMapper.selectByActivityPrizeId(
                param.getActivityId(), param.getPrizeId());
        // 判断活动活着奖品是否存在
        if (null == activityDO || null == activityPrizeDO) {
//            throw new ServiceException(ServiceErrorCodeConstants.DRAW_PRIZE_PARAM_ERROR);
            logger.info("校验抽奖请求失败！：{}", ServiceErrorCodeConstants.DRAW_PRIZE_PARAM_ERROR.getMsg());
            return false;
        }
        // 判断活动是否有效
        if (activityDO.getStatus().equalsIgnoreCase(ActivityStatusEnum.END.name())) {
//            throw new ServiceException(ServiceErrorCodeConstants.DRAW_ACTIVITY_END);
            logger.info("校验抽奖请求失败！：{}", ServiceErrorCodeConstants.DRAW_PRIZE_PARAM_ERROR.getMsg());
            return false;
        }
        // 奖品是否有效
        if (activityPrizeDO.getStatus().equalsIgnoreCase(ActivityPrizeStatusEnum.END.name())) {
//            throw new ServiceException(ServiceErrorCodeConstants.DRAW_PRIZE_END);
            logger.info("校验抽奖请求失败！：{}", ServiceErrorCodeConstants.DRAW_PRIZE_PARAM_ERROR.getMsg());
            return false;
        }
        // 中奖者人数是否和中奖者列表人数一致
        if (activityPrizeDO.getPrizeAmount() != param.getWinnerList().size()) {
//            throw new ServiceException(ServiceErrorCodeConstants.DRAW_PRIZE_WINNER_LIST_ERROR);
            logger.info("校验抽奖请求失败！：{}", ServiceErrorCodeConstants.DRAW_PRIZE_WINNER_LIST_ERROR.getMsg());
            return false;
        }
        return true;
    }

    @Override
    public List<WinningRecordDO> saveWinnerRecords(DrawPrizeParam param) {
        // 查询相关信息：活动、人员、奖品、活动关联奖品表
        ActivityDO activityDO = activityMapper.selectById(param.getActivityId());
        List<UserDO> userDOS = userMapper.batchSelectByIds(param.getWinnerList()
                .stream()
                .map(DrawPrizeParam.Winner::getUserId)
                .collect(Collectors.toList()));
        PrizeDO prizeDO = prizeMapper.selectExistById(param.getPrizeId());
        ActivityPrizeDO activityPrizeDO = activityPrizeMapper.selectByActivityPrizeId(param.getActivityId(), param.getPrizeId());
        // 构造中奖者记录
        List<WinningRecordDO> winningRecordDOS = userDOS.stream()
                .map(userDO -> {
                    WinningRecordDO winningRecordDO = new WinningRecordDO();
                    winningRecordDO.setActivityId(activityDO.getId());
                    winningRecordDO.setActivityName(activityDO.getActivityName());
                    winningRecordDO.setPrizeId(prizeDO.getId());
                    winningRecordDO.setPrizeName(prizeDO.getName());
                    winningRecordDO.setPrizeTier(activityPrizeDO.getPrizeTiers());
                    winningRecordDO.setWinnerId(userDO.getId());
                    winningRecordDO.setWinnerName(userDO.getUserName());
                    winningRecordDO.setWinnerPhoneNumber(userDO.getPhoneNumber());
                    winningRecordDO.setWinnerEmail(userDO.getEmail());
                    winningRecordDO.setWinningTime(param.getWinningTime());
                    return winningRecordDO;
                })
                .collect(Collectors.toList());
        winningRecordMapper.batchInsert(winningRecordDOS);
        // 缓存中奖者记录
        // 1. 缓存奖品维度中奖记录(WinningRecord_activityId_prizeId, winningRecordDOS(奖品维度的中奖名单))
        cacheWinningRecords(param.getActivityId() + "_" + param.getPrizeId(), winningRecordDOS, WINNING_RECORD_CACHE_EXPIRE);
        // 2. 缓存活动维度中奖记录(WinningRecord_activityId, winningRecordDOS(活动维度的中奖名单))
        // 当活动已完成的时候，才会缓存活动维度的中奖记录，因为活动维度的中奖记录是需要在抽奖过程中不断更新的，
        // 如果活动未完成，频繁更新活动维度的中奖记录会增加缓存压力
        if (activityDO.getStatus()
                .equalsIgnoreCase(ActivityStatusEnum.END.name())) {
            // 查询活动维度的全量中奖记录
            List<WinningRecordDO> allList = winningRecordMapper.selectByActivityId(param.getActivityId());
            cacheWinningRecords(String.valueOf(param.getActivityId()), allList, WINNING_RECORD_CACHE_EXPIRE);
        }
        return winningRecordDOS;
    }

    /**
     * 删除中奖者记录
     * @param activityId
     * @param prizeId
     */
    @Override
    public void deleteWinnerRecords(Long activityId, Long prizeId) {
        if (null == activityId) {
            logger.warn("要删除中奖者记录的活动id为空！");
            return;
        }
        // 删除数据库中奖者记录
        winningRecordMapper.deleteByActivityIdAndPrizeId(activityId, prizeId);
        // 删除缓存(奖品维度，活动维度)
        if (null != prizeId) {
            deleteWinningRecordsCache(activityId + "_" + prizeId);
        }
        // 无论是否传递了 prizeId，都要删除活动维度的中奖记录缓存
        // 因为活动维度的中奖记录缓存中包含了奖品维度的中奖记录，所以无论是否传递了 prizeId，都要删除活动维度的中奖记录缓存
        deleteWinningRecordsCache(String.valueOf(activityId));
    }

    /**
     * 获取中奖记录
     * @param param
     * @return
     */
    @Override
    public List<WinningRecordDTO> getRecords(ShowWinningRecordsParam param) {
        // 先从缓存中获取中奖记录（奖品、活动）
        String key = null == param.getPrizeId()
                ? String.valueOf(param.getActivityId())
                : param.getActivityId() + "_" + param.getPrizeId();
        List<WinningRecordDO> cacheWinningRecords = getCacheWinningRecords(key);
        if (!CollectionUtils.isEmpty(cacheWinningRecords)) {
            return convertToWinningRecordDTOS(cacheWinningRecords);
        }
        // 如果缓存中没有，再从数据库中获取，并且更新缓存
        cacheWinningRecords = winningRecordMapper.selectByActivityIdOrPrizeId(param.getActivityId(), param.getPrizeId());
        // 存放记录到缓存中
        if (CollectionUtils.isEmpty(cacheWinningRecords)) {
            logger.info("查询的中奖记录为空！ param:{}", JacksonUtil.writeValueAsString(param));
            return Arrays.asList();
        }
        cacheWinningRecords(key, cacheWinningRecords, WINNING_RECORD_CACHE_EXPIRE);
        return convertToWinningRecordDTOS(cacheWinningRecords);
    }

    /**
     * 将 WinningRecordDO 列表转换为 WinningRecordDTO 列表
     * @param cacheWinningRecords
     * @return
     */
    private List<WinningRecordDTO> convertToWinningRecordDTOS(List<WinningRecordDO> cacheWinningRecords) {
        if (CollectionUtils.isEmpty(cacheWinningRecords)) {
            return Arrays.asList();
        }
        return cacheWinningRecords.stream()
                .map(winningRecordDO -> {
                    WinningRecordDTO winningRecordDTO = new WinningRecordDTO();
                    winningRecordDTO.setWinnerId(winningRecordDO.getWinnerId());
                    winningRecordDTO.setWinnerName(winningRecordDO.getWinnerName());
                    winningRecordDTO.setPrizeName(winningRecordDO.getPrizeName());
                    winningRecordDTO.setPrizeTier(ActivityPrizeTiersEnum
                            .forName(winningRecordDO.getPrizeTier()));
                    winningRecordDTO.setWinningTime(winningRecordDO.getWinningTime());
                    return winningRecordDTO;
                })
                .collect(Collectors.toList());
    }


    /**
     * 缓存中奖者记录
     * @param key
     * @param winningRecordDOS
     * @param time
     */
    private void cacheWinningRecords(String key, List<WinningRecordDO> winningRecordDOS, Long time) {
        String str = "";
        try {
            if (!StringUtils.hasText(key)
                    || CollectionUtils.isEmpty(winningRecordDOS)) {
                logger.warn("缓存中奖者记录为空！ key:{}, value:{}", WINNING_RECORDS_CACHE_KEY_PREFIX + key, winningRecordDOS);
                return;
            }

            str = JacksonUtil.writeValueAsString(winningRecordDOS);
            redisUtil.set(WINNING_RECORDS_CACHE_KEY_PREFIX + key,
                    str,
                    time);
        } catch (Exception e) {
            logger.error("缓存中奖者记录发生异常: key:{}, value:{}", WINNING_RECORDS_CACHE_KEY_PREFIX + key, str, e);
             // 如果发生异常，缓存中奖者记录失败，不影响正常的业务流程，所以不抛出异常
        }
    }

    /**
     * 获取缓存中奖者记录
     * @param key
     * @return
     */
    private List<WinningRecordDO> getCacheWinningRecords(String key) {
        try {
            String str = redisUtil.get(WINNING_RECORDS_CACHE_KEY_PREFIX + key);
            if (!StringUtils.hasText(key)) {
                logger.warn("获取缓存中奖者 key 为空");
                return null;
            }
            if (!StringUtils.hasText(str)) {
                return Arrays.asList();
            }
            return JacksonUtil.readListValue(str, WinningRecordDO.class);
        } catch (Exception e) {
            logger.error("获取缓存中奖者记录发生异常: key:{}", WINNING_RECORDS_CACHE_KEY_PREFIX + key, e);
            // 如果发生异常，获取缓存中奖者记录失败，不影响正常的业务流程，所以不抛出异常
            return Arrays.asList();
        }
    }

    /**
     * 删除中奖者记录缓存(奖品维度，活动维度)
     * @param key
     */
    private void deleteWinningRecordsCache(String key) {
        try {
            if (redisUtil.haskey(WINNING_RECORDS_CACHE_KEY_PREFIX + key)) {
                // 存在删除
                redisUtil.del(WINNING_RECORDS_CACHE_KEY_PREFIX + key);
            }
        } catch (Exception e) {
            logger.error("删除中奖者记录缓存发生异常: key:{}", WINNING_RECORDS_CACHE_KEY_PREFIX + key, e);
        }
    }

    private void validateExecuteDrawRequest(ActivityDO activityDO, ActivityPrizeDO activityPrizeDO) {
        if (activityDO == null || activityPrizeDO == null) {
            throw new ServiceException(ServiceErrorCodeConstants.DRAW_PRIZE_PARAM_ERROR);
        }
        if (ActivityPrizeStatusEnum.END.name().equalsIgnoreCase(activityPrizeDO.getStatus())) {
            throw new ServiceException(ServiceErrorCodeConstants.DRAW_PRIZE_END);
        }
        if (ActivityStatusEnum.END.name().equalsIgnoreCase(activityDO.getStatus())) {
            throw new ServiceException(ServiceErrorCodeConstants.DRAW_ACTIVITY_END);
        }
    }

    private DrawPrizeParam buildServerSelectedDrawParam(ExecuteDrawPrizeParam param,
                                                        ActivityPrizeDO activityPrizeDO,
                                                        List<ActivityUserDO> candidates) {
        List<ActivityUserDO> shuffledCandidates = new ArrayList<>(candidates);
        Collections.shuffle(shuffledCandidates, secureRandom);

        int prizeAmount = Math.toIntExact(activityPrizeDO.getPrizeAmount());
        List<DrawPrizeParam.Winner> winners = shuffledCandidates.stream()
                .limit(prizeAmount)
                .map(activityUserDO -> {
                    DrawPrizeParam.Winner winner = new DrawPrizeParam.Winner();
                    winner.setUserId(activityUserDO.getUserId());
                    winner.setUserName(activityUserDO.getUserName());
                    return winner;
                })
                .collect(Collectors.toList());

        DrawPrizeParam drawPrizeParam = new DrawPrizeParam();
        drawPrizeParam.setActivityId(param.getActivityId());
        drawPrizeParam.setPrizeId(param.getPrizeId());
        drawPrizeParam.setWinningTime(new Date());
        drawPrizeParam.setWinnerList(winners);
        return drawPrizeParam;
    }

    private DrawPrizeDTO buildDrawPrizeDTO(DrawPrizeParam drawPrizeParam,
                                           List<WinningRecordDO> winningRecordDOS) {
        if (CollectionUtil.isEmpty(winningRecordDOS)) {
            throw new IllegalStateException("winning record list should not be empty");
        }

        DrawPrizeDTO dto = new DrawPrizeDTO();
        dto.setActivityId(drawPrizeParam.getActivityId());
        dto.setPrizeId(drawPrizeParam.getPrizeId());
        dto.setWinningTime(drawPrizeParam.getWinningTime());

        WinningRecordDO firstRecord = winningRecordDOS.get(0);
        dto.setPrizeName(firstRecord.getPrizeName());
        dto.setPrizeTier(ActivityPrizeTiersEnum.forName(firstRecord.getPrizeTier()));
        dto.setWinnerList(winningRecordDOS.stream().map(record -> {
            DrawPrizeDTO.WinnerDTO winnerDTO = new DrawPrizeDTO.WinnerDTO();
            winnerDTO.setUserId(record.getWinnerId());
            winnerDTO.setUserName(record.getWinnerName());
            return winnerDTO;
        }).collect(Collectors.toList()));
        return dto;
    }

    private List<WinningRecordDO> finalizeDrawInTransaction(DrawPrizeParam param) {
        statusConvert(param);
        return saveWinnerRecords(param);
    }

    private void notifyWinnersAsync(List<WinningRecordDO> winningRecordDOS) {
        if (CollectionUtil.isEmpty(winningRecordDOS)) {
            return;
        }
        try {
            threadPoolTaskExecutor.execute(() -> sendMail(winningRecordDOS));
        } catch (Exception e) {
            logger.error("提交中奖通知任务失败", e);
        }
    }

    private void sendMail(List<WinningRecordDO> winningRecordDOS) {
        if (CollectionUtil.isEmpty(winningRecordDOS)) {
            return;
        }
        for (WinningRecordDO winningRecordDO : winningRecordDOS) {
            String context = "Hi," + winningRecordDO.getWinnerName()
                    + "，恭喜你在：" + winningRecordDO.getActivityName()
                    + "活动中抽中了奖品：" + ActivityPrizeTiersEnum.forName(winningRecordDO.getPrizeTier()).getMessage()
                    + ": " + winningRecordDO.getPrizeName()
                    + "获奖时间为：" + DateUtil.formatTime(winningRecordDO.getWinningTime()) + "请尽快联系管理员领取奖品！";
            mailUtil.sendSampleMail(winningRecordDO.getWinnerEmail(), "抽奖中奖通知", context);
        }
    }

    private void cleanupAfterDrawFailure(DrawPrizeParam rollbackParam, Long activityId, Long prizeId) {
        rollbackStatusIfNecessary(rollbackParam);
        try {
            if (winnerNeedRollback(activityId, prizeId)) {
                deleteWinnerRecords(activityId, prizeId);
            }
        } catch (Exception e) {
            logger.error("清理中奖记录失败, activityId:{}, prizeId:{}", activityId, prizeId, e);
        }
        try {
            if (activityId != null && activityMapper.selectById(activityId) != null) {
                activityService.cacheActivity(activityId);
            }
        } catch (Exception e) {
            logger.error("回刷活动缓存失败, activityId:{}", activityId, e);
        }
    }

    private void rollbackStatusIfNecessary(DrawPrizeParam rollbackParam) {
        if (!statusNeedRollback(rollbackParam)) {
            return;
        }
        try {
            activityStatusManager.rollbackHandlerEvent(buildRollbackStatusDTO(rollbackParam));
        } catch (Exception e) {
            logger.error("回滚抽奖状态失败, activityId:{}, prizeId:{}",
                    rollbackParam.getActivityId(), rollbackParam.getPrizeId(), e);
        }
    }

    private boolean statusNeedRollback(DrawPrizeParam rollbackParam) {
        if (rollbackParam == null
                || rollbackParam.getActivityId() == null
                || rollbackParam.getPrizeId() == null) {
            return false;
        }
        List<Long> winnerUserIds = extractWinnerUserIds(rollbackParam);
        if (CollectionUtil.isEmpty(winnerUserIds)) {
            return false;
        }
        ActivityPrizeDO activityPrizeDO = activityPrizeMapper.selectByActivityPrizeId(
                rollbackParam.getActivityId(), rollbackParam.getPrizeId());
        return activityPrizeDO != null
                && ActivityPrizeStatusEnum.END.name().equalsIgnoreCase(activityPrizeDO.getStatus());
    }

    private boolean winnerNeedRollback(Long activityId, Long prizeId) {
        if (activityId == null || prizeId == null) {
            return false;
        }
        return winningRecordMapper.countByAPId(activityId, prizeId) > 0;
    }

    private ConvertActivityStatusDTO buildRollbackStatusDTO(DrawPrizeParam rollbackParam) {
        ConvertActivityStatusDTO convertActivityStatusDTO = new ConvertActivityStatusDTO();
        convertActivityStatusDTO.setActivityId(rollbackParam.getActivityId());
        convertActivityStatusDTO.setTargetActivityStatus(ActivityStatusEnum.RUNNING);
        convertActivityStatusDTO.setPrizeId(rollbackParam.getPrizeId());
        convertActivityStatusDTO.setTargetPrizeStatus(ActivityPrizeStatusEnum.INIT);
        convertActivityStatusDTO.setUserIds(extractWinnerUserIds(rollbackParam));
        convertActivityStatusDTO.setTargetUserStatus(ActivityUSerStatusEnum.INIT);
        return convertActivityStatusDTO;
    }

    private List<Long> extractWinnerUserIds(DrawPrizeParam rollbackParam) {
        if (rollbackParam == null || CollectionUtil.isEmpty(rollbackParam.getWinnerList())) {
            return Collections.emptyList();
        }
        return rollbackParam.getWinnerList().stream()
                .map(DrawPrizeParam.Winner::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private void statusConvert(DrawPrizeParam param) {
        ConvertActivityStatusDTO convertActivityStatusDTO = new ConvertActivityStatusDTO();
        convertActivityStatusDTO.setActivityId(param.getActivityId());
        convertActivityStatusDTO.setTargetActivityStatus(ActivityStatusEnum.END);
        convertActivityStatusDTO.setPrizeId(param.getPrizeId());
        convertActivityStatusDTO.setTargetPrizeStatus(ActivityPrizeStatusEnum.END);
        convertActivityStatusDTO.setUserIds(param.getWinnerList().stream()
                .map(DrawPrizeParam.Winner::getUserId)
                .collect(Collectors.toList()));
        convertActivityStatusDTO.setTargetUserStatus(ActivityUSerStatusEnum.END);
        activityStatusManager.handlerEvent(convertActivityStatusDTO);
    }

    private String buildDrawProcessingKey(Long activityId, Long prizeId) {
        return DRAW_PROCESSING_KEY_PREFIX + activityId + "_" + prizeId;
    }

    @Data
    private static class DrawExecutionContext {
        private DrawPrizeParam drawPrizeParam;
        private List<WinningRecordDO> winningRecordDOS;
    }
}
