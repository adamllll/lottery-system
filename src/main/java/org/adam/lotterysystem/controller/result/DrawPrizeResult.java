package org.adam.lotterysystem.controller.result;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class DrawPrizeResult {

    private Long activityId;
    private Long prizeId;
    private String prizeName;
    private String prizeTier;
    private Date winningTime;
    private List<Winner> winnerList;

    @Data
    public static class Winner {
        private Long userId;
        private String userName;
    }
}
