package org.adam.lotterysystem.controller.result;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class FindPrizeListResult implements Serializable {

    // 奖品列表总数
    private Integer total;

    // 奖品列表记录
    private List<PrizeInfo> records;

    @Data
    public static class PrizeInfo implements Serializable {
        // 奖品id
        private String prizeId;
        // 奖品名称
        private String prizeName;

        // 奖品描述
        private String description;

        // 奖品价格
        private BigDecimal price;

        // 奖品图片URL
        private String imageUrl;
    }
}
