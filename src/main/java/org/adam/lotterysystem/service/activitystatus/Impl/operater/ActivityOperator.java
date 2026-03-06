package org.adam.lotterysystem.service.activitystatus.Impl.operater;

import org.adam.lotterysystem.service.dto.ConvertActivityStatusDTO;
import org.springframework.stereotype.Component;

@Component
public class ActivityOperator extends AbstractActivityOperator{
    @Override
    public Integer sequence() {
        return 2;
    }

    @Override
    public Boolean needConvert(ConvertActivityStatusDTO convertActivityStatusDTO) {
        // 需要判断奖品是否全部抽完，如果奖品全部抽完了，那么活动状态就要转换成结束
        return null;
    }
}
