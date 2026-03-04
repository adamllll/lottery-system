package org.adam.lotterysystem.service;

import org.adam.lotterysystem.controller.param.CreateActivityParam;
import org.adam.lotterysystem.controller.param.PageParam;
import org.adam.lotterysystem.controller.result.FindActivityListResult;
import org.adam.lotterysystem.service.dto.ActivityDTO;
import org.adam.lotterysystem.service.dto.ActivityDetailDTO;
import org.adam.lotterysystem.service.dto.CreateActivityDTO;
import org.adam.lotterysystem.service.dto.PageListDTO;


public interface ActivityService {

    // 创建活动
    CreateActivityDTO createActivity(CreateActivityParam param);

    // 查询活动(摘要)列表
    PageListDTO<ActivityDTO> findActivityList(PageParam param);

    // 查询活动详情
    ActivityDetailDTO getActivityDetail(Long activityId);
}
