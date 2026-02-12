package org.adam.lotterysystem.controller.result;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserLoginResult implements Serializable {
    // JWT令牌
    private String token;
    // 登录身份
    private  String identity;
}
