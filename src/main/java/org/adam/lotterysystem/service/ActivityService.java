package org.adam.lotterysystem.service;

import org.adam.lotterysystem.controller.param.CreateActivityParam;
import org.adam.lotterysystem.service.dto.CreateActivityDTO;


public interface ActivityService {

    // 创建活动
    CreateActivityDTO createActivity(CreateActivityParam param);
}
