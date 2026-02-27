package org.adam.lotterysystem.service.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ActivityUSerStatusEnum {

    INIT(1, "初始化"),
    END(2, "已被抽取");

    private final Integer code;
    private final String message;

    public static ActivityUSerStatusEnum forName(String name) {
        for (ActivityUSerStatusEnum activityUSerStatusEnum : ActivityUSerStatusEnum.values()) {
            if (activityUSerStatusEnum.name().equals(name)) {
                return activityUSerStatusEnum;
            }
        }
        return null;
    }
}
