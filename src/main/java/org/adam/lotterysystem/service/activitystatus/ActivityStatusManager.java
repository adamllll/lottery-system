package org.adam.lotterysystem.service.activitystatus;

import org.adam.lotterysystem.service.dto.ConvertActivityStatusDTO;

public interface ActivityStatusManager {

    // 处理活动相关状态转换
    void handlerEvent(ConvertActivityStatusDTO convertActivityStatusDTO);
}
