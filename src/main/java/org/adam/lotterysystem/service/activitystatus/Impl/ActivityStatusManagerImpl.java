package org.adam.lotterysystem.service.activitystatus.Impl;

import org.adam.lotterysystem.service.activitystatus.ActivityStatusManager;
import org.adam.lotterysystem.service.activitystatus.Impl.operater.AbstractActivityOperator;
import org.adam.lotterysystem.service.dto.ConvertActivityStatusDTO;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

@Component
public class ActivityStatusManagerImpl implements ActivityStatusManager {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ActivityStatusManagerImpl.class);

    @Autowired
    private final Map<String , AbstractActivityOperator> activityOperatorMap = new HashMap<>();

    @Override
    public void handlerEvent(ConvertActivityStatusDTO convertActivityStatusDTO) {

        if (CollectionUtils.isEmpty(activityOperatorMap)) {
            logger.warn("operator为空！没有找到任何活动状态转换处理器");
            return;
        }
        Map<String, AbstractActivityOperator> currMap = new HashMap<>(activityOperatorMap);
        Boolean update = false;
        // 先处理： 人员、奖品
        update = processConvertStatus(convertActivityStatusDTO ,currMap, 1);

        // 后处理： 活动
        if (update) {
            update = processConvertStatus(convertActivityStatusDTO ,currMap, 2);

        } else {
            update = processConvertStatus(convertActivityStatusDTO ,currMap, 2);
        }

        // 更新缓存
        if (update) {
            
        }
    }

    /**
     * 扭转状态
     * @param convertActivityStatusDTO
     * @param currMap
     * @param sequence
     * @return
     */
    private Boolean processConvertStatus(ConvertActivityStatusDTO convertActivityStatusDTO, Map<String, AbstractActivityOperator> currMap, int sequence) {
        // 遍历 currMap

        // Operator 是否需要转换

        // 是否需要转换

        // currMap 删除当前 Operator

        // 返回
        return false;
    }
}
