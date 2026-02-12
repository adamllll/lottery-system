package org.adam.lotterysystem.service.dto;

import lombok.Data;
import org.adam.lotterysystem.service.enums.UserIdentityEnum;

@Data
public class UserLoginDTO {
    /**
     * JWT令牌
     */
    private String token;
    /**
     * 登录身份
     */
    private UserIdentityEnum identity;
}
