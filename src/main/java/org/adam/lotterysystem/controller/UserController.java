package org.adam.lotterysystem.controller;

import org.adam.lotterysystem.common.errorcode.ControllerErrorCodeConstants;
import org.adam.lotterysystem.common.exception.ControllerException;
import org.adam.lotterysystem.common.pojo.CommonResult;
import org.adam.lotterysystem.controller.param.UserRegisterParam;
import org.adam.lotterysystem.controller.result.UserRegisterResult;
import org.adam.lotterysystem.service.UserService;
import org.adam.lotterysystem.service.VerificationCodeService;
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

    @Autowired
    private VerificationCodeService verificationCodeService;

    /**
     * 注册
     */
    //@PostMapping
    @RequestMapping(value = "/register")
    public CommonResult<UserRegisterResult> userRegister(
            @Validated @RequestBody UserRegisterParam param) {
        logger.info("用户注册，phoneNumber={}", param.getPhoneNumber());
        boolean verifyPassed = verificationCodeService.checkVerificationCode(
                param.getPhoneNumber(),
                param.getVerificationCode());
        if (!verifyPassed) {
            throw new ControllerException(ControllerErrorCodeConstants.VERIFICATION_CODE_ERROR);
        }
        // 调用 Service 层进行注册
        UserRegisterDTO userRegisterDTO = userService.registerDTO(param);
        return CommonResult.success(converToUserRegisterResult(userRegisterDTO));
    }

    /**
     * 发送验证码
     * @param phoneNumber
     * @return
     */
    @RequestMapping("/verification-code/send")
    public CommonResult<Boolean> sendVerificationCode(String phoneNumber) {
        logger.info("发送验证码，参数：{}", phoneNumber);
        verificationCodeService.sendVerificationCode(phoneNumber);
        return CommonResult.success(true);
    }

    /**
     * 核验验证码
     * @param phoneNumber 手机号
     * @param code 验证码
     * @return 核验结果
     */
    @RequestMapping("/verification-code/check")
    public CommonResult<Boolean> checkVerificationCode(String phoneNumber, String code) {
        logger.info("核验验证码，phoneNumber={}", phoneNumber);
        return CommonResult.success(verificationCodeService.checkVerificationCode(phoneNumber, code));
    }

    /**
     * 转换注册返回结果
     * @param userRegisterDTO
     * @return
     */
    private UserRegisterResult converToUserRegisterResult(UserRegisterDTO userRegisterDTO) {
        UserRegisterResult result = new UserRegisterResult();
        if (null == userRegisterDTO) {
            throw new ControllerException(ControllerErrorCodeConstants.REGISTER_ERROR);
        }
        result.setUserId(userRegisterDTO.getUserId());
        return result;
    }
}
