package org.adam.lotterysystem.controller.result;

import lombok.Data;

import java.io.Serializable;

@Data
public class CreateActivityResult implements Serializable {

    // 活动 ID
    private Long activityId;
}
