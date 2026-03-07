package org.adam.lotterysystem.service.activitystatus.Impl.operater;

import org.adam.lotterysystem.service.dto.ConvertActivityStatusDTO;

public abstract class AbstractActivityOperator {

    // 控制处理顺序，数值越小优先级越高
    public abstract Integer sequence();

    // 是否需要转换
    public abstract Boolean needConvert(ConvertActivityStatusDTO convertActivityStatusDTO);

    // 执行转换
    public abstract Boolean convert(ConvertActivityStatusDTO convertActivityStatusDTO);
}
