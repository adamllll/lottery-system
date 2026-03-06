package org.adam.lotterysystem.service.activitystatus.Impl.operater;

import org.adam.lotterysystem.service.dto.ConvertActivityStatusDTO;
import org.springframework.stereotype.Component;

@Component
public class PrizeOperator extends AbstractActivityOperator{
    @Override
    public Integer sequence() {
        return 1;
    }

    @Override
    public Boolean needConvert(ConvertActivityStatusDTO convertActivityStatusDTO) {
        // 判断当前奖品状态是否不是 END
        return null;
    }
}
