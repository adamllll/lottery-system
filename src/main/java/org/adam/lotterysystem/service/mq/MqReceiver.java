package org.adam.lotterysystem.service.mq;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import org.adam.lotterysystem.common.exception.ServiceException;
import org.adam.lotterysystem.common.utils.JacksonUtil;
import org.adam.lotterysystem.common.utils.MailUtil;
import org.adam.lotterysystem.common.utils.SMSUtil;
import org.adam.lotterysystem.controller.param.DrawPrizeParam;
import org.adam.lotterysystem.dao.dataobject.ActivityDO;
import org.adam.lotterysystem.dao.dataobject.ActivityPrizeDO;
import org.adam.lotterysystem.dao.dataobject.WinningRecordDO;
import org.adam.lotterysystem.dao.mapper.ActivityPrizeMapper;
import org.adam.lotterysystem.dao.mapper.WinningRecordMapper;
import org.adam.lotterysystem.service.DrawPrizeService;
import org.adam.lotterysystem.service.activitystatus.ActivityStatusManager;
import org.adam.lotterysystem.service.dto.ConvertActivityStatusDTO;
import org.adam.lotterysystem.service.enums.ActivityPrizeStatusEnum;
import org.adam.lotterysystem.service.enums.ActivityPrizeTiersEnum;
import org.adam.lotterysystem.service.enums.ActivityStatusEnum;
import org.adam.lotterysystem.service.enums.ActivityUSerStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.adam.lotterysystem.common.config.DirectRabbitConfig.QUEUE_NAME;

@Component
@RabbitListener(queues = QUEUE_NAME)
public class MqReceiver {

    public static final Logger logger = LoggerFactory.getLogger(MqReceiver.class);

