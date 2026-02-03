package org.adam.lotterysystem.controller.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
@Data
public class UserRegisterParam implements Serializable {
    // 用户名
    @NotBlank(message = "姓名不能为空！")
    private String name;
    // 邮箱
    @NotBlank(message = "邮箱不能为空！")
    private String mail;
    // 手机号
    @NotBlank(message = "手机号不能为空！")
    private String phoneNumber;
    // 密码
    private String password;
    // 身份信息
    @NotBlank(message = "身份信息不能为空！")
    private String identity;
}
