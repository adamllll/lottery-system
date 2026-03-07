package org.adam.lotterysystem.service.mq;

import org.adam.lotterysystem.common.exception.ServiceException;
import org.adam.lotterysystem.common.utils.JacksonUtil;
import org.adam.lotterysystem.controller.param.DrawPrizeParam;
import org.adam.lotterysystem.service.DrawPrizeService;
import org.adam.lotterysystem.service.activitystatus.ActivityStatusManager;
import org.adam.lotterysystem.service.dto.ConvertActivityStatusDTO;
import org.adam.lotterysystem.service.enums.ActivityPrizeStatusEnum;
import org.adam.lotterysystem.service.enums.ActivityStatusEnum;
import org.adam.lotterysystem.service.enums.ActivityUSerStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

            // 通知中奖者
        } catch (ServiceException e) {
            logger.error("处理抽奖消息发生异常: {}:{}", e.getCode(), e.getMessage(), e);
            // 如果发生异常，需要保证事务一致性(回滚),抛出异常
        }catch (Exception e) {
            logger.error("处理抽奖消息发生未知异常: {}", e);
            // 如果发生未知异常，需要保证事务一致性(回滚),抛出异常
             throw new RuntimeException(e);
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
