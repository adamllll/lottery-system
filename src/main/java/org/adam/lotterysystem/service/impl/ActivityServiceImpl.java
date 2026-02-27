package org.adam.lotterysystem.service.impl;

import org.adam.lotterysystem.common.errorcode.ServiceErrorCodeConstants;
import org.adam.lotterysystem.common.exception.ServiceException;
import org.adam.lotterysystem.controller.param.CreateActivityParam;
import org.adam.lotterysystem.controller.param.CreatePrizeByActivityParam;
import org.adam.lotterysystem.controller.param.CreateUserByActivityParam;
import org.adam.lotterysystem.dao.dataobject.ActivityDO;
import org.adam.lotterysystem.dao.dataobject.ActivityPrizeDO;
import org.adam.lotterysystem.dao.dataobject.ActivityUserDO;
import org.adam.lotterysystem.dao.mapper.*;
import org.adam.lotterysystem.service.ActivityService;
import org.adam.lotterysystem.service.dto.CreateActivityDTO;
import org.adam.lotterysystem.service.enums.ActivityPrizeStatusEnum;
import org.adam.lotterysystem.service.enums.ActivityPrizeTiersEnum;
import org.adam.lotterysystem.service.enums.ActivityStatusEnum;
import org.adam.lotterysystem.service.enums.ActivityUSerStatusEnum;
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

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ActivityUserMapper activityUserMapper;

    @Autowired
    private ActivityPrizeMapper activityPrizeMapper;

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
        List<CreatePrizeByActivityParam> prizeParams =  param.getActivityPrizeList();
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

        // 校验活动奖品等级有效性
        param.getActivityPrizeList().forEach(prize -> {
            if (null == ActivityPrizeTiersEnum.forName(prize.getPrizeTiers())) {
                throw new ServiceException(ServiceErrorCodeConstants.CREATE_ACTIVITY_PRIZE_TIERS_ERROR);
            }
        });
    }
}
