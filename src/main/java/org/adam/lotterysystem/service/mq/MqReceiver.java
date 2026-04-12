package org.adam.lotterysystem.service.mq;

import org.adam.lotterysystem.common.exception.ServiceException;
import org.adam.lotterysystem.common.utils.JacksonUtil;
import org.adam.lotterysystem.controller.param.DrawPrizeParam;
import org.adam.lotterysystem.service.DrawPrizeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.adam.lotterysystem.common.config.DirectRabbitConfig.QUEUE_NAME;

@Component
@RabbitListener(queues = QUEUE_NAME)
public class MqReceiver {

    public static final Logger logger = LoggerFactory.getLogger(MqReceiver.class);

    @Autowired
    private DrawPrizeService drawPrizeService;

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
            // 1. 前端有可能发起两个一样的抽奖请求，对于param来说也是一样的两个请求
            // 2. param：最后一个奖项： 处理param1：活动完成、奖品完成 处理param2：回滚活动、奖品状态
            if (!drawPrizeService.checkDrawPrizeStatus(param)) {
                return;
            }

            drawPrizeService.finalizeDraw(param);
        } catch (ServiceException e) {
            logger.error("处理抽奖消息发生异常: {}:{}", e.getCode(), e.getMessage(), e);
            throw e;
        }catch (Exception e) {
            logger.error("处理抽奖消息发生未知异常: {}", e);
            throw e;
        }
    }
}

