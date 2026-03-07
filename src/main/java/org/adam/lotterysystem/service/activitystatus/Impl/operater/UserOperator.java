package org.adam.lotterysystem.service.activitystatus.Impl.operater;

import org.adam.lotterysystem.dao.dataobject.ActivityUserDO;
import org.adam.lotterysystem.dao.mapper.ActivityUserMapper;
import org.adam.lotterysystem.service.dto.ConvertActivityStatusDTO;
import org.adam.lotterysystem.service.enums.ActivityUSerStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
public class UserOperator extends AbstractActivityOperator {

    @Autowired
    private ActivityUserMapper activityUserMapper;

    @Override
    public Integer sequence() {
        return 1;
    }

    @Override
    public Boolean needConvert(ConvertActivityStatusDTO convertActivityStatusDTO) {
        Long activityId = convertActivityStatusDTO.getActivityId();
        List<Long> userIds = convertActivityStatusDTO.getUserIds();
        ActivityUSerStatusEnum targetUserStatus = convertActivityStatusDTO.getTargetUserStatus();
            if (null == activityId
                    || CollectionUtils.isEmpty(userIds)
                    || null == targetUserStatus) {
                return false;
            }
            List<ActivityUserDO> activityUserDOList = activityUserMapper.batchSelectByActivityUserIds(activityId, userIds);
            if (CollectionUtils.isEmpty(activityUserDOList)) {
                return false;
            }
            for (ActivityUserDO activityUserDO : activityUserDOList) {
                // 判断当前用户状态是否不是 END，如果当前用户状态已经是 END了，那么就不需要转换了
                if (activityUserDO.getStatus().equalsIgnoreCase(targetUserStatus.name())) {
                    return false;
                }
            }
            return true;
    }

    @Override
    public Boolean convert(ConvertActivityStatusDTO convertActivityStatusDTO) {
        Long activityId = convertActivityStatusDTO.getActivityId();
        List<Long> userIds = convertActivityStatusDTO.getUserIds();
        ActivityUSerStatusEnum targetUserStatus = convertActivityStatusDTO.getTargetUserStatus();
        try {
            activityUserMapper.batchUpdateStatus(activityId, userIds, targetUserStatus.name());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
