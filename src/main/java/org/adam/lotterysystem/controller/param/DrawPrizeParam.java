package org.adam.lotterysystem.controller.param;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class DrawPrizeParam {

    // 活动 ID
    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    // 奖品 ID
    @NotNull(message = "奖品ID不能为空")
    private Long prizeId;

    // 奖品等级
    @NotBlank(message = "奖品等级不能为空")
    private String prizeTiers;

    // 中奖时间
    @NotNull(message = "中奖时间不能为空")
    private Date winningTime;

    // 中奖用户列表
    @NotEmpty(message = "中奖用户列表不能为空")
    @Valid
    private List<Winner> winnerList;

    @Data
    public static class Winner {
        // 用户 ID
        @NotNull(message = "中奖用户ID不能为空")
        private Long userId;
        // 用户名
        @NotBlank(message = "中奖用户名不能为空")
        private String userName;
    }
}
