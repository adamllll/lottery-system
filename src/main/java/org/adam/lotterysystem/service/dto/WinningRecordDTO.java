package org.adam.lotterysystem.service.dto;

import lombok.Data;
import org.adam.lotterysystem.service.enums.ActivityPrizeTiersEnum;

import java.util.Date;

@Data
public class WinningRecordDTO {
    // 中奖者 ID
    private Long winnerId;

    // 中奖者名称
    private String winnerName;

    // 奖品名称
    private String prizeName;

    // 奖品等级
    private ActivityPrizeTiersEnum prizeTier;

    // 中奖时间
    private Date winningTime;
}
