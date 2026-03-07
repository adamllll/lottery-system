package org.adam.lotterysystem.service.activitystatus.Impl.operater;

import org.adam.lotterysystem.dao.dataobject.ActivityDO;
import org.adam.lotterysystem.dao.mapper.ActivityMapper;
import org.adam.lotterysystem.dao.mapper.ActivityPrizeMapper;
import org.adam.lotterysystem.service.dto.ConvertActivityStatusDTO;
import org.adam.lotterysystem.service.enums.ActivityPrizeStatusEnum;
import org.adam.lotterysystem.service.enums.ActivityStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ActivityOperator extends AbstractActivityOperator{

    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private ActivityPrizeMapper activityPrizeMapper;

    @Override
    public Integer sequence() {
        return 2;
    }

    @Override
    public Boolean needConvert(ConvertActivityStatusDTO convertActivityStatusDTO) {
        Long activityId = convertActivityStatusDTO.getActivityId();
        ActivityStatusEnum targetStatus = convertActivityStatusDTO.getTargetActivityStatus();
        if (null == activityId
                || null == targetStatus) {
            return false;
        }
        ActivityDO activityDO = activityMapper.selectById(activityId);
        if (null == activityDO) {
            return false;
        }
        // 当前活动状态与传入状态是否一致
        if (targetStatus.name().equalsIgnoreCase(activityDO.getStatus())){
            return false;
        }
        // 需要判断奖品是否全部抽完，如果奖品全部抽完了，那么活动状态就要转换成结束
        // 查询RUNNING状态的奖品数量，如果数量为0了，那么就说明奖品全部抽完了
        int count = activityPrizeMapper.countPrize(activityId, ActivityPrizeStatusEnum.INIT.name());
        if (count > 0) {
            return false;
        }
        return true;
    }

    @Override
    public Boolean convert(ConvertActivityStatusDTO convertActivityStatusDTO) {
        try {
            activityMapper.updateStatus(convertActivityStatusDTO.getActivityId(),
                    convertActivityStatusDTO.getTargetActivityStatus().name());
            return true;
        }catch (Exception e) {
            return false;
        }
    }
}
