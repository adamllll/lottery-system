package org.adam.lotterysystem.controller.result;

import lombok.Data;
import org.adam.lotterysystem.controller.ActivityController;

import java.io.Serializable;
import java.util.List;

@Data
public class FindActivityListResult implements Serializable {

    // 总量
    private Integer total;

    // 记录列表
    private List<ActivityInfo> records;

    @Data
    public static class ActivityInfo implements Serializable {

        // 活动i
        private Long activityId;

        // 活动名称
        private String activityName;

        // 活动描述
        private String description;

        // 活动是否有效
        private Boolean valid;
    }
}
