package org.adam.lotterysystem.service.impl;

import org.adam.lotterysystem.common.errorcode.ServiceErrorCodeConstants;
import org.adam.lotterysystem.common.exception.ServiceException;
import org.adam.lotterysystem.common.utils.JacksonUtil;
import org.adam.lotterysystem.common.utils.RedisUtil;
import org.adam.lotterysystem.controller.param.CreateActivityParam;
import org.adam.lotterysystem.controller.param.CreatePrizeByActivityParam;
import org.adam.lotterysystem.controller.param.CreateUserByActivityParam;
import org.adam.lotterysystem.controller.param.PageParam;
import org.adam.lotterysystem.dao.dataobject.ActivityDO;
import org.adam.lotterysystem.dao.dataobject.ActivityPrizeDO;
import org.adam.lotterysystem.dao.dataobject.ActivityUserDO;
import org.adam.lotterysystem.dao.dataobject.PrizeDO;
import org.adam.lotterysystem.dao.mapper.*;
import org.adam.lotterysystem.service.ActivityService;
import org.adam.lotterysystem.service.dto.ActivityDTO;
import org.adam.lotterysystem.service.dto.ActivityDetailDTO;
import org.adam.lotterysystem.service.dto.CreateActivityDTO;
import org.adam.lotterysystem.service.dto.PageListDTO;
import org.adam.lotterysystem.service.enums.ActivityPrizeStatusEnum;
import org.adam.lotterysystem.service.enums.ActivityPrizeTiersEnum;
import org.adam.lotterysystem.service.enums.ActivityStatusEnum;
import org.adam.lotterysystem.service.enums.ActivityUSerStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ActivityServiceImpl implements ActivityService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityServiceImpl.class);

    // 活动信息在 Redis 中的 key 前缀，实际 key 还需要拼接活动 id，例如：ACTIVITY_10001
    private final String ACTIVITY_PREFIX = "ACTIVITY_";
    // 活动信息缓存在 Redis 中的 key 的过期时间，单位：秒（这里设置为三天）
    private final Long ACTIVITY_TIMEOUT = 60 * 60 * 24 * 3L;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PrizeMapper prizeMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ActivityUserMapper activityUserMapper;

    @Autowired
    private ActivityPrizeMapper activityPrizeMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    @Transactional(rollbackFor = Exception.class) // 涉及多表
    public CreateActivityDTO createActivity(CreateActivityParam param) {
        // 校验活动信息是否正确
        checkActivityInfo(param);
        // 保存活动信息
        ActivityDO activityDO = new ActivityDO();
        activityDO.setActivityName(param.getActivityName());
        activityDO.setDescription(param.getDescription());
        activityDO.setStatus(ActivityStatusEnum.RUNNING.name());
        activityMapper.insert(activityDO);
        // 保存活动关联的奖品信息
        List<CreatePrizeByActivityParam> prizeParams = param.getActivityPrizeList();
        List<ActivityPrizeDO> activityPrizeDOList = prizeParams.stream().map(prizeParam -> {
            ActivityPrizeDO activityPrizeDO = new ActivityPrizeDO();
            activityPrizeDO.setActivityId(activityDO.getId());
            activityPrizeDO.setPrizeId(prizeParam.getPrizeId());
            activityPrizeDO.setPrizeAmount(prizeParam.getPrizeAmount());
            activityPrizeDO.setPrizeTiers(prizeParam.getPrizeTiers());
            activityPrizeDO.setStatus(ActivityPrizeStatusEnum.INIT.name());
            return activityPrizeDO;
        }).collect(Collectors.toList());
        activityPrizeMapper.batchInsert(activityPrizeDOList);
        // 保存活动关联的人员信息
        List<CreateUserByActivityParam> userParams = param.getActivityUserList();
        List<ActivityUserDO> activityUserDOList = userParams.stream().map(userParam -> {
            ActivityUserDO activityUserDO = new ActivityUserDO();
            activityUserDO.setActivityId(activityDO.getId());
            activityUserDO.setUserId(userParam.getUserId());
            activityUserDO.setUserName(userParam.getUserName());
            activityUserDO.setStatus(ActivityUSerStatusEnum.INIT.name());
            return activityUserDO;
        }).collect(Collectors.toList());
        activityUserMapper.batchInsert(activityUserDOList);
        // 整合完整的活动信息，存放Redis
        // activityId : ActivityDetailInfoTOD:活动信息+奖品信息+人员信息

        // 先获取奖品基本属性列表
        // 获取需要查询的奖品id 列表
        List<Long> prizeIds = param.getActivityPrizeList().stream()
                .map(CreatePrizeByActivityParam::getPrizeId)
                .distinct() // 去重
                .collect(Collectors.toList());
        List<PrizeDO> prizeDOList = prizeMapper.batchSelectByIds(prizeIds);
        ActivityDetailDTO detailDTO = convertToActivityDetailDTO(activityDO, activityPrizeDOList, activityUserDOList, prizeDOList);

        cacheActivity(detailDTO);
        // 返回创建活动的结果
        CreateActivityDTO createActivityDTO = new CreateActivityDTO();
        createActivityDTO.setActivityId(activityDO.getId());
        return createActivityDTO;
    }


    // 缓存完整的活动信息到 Redis
    private void cacheActivity(ActivityDetailDTO detailDTO) {
        // key: activityId(实际上：ACTIVITY_**)
        // value: ActivityDetailDTO(实际上：json字符串)
        if (null == detailDTO || null == detailDTO.getActivityId()) {
            logger.warn("缓存活动信息不存在");
            return;
        }
        try {
            redisUtil.set(ACTIVITY_PREFIX + detailDTO.getActivityId(), JacksonUtil.writeValueAsString(detailDTO), ACTIVITY_TIMEOUT);
        } catch (Exception e) {
            logger.error("缓存活动信息失败, ActivityDetailDTO={}", JacksonUtil.writeValueAsString(detailDTO), e);
        }
    }

    // 根据活动 Id 从缓存中获取完整的活动信息
    private ActivityDetailDTO getActivityFromCache(Long activityId) {
        if (null == activityId) {
            logger.warn("活动 id 不存在");
            return null;
        }
        try {
            String str = redisUtil.get(ACTIVITY_PREFIX + activityId);
            if (StringUtils.hasText(str)) {
                logger.info("获取缓存数据为空, key={}", ACTIVITY_PREFIX + activityId);
                return null;
            }
            return JacksonUtil.readValue(str, ActivityDetailDTO.class);
        } catch (Exception e) {
            logger.error("从缓存中获取活动信息失败, key={}", ACTIVITY_PREFIX + activityId, e);
            return null;
        }

    }

    /**
     * 根据活动基本信息、活动奖品信息、活动人员信息和奖品基本属性信息，整合成完整的活动详细信息
     * @param activityDO
     * @param activityPrizeDOList
     * @param activityUserDOList
     * @param prizeDOList
     * @return
     */
    private ActivityDetailDTO convertToActivityDetailDTO(ActivityDO activityDO, List<ActivityPrizeDO> activityPrizeDOList, List<ActivityUserDO> activityUserDOList, List<PrizeDO> prizeDOList) {
        ActivityDetailDTO detailDTO = new ActivityDetailDTO();
        detailDTO.setActivityId(activityDO.getId());
        detailDTO.setActivityName(activityDO.getActivityName());
        detailDTO.setDescription(activityDO.getDescription());
        detailDTO.setStatus(ActivityStatusEnum.forName(activityDO.getStatus()));

        // 奖品信息列表
        // activityPrizeDO:id name imageUrl price description tiers prizeAmount status
        // prizeDO:{id,name...},{id,name...}
        List<ActivityDetailDTO.PrizeDTO> prizeDTOList = activityPrizeDOList
                .stream()
                .map(activityPrizeDO -> {
                    ActivityDetailDTO.PrizeDTO prizeDTO = new ActivityDetailDTO.PrizeDTO();
                    prizeDTO.setId(activityPrizeDO.getPrizeId());
                    Optional<PrizeDO> optionalPrizeDO = prizeDOList.stream()
                            .filter(item -> item.getId().equals(activityPrizeDO.getPrizeId()))
                            .findFirst();
                    // 如果PrizeDO为空，不执行当前方法
                    optionalPrizeDO.ifPresent(prizeDO -> {
                        prizeDTO.setName(prizeDO.getName());
                        prizeDTO.setImageUrl(prizeDO.getImageUrl());
                        prizeDTO.setPrice(prizeDO.getPrice());
                        prizeDTO.setDescription(prizeDO.getDescription());
                    });
                    prizeDTO.setTiers(ActivityPrizeTiersEnum.forName(activityPrizeDO.getPrizeTiers()));
                    prizeDTO.setPrizeAmount(activityPrizeDO.getPrizeAmount());
                    prizeDTO.setStatus(ActivityPrizeStatusEnum.forName(activityPrizeDO.getStatus()));
                    return prizeDTO;
                }).collect(Collectors.toList());
        detailDTO.setPrizeDTOList(prizeDTOList);

        // 人员信息列表
        List<ActivityDetailDTO.UserDTO> userDTOList = activityUserDOList.stream()
                .map(activityUserDO -> {
                    ActivityDetailDTO.UserDTO userDTO = new ActivityDetailDTO.UserDTO();
                    userDTO.setUserId(activityUserDO.getUserId());
                    userDTO.setUserName(activityUserDO.getUserName());
                    userDTO.setStatus(ActivityUSerStatusEnum.forName(activityUserDO.getStatus()));
                    return userDTO;
                }).collect(Collectors.toList());
        detailDTO.setUserDTOList(userDTOList);
        return detailDTO;
    }

    /**
     * 校验活动有效性
     *
     * @param param
     */
    private void checkActivityInfo(CreateActivityParam param) {
        if (null == param) {
            throw new ServiceException(ServiceErrorCodeConstants.CREATE_ACTIVITY_INFO_IS_EMPTY);
        }
        // 人员 id在人员表中是否存在
        List<Long> userIds = param.getActivityUserList()
                .stream().
                map(CreateUserByActivityParam::getUserId)
                .distinct() // 去重
                .collect(Collectors.toList());
        List<Long> existUserIds = userMapper.selectExistByIds(userIds);
        if (CollectionUtils.isEmpty(existUserIds)) {
            throw new ServiceException(ServiceErrorCodeConstants.CREATE_ACTIVITY_USER_ERROR);
        }
        userIds.forEach(id -> {
            if (!existUserIds.contains(id)) {
                throw new ServiceException(ServiceErrorCodeConstants.CREATE_ACTIVITY_USER_ERROR);
            }
        });
        // 奖品 id在奖品表中是否存在
        List<Long> prizeIds = param.getActivityPrizeList()
                .stream()
                .map(CreatePrizeByActivityParam::getPrizeId)
                .distinct() // 去重
                .collect(Collectors.toList());
        List<Long> existPrizeIds = prizeMapper.selectExistByIds(prizeIds);
        if (CollectionUtils.isEmpty(existPrizeIds)) {
            throw new ServiceException(ServiceErrorCodeConstants.CREATE_ACTIVITY_PRIZE_ERROR);
        }
        prizeIds.forEach(id -> {
            if (!existPrizeIds.contains(id)) {
                throw new ServiceException(ServiceErrorCodeConstants.CREATE_ACTIVITY_PRIZE_ERROR);
            }
        });
        // 人员数量≥奖品数量
        int userAmount = param.getActivityUserList().size();
        long prizeAmount = param.getActivityPrizeList()
                .stream()
                .mapToLong(CreatePrizeByActivityParam::getPrizeAmount)
                .sum();
        if (userAmount < prizeAmount) {
            throw new ServiceException(ServiceErrorCodeConstants.CREATE_ACTIVITY_USER_AMOUNT_LT_PRIZE_AMOUNT_ERROR);
        }

        // 校验活动奖品等级有效性
        param.getActivityPrizeList().forEach(prize -> {
            if (null == ActivityPrizeTiersEnum.forName(prize.getPrizeTiers())) {
                throw new ServiceException(ServiceErrorCodeConstants.CREATE_ACTIVITY_PRIZE_TIERS_ERROR);
            }
        });
    }

    @Override
    public PageListDTO<ActivityDTO> findActivityList(PageParam param) {
        // 获取总量
        int total = activityMapper.count(param);

        // 获取当前页表列
        List<ActivityDO> activityDOList = activityMapper.selectActivityList(param.offset(), param.getPageSize());
        List<ActivityDTO> activityDTOList = activityDOList.stream().map(activityDO -> {
            ActivityDTO activityDTO = new ActivityDTO();
            activityDTO.setActivityId(activityDO.getId());
            activityDTO.setActivityName(activityDO.getActivityName());
            activityDTO.setDescription(activityDO.getDescription());
            activityDTO.setStatus(ActivityStatusEnum.forName(activityDO.getStatus()));
            return activityDTO;
        }).collect(Collectors.toList());
        return new PageListDTO<>(total, activityDTOList);
    }

    @Override
    public ActivityDetailDTO getActivityDetail(Long activityId) {
        if (null == activityId) {
            logger.warn("查询活动 id 不存在");
            return null;
        }
        // 先从缓存中获取活动信息
        ActivityDetailDTO detailDTO = getActivityFromCache(activityId);
        if (null != detailDTO) {
            logger.info("从缓存中获取活动信息成功, activityDTO= {}",
                    JacksonUtil.writeValueAsString(detailDTO));
            return detailDTO;
        }

        //  如果缓存中没有，再从数据库中获取活动信息
        // 查询活动表
        ActivityDO activityDO = activityMapper.selectById(activityId);

        // 查询活动奖品表
        List<ActivityPrizeDO> activityPrizeDO = activityPrizeMapper.selectByActivityId(activityId);
        // 查询活动人员表
        List<ActivityUserDO> activityUserDO = activityUserMapper.selectByActivityId(activityId);
        // 查询奖品表，获取奖品基本属性
        List<Long> prizeIds = activityPrizeDO.stream()
                .map(ActivityPrizeDO::getPrizeId)
                .collect(Collectors.toList());
        List<PrizeDO> prizeDOList = prizeMapper.batchSelectByIds(prizeIds);

        // 整合活动详细信息，返回Redis
        detailDTO = convertToActivityDetailDTO(activityDO, activityPrizeDO, activityUserDO, prizeDOList);
        cacheActivity(detailDTO);
        // 返回活动详细信息
        return detailDTO;
    }

    /**
     *
     * @param activityId
     */
    @Override
    public void cacheActivity(Long activityId) {

        if (null == activityId) {
            logger.warn("缓存活动失败, activityId={}", activityId);
            throw new ServiceException(ServiceErrorCodeConstants.CACHE_ACTIVITY_ID_IS_NULL);
        }

        // 查询表数据： 活动表、活动奖品表、活动人员表、奖品表
        ActivityDO activityDO = activityMapper.selectById(activityId);
        if (null == activityDO) {
            logger.error("查询活动表数据不存在, activityId={}", activityId);
            throw new ServiceException(ServiceErrorCodeConstants.CACHE_ACTIVITY_ID_IS_NULL);
        }

        // 查询活动奖品表
        List<ActivityPrizeDO> activityPrizeDO = activityPrizeMapper.selectByActivityId(activityId);
        // 查询活动人员表
        List<ActivityUserDO> activityUserDO = activityUserMapper.selectByActivityId(activityId);
        // 查询奖品表，获取奖品基本属性
        List<Long> prizeIds = activityPrizeDO.stream()
                .map(ActivityPrizeDO::getPrizeId)
                .collect(Collectors.toList());
        List<PrizeDO> prizeDOList = prizeMapper.batchSelectByIds(prizeIds);

        // 整合活动详细信息，返回Redis
        // 整合完整的活动信息并缓存
        cacheActivity(convertToActivityDetailDTO(activityDO, activityPrizeDO, activityUserDO, prizeDOList));
    }
}


