package org.adam.lotterysystem.service.impl;

import org.adam.lotterysystem.common.errorcode.ServiceErrorCodeConstants;
import org.adam.lotterysystem.common.exception.ServiceException;
import org.adam.lotterysystem.controller.param.CreateActivityParam;
import org.adam.lotterysystem.controller.param.CreatePrizeByActivityParam;
import org.adam.lotterysystem.controller.param.CreateUserByActivityParam;
import org.adam.lotterysystem.dao.mapper.PrizeMapper;
import org.adam.lotterysystem.dao.mapper.UserMapper;
import org.adam.lotterysystem.service.ActivityService;
import org.adam.lotterysystem.service.dto.CreateActivityDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityServiceImpl implements ActivityService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PrizeMapper prizeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class) // 涉及多表
    public CreateActivityDTO createActivity(CreateActivityParam param) {
        // 校验活动信息是否正确
        checkActivityInfo(param);
        // 保存活动信息

        // 保存活动关联的奖品信息

        // 保存活动关联的人员信息

        // 整合完整的活动信息，存放Redis

        // 返回创建活动的结果
        return null;
    }

    /**
     * 校验活动有效性
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
            throw new ServiceException(ServiceErrorCodeConstants.CREATE_PRIZE_USER_ERROR);
        }
    }
}
