package org.adam.lotterysystem.service.dto;

import lombok.Data;
import org.adam.lotterysystem.service.enums.ActivityPrizeTiersEnum;

import java.util.Date;
import java.util.List;

@Data
public class DrawPrizeDTO {

    private Long activityId;
    private Long prizeId;
    private String prizeName;
    private ActivityPrizeTiersEnum prizeTier;
    private Date winningTime;
    private List<WinnerDTO> winnerList;

    @Data
    public static class WinnerDTO {
        private Long userId;
        private String userName;
    }
}
