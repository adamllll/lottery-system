package org.adam.lotterysystem.service.dto;

import lombok.Data;
import org.adam.lotterysystem.service.enums.ActivityPrizeStatusEnum;
import org.adam.lotterysystem.service.enums.ActivityPrizeTiersEnum;
import org.adam.lotterysystem.service.enums.ActivityStatusEnum;
import org.adam.lotterysystem.service.enums.ActivityUSerStatusEnum;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ActivityDetailDTO {
    // 活动信息
    private Long activityId;

    // 活动名称
    private String activityName;

    // 活动描述
    private String description;

    // 活动状态
    private ActivityStatusEnum status;

    // 活动是否有效
    public Boolean valid() {
        return status.equals(ActivityStatusEnum.RUNNING);
    }

    // 奖品信息(列表)
    private List<PrizeDTO> prizeDTOList;

    // 人员信息(列表)
    private List<UserDTO> userDTOList;

    @Data
    public static class PrizeDTO {
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

        // 奖品等级
        private ActivityPrizeTiersEnum tiers;

        // 奖品数量
        private Long prizeAmount;

        // 奖品状态
        private ActivityPrizeStatusEnum status;

        public Boolean valid() {
            return status.equals(ActivityPrizeStatusEnum.INIT);
        }
    }

    @Data
    public static class UserDTO {
        // 用户 id
        private Long userId;

        // 用户名称
        private String userName;

        // 用户状态
        private ActivityUSerStatusEnum status;

        public Boolean valid() {
            return status.equals(ActivityUSerStatusEnum.INIT);
        }
    }
}
