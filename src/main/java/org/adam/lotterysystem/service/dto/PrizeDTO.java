package org.adam.lotterysystem.service.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PrizeDTO {

    // 奖品 id
    private Long id;

    // 奖品名称
    private String name;

    // 图片索引
    private String imageUrl;

    // 奖品价格
    private BigDecimal price;

    // 奖品描述
    private String description;
}
