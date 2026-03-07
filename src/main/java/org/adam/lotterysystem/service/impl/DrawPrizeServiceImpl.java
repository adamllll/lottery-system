package org.adam.lotterysystem.service.impl;

import org.adam.lotterysystem.common.errorcode.ServiceErrorCodeConstants;
import org.adam.lotterysystem.common.exception.ServiceException;
import org.adam.lotterysystem.common.utils.JacksonUtil;
import org.adam.lotterysystem.controller.param.DrawPrizeParam;
import org.adam.lotterysystem.dao.dataobject.ActivityDO;
import org.adam.lotterysystem.dao.dataobject.ActivityPrizeDO;
import org.adam.lotterysystem.dao.mapper.ActivityMapper;
import org.adam.lotterysystem.dao.mapper.ActivityPrizeMapper;
import org.adam.lotterysystem.service.DrawPrizeService;
import org.adam.lotterysystem.service.enums.ActivityPrizeStatusEnum;
import org.adam.lotterysystem.service.enums.ActivityStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.adam.lotterysystem.common.config.DirectRabbitConfig.EXCHANGE_NAME;
import static org.adam.lotterysystem.common.config.DirectRabbitConfig.ROUTING;

@Service
public class DrawPrizeServiceImpl implements DrawPrizeService {

    private static final Logger logger = LoggerFactory.getLogger(DrawPrizeServiceImpl.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ActivityPrizeMapper activityPrizeMapper;

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
    public void checkDrawPrizeStatus(DrawPrizeParam param) {

        ActivityDO activityDO = activityMapper.selectById(param.getActivityId());
        // 奖品是否存在可以在 activity_prize 表中查询(在保存activity做了本地事务保证了奖品和活动的关系，所以这里不需要单独查询奖品表了)
        ActivityPrizeDO activityPrizeDO = activityPrizeMapper.selectByActivityPrizeId(
                param.getActivityId(), param.getPrizeId());
        // 判断活动活着奖品是否存在
        if (null == activityDO || null == activityPrizeDO) {
            throw new ServiceException(ServiceErrorCodeConstants.DRAW_PRIZE_PARAM_ERROR);
        }
        // 判断活动是否有效
        if (activityDO.getStatus().equalsIgnoreCase(ActivityStatusEnum.END.name())) {
            throw new ServiceException(ServiceErrorCodeConstants.DRAW_ACTIVITY_END);
        }
        // 奖品是否有效
        if (activityPrizeDO.getStatus().equalsIgnoreCase(ActivityPrizeStatusEnum.END.name())) {
            throw new ServiceException(ServiceErrorCodeConstants.DRAW_PRIZE_END);
        }
        // 中奖者人数是否和中奖者列表人数一致
        if (activityPrizeDO.getPrizeAmount() != param.getWinnerList().size()) {
            throw new ServiceException(ServiceErrorCodeConstants.DRAW_PRIZE_WINNER_LIST_ERROR);
        }
    }
}
