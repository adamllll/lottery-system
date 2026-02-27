package org.adam.lotterysystem.service.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ActivityStatusEnum {

    RUNNING(1, "活动进行中"),
    END(2, "活动已结束");

    private final Integer code;
    private final String message;

    public static ActivityStatusEnum forName(String name) {
        for (ActivityStatusEnum activityStatusEnum : ActivityStatusEnum.values()) {
            if (activityStatusEnum.name().equals(name)) {
                return activityStatusEnum;
            }
        }
        return null;
    }
}