    @Autowired
    private DrawPrizeService drawPrizeService;
    @Autowired
    private ActivityStatusManager activityStatusManager;
    @Autowired
    @Qualifier("asyncServiceExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Autowired
    private MailUtil mailUtil;
    @Autowired
    private ActivityPrizeMapper activityPrizeMapper;
    @Autowired
    private WinningRecordMapper winningRecordMapper;

    @RabbitHandler
    public void process(Map<String , String> message) {
        // 成功接收到队列的消息打印日志
        logger.info("Mq接收到抽奖消息: {}",
                JacksonUtil.writeValueAsString(message));
        String paramString = message.get("messageData");
        // 反序列化消息体
        DrawPrizeParam param = JacksonUtil.readValue(paramString, DrawPrizeParam.class);
        // 处理抽奖逻辑
        try {
            // 校验抽奖请求是否有效
            drawPrizeService.checkDrawPrizeStatus(param);

            // 状态扭转处理
            statusConvert(param);

            // 保存中奖名单
            List<WinningRecordDO>winningRecordDOS = drawPrizeService.saveWinnerRecords(param);

            // 通知中奖者(邮箱 短信)
            // 抽奖之后的后续流程，异步(并发)处理
            syncExecutor(winningRecordDOS);
        } catch (ServiceException e) {
            logger.error("处理抽奖消息发生异常: {}:{}", e.getCode(), e.getMessage(), e);
            // 如果发生异常，需要保证事务一致性(回滚),抛出异常：消息重试(解决异常导致的事务不一致问题)
            rollback(param);
            throw e;
        }catch (Exception e) {
            logger.error("处理抽奖消息发生未知异常: {}", e);
            // 如果发生未知异常，需要保证事务一致性(回滚),抛出异常
            rollback(param);
            throw e;
        }
    }

    /**
     * 处理抽奖异常的回滚逻辑：恢复处理请求之前的库表状态
     * @param param
     */
    private void rollback(DrawPrizeParam param) {
        // 1. 回滚状态：活动表、奖品表、人员表
        // 判断状态是否需要回滚 不需要直接return
        if (!statusNeedRollback(param)) {
            return;
        }
        rollbackStatus(param);
        // 2. 回滚中奖者名单
        if (!winnerNeedRollback(param)) {
            return;
        }
        rollbackWinner(param);
    }

    /**
     * 回滚中奖者名单：删除奖品的中奖者名单记录(如果存在)，保证中奖者名单的正确性
     * @param param
     */
    private void rollbackWinner(DrawPrizeParam param) {
        drawPrizeService.deleteWinnerRecords(param.getActivityId(), param.getPrizeId());
    }

    private boolean winnerNeedRollback(DrawPrizeParam param) {
        // 判断中奖者名单是否需要回滚：查询中奖者名单，如果存在记录则需要回滚，如果不存在记录则不需要回滚
        int count = winningRecordMapper.countByAPId(param.getActivityId(), param.getPrizeId());
        return count > 0;
    }

    /**
     * 回滚状态：活动表、奖品表、人员表
     * @param param
     */
    private void rollbackStatus(DrawPrizeParam param) {
        // 涉及状态的恢复，使用 ActivityStatusManager 进行状态的恢复，保证状态恢复的一致性和完整性
        ConvertActivityStatusDTO convertActivityStatusDTO = new ConvertActivityStatusDTO();
        convertActivityStatusDTO.setActivityId(param.getActivityId());
        convertActivityStatusDTO.setTargetActivityStatus(ActivityStatusEnum.RUNNING);
        convertActivityStatusDTO.setPrizeId(param.getPrizeId());
        convertActivityStatusDTO.setTargetPrizeStatus(ActivityPrizeStatusEnum.INIT);
        convertActivityStatusDTO.setUserIds(
                param.getWinnerList()
                        .stream()
                        .map(DrawPrizeParam.Winner::getUserId)
                        .collect(Collectors.toList()));
         convertActivityStatusDTO.setTargetUserStatus(ActivityUSerStatusEnum.INIT);
        activityStatusManager.rollbackHandlerEvent(convertActivityStatusDTO);
    }

    /**
     * 判断状态是否需要回滚
     * @param param
     * @return
     */
    private boolean statusNeedRollback(DrawPrizeParam param) {
        // 判断活动 + 奖品 + 人员 这三张表的状态是否已经扭转(不太优雅)
        // 扭转状态时保证了事务一致性，要么都扭转成功，要么都不扭转成功
        // 只需要判断人员/奖品是否已经扭转状态就可以了，因为活动状态的扭转依赖于人员/奖品状态的扭转
        ActivityPrizeDO activityPrizeDO = activityPrizeMapper.selectByActivityPrizeId(param.getActivityId(), param.getPrizeId());
        return activityPrizeDO.getStatus().equalsIgnoreCase(ActivityPrizeStatusEnum.END.name());
    }

    /**
     * 抽奖之后的后续流程，异步(并发)处理
     * @param winningRecordDOS
     */
    private void syncExecutor(List<WinningRecordDO> winningRecordDOS) {
        // 通过线程池、异步任务等方式实现异步处理
        // 优化扩展：加入策略模式或者其他设计模式
        // 短信通知 当前拿不到签名/模板时，不要硬做“获奖短信详情通知”
//        threadPoolTaskExecutor.execute(() -> sendSms(winningRecordDOS));
        // 邮件通知
        threadPoolTaskExecutor.execute(() -> sendMail(winningRecordDOS));
    }

    /**
     * 发送邮件
     * @param winningRecordDOS
     */
    private void sendMail(List<WinningRecordDO> winningRecordDOS) {
        if (CollectionUtil.isEmpty(winningRecordDOS)) {
            logger.info("没有中奖者记录，不需要发送邮件通知");
             return;
        }
        for (WinningRecordDO winningRecordDO : winningRecordDOS) {
            // 发送邮件通知
            String context = "Hi," + winningRecordDO.getWinnerName()
                    + "，恭喜你在：" + winningRecordDO.getActivityName()
                    + "活动中抽中了奖品：" + ActivityPrizeTiersEnum.forName(winningRecordDO.getPrizeTier()).getMessage()
                    + ": " + winningRecordDO.getPrizeName()
                    + "获奖时间为：" + DateUtil.formatTime(winningRecordDO.getWinningTime()) + "请尽快联系管理员领取奖品！";
            mailUtil.sendSampleMail(winningRecordDO.getWinnerEmail(), "抽奖中奖通知", context);
        }
    }

    // 状态扭转
    private void statusConvert(DrawPrizeParam param) {
        ConvertActivityStatusDTO convertActivityStatusDTO = new ConvertActivityStatusDTO();
        activityStatusManager.handlerEvent(convertActivityStatusDTO);
        convertActivityStatusDTO.setActivityId(param.getActivityId());
        convertActivityStatusDTO.setTargetActivityStatus(ActivityStatusEnum.END);
        convertActivityStatusDTO.setPrizeId(param.getPrizeId());
        convertActivityStatusDTO.setTargetPrizeStatus(ActivityPrizeStatusEnum.END);
        convertActivityStatusDTO.setUserIds(
                param.getWinnerList()
                        .stream()
                        .map(DrawPrizeParam.Winner::getUserId)
                        .collect(Collectors.toList()));
        convertActivityStatusDTO.setTargetUserStatus(ActivityUSerStatusEnum.END);
        activityStatusManager.handlerEvent(convertActivityStatusDTO);
    }
/*    private void statusConvert(DrawPrizeParam param) {
        // 存在的问题
        // 1. 活动扭转的状态有依赖性，导致代码维护性差
        // 2. 状态扭转模块可能会扩展，当前写法扩展性差，维护性差
        // 3. 代码灵活性、扩展性、维护性差
        // 解决方案：设计模式(责任链设计模式、策略模式)

        // 活动 ： RUNNING （全部奖品抽取完毕才改变状态）
        // 奖品 ： INIT --> END(被抽取)
        // 人员 ： INIT --> END(被抽过了)
        // 1. 扭转奖品状态
        // 查询活动关联的奖品信息
        // 条件判断是否符合扭转奖品状态：判断当前状态是否为 END(是为不需要扭转)
        // 2. 扭转人员状态
        // 3. 扭转活动状态(必须在扭转奖品之后完成)
        // 4. 更新活动完整信息缓存
    }*/
}


