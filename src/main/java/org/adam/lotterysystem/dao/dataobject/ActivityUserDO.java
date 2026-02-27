package org.adam.lotterysystem.dao.dataobject;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ActivityUserDO extends BaseDO{

    // 关联活动 ID
    private Long activityId;

    // 关联的用户 ID
    private Long userId;

    // 关联的用户名
    private String userName;

    // 用户状态
    private String status;
}
