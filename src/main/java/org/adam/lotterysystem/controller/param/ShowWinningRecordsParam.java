package org.adam.lotterysystem.controller.param;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class ShowWinningRecordsParam implements Serializable {

    // 活动 ID
    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    // 奖品 ID
    private Long prizeId;
}
