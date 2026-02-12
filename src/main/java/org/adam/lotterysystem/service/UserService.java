package org.adam.lotterysystem.service;


import org.adam.lotterysystem.controller.param.UserLoginParam;
import org.adam.lotterysystem.controller.param.UserRegisterParam;
import org.adam.lotterysystem.service.dto.UserLoginDTO;
import org.adam.lotterysystem.service.dto.UserRegisterDTO;

public interface UserService {
    /**
     * 用户注册
     */
    UserRegisterDTO registerDTO(UserRegisterParam param);

    /**
     * 用户登录
     *  支持两种登录方式：密码登录，手机号+验证码登录
     * @param param
     * @return
     */
    UserLoginDTO login(UserLoginParam param);
}
