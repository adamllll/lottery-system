package org.adam.lotterysystem.service.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter // 生成 getter方法
@AllArgsConstructor // 生成包含所有字段的构造函数
public enum UserIdentityEnum {
    ADMIN("管理员"),
    NOMORAL("普通用户");

    private final String message; // 描述信息

    public static UserIdentityEnum forName(String name) {
        for (UserIdentityEnum identity : UserIdentityEnum.values()) {
            if (identity.name().equalsIgnoreCase(name)) {
                return identity;
            }
        }
        return null;
    }
}
