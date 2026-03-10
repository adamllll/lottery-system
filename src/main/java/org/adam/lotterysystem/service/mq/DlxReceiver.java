package org.adam.lotterysystem.service.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.adam.lotterysystem.common.config.DirectRabbitConfig.*;

@Component
@RabbitListener(queues = DLX_QUEUE_NAME)
public class DlxReceiver {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final Logger logger = LoggerFactory.getLogger(DlxReceiver.class);

    @RabbitHandler
    public void process(Map<String, String> message) {
        // 死信队列的处理方法
        logger.info("开始处理异常消息！死信队列接收到消息: {}", message);
        // 直接将消息重新发送到原交换机和路由键，进行重试
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING, message);
    }
}
