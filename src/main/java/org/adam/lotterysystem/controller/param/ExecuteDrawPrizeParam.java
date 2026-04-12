package org.adam.lotterysystem.controller.param;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExecuteDrawPrizeParam {

    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    @NotNull(message = "奖项ID不能为空")
    private Long prizeId;
}
