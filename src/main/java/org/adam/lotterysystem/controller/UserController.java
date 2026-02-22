package org.adam.lotterysystem.controller;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.adam.lotterysystem.common.errorcode.ControllerErrorCodeConstants;
import org.adam.lotterysystem.common.exception.ControllerException;
import org.adam.lotterysystem.common.pojo.CommonResult;
import org.adam.lotterysystem.common.utils.JacksonUtil;
import org.adam.lotterysystem.common.utils.JWTUtil;
import org.adam.lotterysystem.controller.param.AdminCreateUserParam;
import org.adam.lotterysystem.controller.param.ShortPasswordLogin;
import org.adam.lotterysystem.controller.param.UserPasswordLoginParam;
import org.adam.lotterysystem.controller.param.UserRegisterParam;
import org.adam.lotterysystem.controller.result.BaseUserInfoResult;
import org.adam.lotterysystem.controller.result.UserLoginResult;
import org.adam.lotterysystem.controller.result.UserRegisterResult;
import org.adam.lotterysystem.service.UserService;
import org.adam.lotterysystem.service.VerificationCodeService;
import org.adam.lotterysystem.service.dto.UserDTO;
import org.adam.lotterysystem.service.dto.UserLoginDTO;
import org.adam.lotterysystem.service.dto.UserRegisterDTO;
import org.adam.lotterysystem.service.enums.UserIdentityEnum;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
     * 管理员新增普通用户（后台入口，不需要验证码）
     */
    @RequestMapping(value = "/admin/user/add")
    public CommonResult<UserRegisterResult> adminAddUser(
            @Validated @RequestBody AdminCreateUserParam param,
            HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new ControllerException(403, "仅管理员可以新增用户");
        }

        UserRegisterParam registerParam = new UserRegisterParam();
        registerParam.setName(param.getName());
        registerParam.setMail(param.getMail());
        registerParam.setPhoneNumber(param.getPhoneNumber());
        registerParam.setIdentity(UserIdentityEnum.NORMAL.name());

        UserRegisterDTO userRegisterDTO = userService.registerDTO(registerParam);
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
     * 密码登录
     * @param param
     * @return
     */
    @RequestMapping("/password/login")
    public CommonResult<UserLoginResult> userPasswordLogin(@Validated @RequestBody UserPasswordLoginParam param) {
        logger.info("密码登录, UserPasswordLoginParam={}", JacksonUtil.writeValueAsString(param));
        UserLoginDTO userLoginDTO = userService.login(param);
        return CommonResult.success(converToUserLoginResult(userLoginDTO));
    }

    /**
     * 验证码登录
     * @param param
     * @return
     */
    @RequestMapping("/message/login")
    public CommonResult<UserLoginResult> shortPasswordLogin(@Validated @RequestBody ShortPasswordLogin param) {
        logger.info("密码登录, shortPasswordLogin={}", JacksonUtil.writeValueAsString(param));
        UserLoginDTO userLoginDTO = userService.login(param);
        return CommonResult.success(converToUserLoginResult(userLoginDTO));
    }

    @RequestMapping("/base-user/find-list")
    public CommonResult<List<BaseUserInfoResult>> findBaseUserInfoList(String identity) {
        logger.info("查询基础用户信息列表，identity={}", identity);
        List<UserDTO> userDTOList = userService.findUserInfo(UserIdentityEnum.forName(identity));
        return CommonResult.success(converToList(userDTOList));
    }

    private List<BaseUserInfoResult> converToList(List<UserDTO> userDTOList) {
        if (CollectionUtils.isEmpty(userDTOList)) {
            return Arrays.asList();
        }

        return userDTOList.stream().map(
                userDTO -> {
                    BaseUserInfoResult result = new BaseUserInfoResult();
                    result.setUserId(userDTO.getUserId());
                    result.setUserName(userDTO.getUserName());
                    result.setIdentity(userDTO.getIdentity().name());
                    return result;
                }
        ).collect(Collectors.toList());
    }

    private UserLoginResult converToUserLoginResult(UserLoginDTO userLoginDTO) {
        if (null == userLoginDTO) {
            throw new ControllerException(ControllerErrorCodeConstants.LOGIN_ERROR);
        }
        UserLoginResult result = new UserLoginResult();
        result.setToken(userLoginDTO.getToken());
        result.setIdentity(userLoginDTO.getIdentity().name());
        return result;
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

    /**
     * 校验当前请求是否为管理员身份
     */
    private boolean isAdmin(HttpServletRequest request) {
        String token = request.getHeader("user_token");
        Claims claims = JWTUtil.parseJWT(token);
        if (claims == null) {
            return false;
        }
        Object identity = claims.get("identity");
        if (identity == null) {
            return false;
        }
        return UserIdentityEnum.ADMIN.name().equalsIgnoreCase(identity.toString());
    }
}
