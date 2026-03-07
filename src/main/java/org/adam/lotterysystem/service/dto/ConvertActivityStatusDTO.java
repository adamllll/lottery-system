package org.adam.lotterysystem.service.dto;

import lombok.Data;
import org.adam.lotterysystem.service.activitystatus.ActivityStatusManager;
import org.adam.lotterysystem.service.enums.ActivityPrizeStatusEnum;
import org.adam.lotterysystem.service.enums.ActivityStatusEnum;
import org.adam.lotterysystem.service.enums.ActivityUSerStatusEnum;

import java.util.List;

@Data
public class ConvertActivityStatusDTO {

    // 活动 id
    private Long activityId;

    // 目标活动状态
    private ActivityStatusEnum targetActivityStatus;

    // 奖品 id
    private Long prizeId;

    // 目标奖品状态
    private ActivityPrizeStatusEnum targetPrizeStatus;

    // 人员 id列表
    private List<Long> userIds;

    // 目标用户状态
    private ActivityUSerStatusEnum targetUserStatus;

}
