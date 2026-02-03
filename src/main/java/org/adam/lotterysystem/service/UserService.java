package org.adam.lotterysystem.service;


import org.adam.lotterysystem.controller.param.UserRegisterParam;
import org.adam.lotterysystem.service.dto.UserRegisterDTO;

public interface UserService {
    /**
     * 注册
     */
    UserRegisterDTO registerDTO(UserRegisterParam param);
}
