package org.adam.lotterysystem.controller.result;

import lombok.Data;
import org.adam.lotterysystem.service.dto.ActivityDetailDTO;
import org.adam.lotterysystem.service.enums.ActivityPrizeStatusEnum;
import org.adam.lotterysystem.service.enums.ActivityPrizeTiersEnum;
import org.adam.lotterysystem.service.enums.ActivityStatusEnum;
import org.adam.lotterysystem.service.enums.ActivityUSerStatusEnum;
import org.apache.catalina.User;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class GetActivityDetailResult implements Serializable {
    // 活动信息
    private Long activityId;

    // 活动名称
    private String activityName;

    // 活动描述
    private String description;

    // 活动是否有效
    private Boolean valid;

    // 奖品信息(列表)
    private List<Prize> prizes;

    // 人员信息(列表)
    private List<User> users;

    @Data
    public static class Prize {
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

        /**
         * 奖品等级
         * @see ActivityPrizeTiersEnum#getMessage()
         */
        private String tiers;

        // 奖品数量
        private Long prizeAmount;

        // 奖品是否有效
        private Boolean valid;

    }

    @Data
    public static class User {
        // 用户 id
        private Long userId;

        // 用户名称
        private String userName;

        // 用户是否有效
        private Boolean valid;
    }
}
