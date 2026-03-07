package org.adam.lotterysystem.service.activitystatus.Impl.operater;

import org.adam.lotterysystem.dao.dataobject.ActivityPrizeDO;
import org.adam.lotterysystem.dao.mapper.ActivityPrizeMapper;
import org.adam.lotterysystem.service.dto.ConvertActivityStatusDTO;
import org.adam.lotterysystem.service.enums.ActivityPrizeStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PrizeOperator extends AbstractActivityOperator{

    @Autowired
    private ActivityPrizeMapper activityPrizeMapper;

    @Override
    public Integer sequence() {
        return 1;
    }

    @Override
    public Boolean needConvert(ConvertActivityStatusDTO convertActivityStatusDTO) {
        Long activityId = convertActivityStatusDTO.getActivityId();
        Long prizeId = convertActivityStatusDTO.getPrizeId();
        ActivityPrizeStatusEnum targetPrizeStatus = convertActivityStatusDTO.getTargetPrizeStatus();
        if (null == prizeId
                || null == targetPrizeStatus
                || null == activityId) {
            return false;
        }
        ActivityPrizeDO activityPrizeDO = activityPrizeMapper.selectByActivityPrizeId(activityId, prizeId);
        if (null == activityPrizeDO) {
            return false;
        }
        // 判断当前奖品状态是否不是 END，如果当前奖品状态已经是 END了，那么就不需要转换了
        if (targetPrizeStatus.name().equalsIgnoreCase(activityPrizeDO.getStatus())) {
            return false;
        }
        return true;
    }

    @Override
    public Boolean convert(ConvertActivityStatusDTO convertActivityStatusDTO) {
        Long activityId = convertActivityStatusDTO.getActivityId();
        Long prizeId = convertActivityStatusDTO.getPrizeId();
        ActivityPrizeStatusEnum targetPrizeStatus = convertActivityStatusDTO.getTargetPrizeStatus();
        try {
            activityPrizeMapper.updateStatus(activityId, prizeId, targetPrizeStatus.name());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
