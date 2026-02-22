package org.adam.lotterysystem.service.dto;

import lombok.Data;
import org.adam.lotterysystem.service.enums.UserIdentityEnum;

@Data
public class UserDTO {
    // 用户 Id
    private Long userId;
    // 用户名
    private String userName;
    // 邮箱
    private String email;
    // 手机号
    private String phoneNumber;
    // 身份信息
    private UserIdentityEnum identity;
}
