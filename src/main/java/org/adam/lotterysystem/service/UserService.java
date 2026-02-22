package org.adam.lotterysystem.service;


import org.adam.lotterysystem.controller.param.UserLoginParam;
import org.adam.lotterysystem.controller.param.UserRegisterParam;
import org.adam.lotterysystem.service.dto.UserDTO;
import org.adam.lotterysystem.service.dto.UserLoginDTO;
import org.adam.lotterysystem.service.dto.UserRegisterDTO;
import org.adam.lotterysystem.service.enums.UserIdentityEnum;

import java.util.List;

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

    /**
     * 根据身份信息查询用户信息
     * @param forName: 如果为空，则查询所有用户信息；如果不为空，则查询对应身份的用户信息
     * @return
     */
    List<UserDTO> findUserInfo(UserIdentityEnum identity);
}
