package org.adam.lotterysystem.controller;

import org.adam.lotterysystem.common.errorcode.ControllerErrorCodeConstants;
import org.adam.lotterysystem.common.exception.ControllerException;
import org.adam.lotterysystem.common.pojo.CommonResult;
import org.adam.lotterysystem.common.utils.JacksonUtil;
import org.adam.lotterysystem.controller.param.UserRegisterParam;
import org.adam.lotterysystem.controller.result.UserRegisterResult;
import org.adam.lotterysystem.service.UserService;
import org.adam.lotterysystem.service.dto.UserRegisterDTO;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;

@RestController
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;
    /**
     * 注册
     */
    //@PostMapping
    @RequestMapping(value = "/register")
    public CommonResult<UserRegisterResult> userRegister(
            @Validated @RequestBody UserRegisterParam param) {
        // 日志打印
        logger.info("用户注册，参数：{}", JacksonUtil.writeValueAsString(param));
        // 调用 Service 层进行注册
        UserRegisterDTO userRegisterDTO = userService.registerDTO(param);
        return CommonResult.success(converToUserRegisterResult(userRegisterDTO));
    }

    private UserRegisterResult converToUserRegisterResult(UserRegisterDTO userRegisterDTO) {
        UserRegisterResult result = new UserRegisterResult();
        if (null == userRegisterDTO) {
            throw new ControllerException(ControllerErrorCodeConstants.REGISTER_ERROR);
        }
        result.setUserId(userRegisterDTO.getUserId());
        return result;
    }
}
