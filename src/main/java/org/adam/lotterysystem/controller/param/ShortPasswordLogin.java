package org.adam.lotterysystem.controller.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ShortPasswordLogin extends UserLoginParam{
    /**
     * 手机号登录，手机号必填
     */
    @NotBlank(message = "手机号不能为空")
    private String loginMobile;
    /**
     * 验证码登录，验证码必填
     */
    @NotBlank(message = "验证码不能为空")
    private String verificationCode;
}
