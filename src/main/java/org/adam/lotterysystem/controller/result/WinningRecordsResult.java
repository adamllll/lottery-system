package org.adam.lotterysystem.controller.result;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class WinningRecordsResult implements Serializable {

    // 中奖者ID
    private Long winnerId;

    // 中奖者名称
    private String winnerName;

    // 奖品名称
    private String prizeName;

    // 奖品等级
    private String prizeTier;

    // 中奖时间
    private Date winningTime;
}
