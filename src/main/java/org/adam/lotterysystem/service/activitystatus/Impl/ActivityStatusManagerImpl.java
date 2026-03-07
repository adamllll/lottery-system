package org.adam.lotterysystem.service.activitystatus.Impl;

import org.adam.lotterysystem.common.errorcode.ServiceErrorCodeConstants;
import org.adam.lotterysystem.common.exception.ServiceException;
import org.adam.lotterysystem.service.ActivityService;
import org.adam.lotterysystem.service.activitystatus.ActivityStatusManager;
import org.adam.lotterysystem.service.activitystatus.Impl.operater.AbstractActivityOperator;
import org.adam.lotterysystem.service.dto.ConvertActivityStatusDTO;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component
public class ActivityStatusManagerImpl implements ActivityStatusManager {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ActivityStatusManagerImpl.class);

    @Autowired
    private final Map<String , AbstractActivityOperator> activityOperatorMap = new HashMap<>();

    @Autowired
    private ActivityService activityService;

    @Override
    @Transactional(rollbackFor = Exception.class) // 添加事务注解，确保在转换过程中出现异常时能够回滚
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
        update = processConvertStatus(convertActivityStatusDTO ,currMap, 2) || update;

        // 更新缓存
        if (update) {
            activityService.cacheActivity(convertActivityStatusDTO.getActivityId());
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
        Boolean update = false;
        // 遍历 currMap
        Iterator<Map.Entry<String, AbstractActivityOperator>> iterator = currMap.entrySet().iterator();
        while (iterator.hasNext()) {
            AbstractActivityOperator operator = iterator.next().getValue();
            // Operator 是否需要转换
            if (operator.sequence() != sequence
                    || !operator.needConvert(convertActivityStatusDTO)){
                continue;
            }
            // 需要转换
            if (!operator.convert(convertActivityStatusDTO)) {
                logger.error("{}状态转换失败,无法继续进行后续转换", operator.getClass().getName());
                throw new ServiceException(ServiceErrorCodeConstants.ACTIVITY_STATUS_CONVERT_ERROR);
            }
            // currMap 删除当前 Operator
            iterator.remove();
            update = true;
        }
        // 返回
        return update;
    }
}
