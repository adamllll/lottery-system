package org.adam.lotterysystem.dao.dataobject;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ActivityPrizeDO extends BaseDO{

    // 关联活动 ID
    private Long activityId;

    // 关联的奖品 ID
    private Long prizeId;

    // 奖品数量
    private Long prizeAmount;

    // 奖品等级
    private String prizeTiers;

    // 奖品状态
    private String status;
}
