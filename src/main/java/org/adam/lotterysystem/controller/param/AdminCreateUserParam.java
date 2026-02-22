package org.adam.lotterysystem.controller.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class AdminCreateUserParam implements Serializable {

    @NotBlank(message = "用户名不能为空！")
    private String name;

    @NotBlank(message = "用户邮箱不能为空！")
    private String mail;

    @NotBlank(message = "用户电话号码不能为空！")
    private String phoneNumber;
}
