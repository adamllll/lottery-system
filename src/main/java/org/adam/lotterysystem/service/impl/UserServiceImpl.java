package org.adam.lotterysystem.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import jakarta.validation.constraints.NotBlank;
import org.adam.lotterysystem.common.errorcode.ServiceErrorCodeConstants;
import org.adam.lotterysystem.common.exception.ServiceException;
import org.adam.lotterysystem.common.utils.JWTUtil;
import org.adam.lotterysystem.common.utils.RegexUtil;
import org.adam.lotterysystem.controller.param.ShortPasswordLogin;
import org.adam.lotterysystem.controller.param.UserLoginParam;
import org.adam.lotterysystem.controller.param.UserPasswordLoginParam;
import org.adam.lotterysystem.controller.param.UserRegisterParam;
import org.adam.lotterysystem.dao.dataobject.Encrypt;
import org.adam.lotterysystem.dao.dataobject.UserDO;
import org.adam.lotterysystem.dao.mapper.UserMapper;
import org.adam.lotterysystem.service.UserService;
import org.adam.lotterysystem.service.dto.UserLoginDTO;
import org.adam.lotterysystem.service.dto.UserRegisterDTO;
import org.adam.lotterysystem.service.enums.UserIdentityEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Override
    public UserRegisterDTO registerDTO(UserRegisterParam param) {
        // 校验注册信息
        checkRegisterInfo(param);
        // 加密私密数据(构造dao层对象，调用dao层保存数据)
        UserDO userDO = new UserDO();
        userDO.setUserName(param.getName());
        userDO.setEmail(param.getMail());
        userDO.setPhoneNumber(new Encrypt(param.getPhoneNumber()));
        userDO.setIdentity(param.getIdentity());

        if (StringUtils.hasText(param.getPassword())) {
            userDO.setPassword(DigestUtil.sha256Hex(param.getPassword())); // SHA-256加密
        }

        // 保存数据
        userMapper.insert(userDO);

        // 构造返回结果
        UserRegisterDTO userRegisterDTO = new UserRegisterDTO();
        userRegisterDTO.setUserId(userDO.getId());
        return userRegisterDTO;
    }

    private void checkRegisterInfo(UserRegisterParam param) {
        if (null == param) {
            throw new ServiceException(ServiceErrorCodeConstants.REGISTER_INFO_IS_EMPTY);
        }
        // 校验邮箱格式 xxx@xxx.xxx
        if (!RegexUtil.checkMail(param.getMail())) {
            throw new ServiceException(ServiceErrorCodeConstants.REGISTER_MAIL_FORMAT_ERROR);
        }
        // 校验手机号格式
        if (!RegexUtil.checkMobile(param.getPhoneNumber())) {
            throw new ServiceException(ServiceErrorCodeConstants.REGISTER_MOBILE_FORMAT_ERROR);
        }
        // 校验身份信息
        if (null == UserIdentityEnum.forName(param.getIdentity())) {
            throw new ServiceException(ServiceErrorCodeConstants.REGISTER_IDENTITY_ERROR);
        }

        // 校验管理员密码(必填)
        if (param.getIdentity().equals(UserIdentityEnum.ADMIN.name()) &&
                !StringUtils.hasText(param.getPassword())) {
            throw new ServiceException(ServiceErrorCodeConstants.REGISTER_PASSWORD_IS_EMPTY);
        }

        // 密码校验 >= 6位
        if (StringUtils.hasText(param.getPassword()) && !RegexUtil.checkPassword(param.getPassword())) {
            throw new ServiceException(ServiceErrorCodeConstants.REGISTER_PASSWORD_ERROR);
        }
        // 校验邮箱是否被使用
        if (checkMailUsed(param.getMail())) {
            throw new ServiceException(ServiceErrorCodeConstants.REGISTER_MAIL_USED);
        }
        // 校验手机号是否被使用
        if (checkPhoneNumberUsed(param.getPhoneNumber())) {
            throw new ServiceException(ServiceErrorCodeConstants.REGISTER_PHONE_USED);
        }
    }

    /**
     * 校验邮箱是否被使用
     * @param mail
     * @return
     */
    private boolean checkMailUsed(String mail) {
        int count = userMapper.countByMail(mail);
        return count > 0;
    }

    /**
     * 校验手机号是否被使用
     * @param phoneNumber
     * @return
     */
    private boolean checkPhoneNumberUsed(@NotBlank(message = "手机号不能为空！") String phoneNumber) {
        int count = userMapper.countByPhoneNumber(new Encrypt(phoneNumber));
        return count > 0;
    }

    @Override
    public UserLoginDTO login(UserLoginParam param) {
        UserLoginDTO userLoginDTO;
        // 类型检查与类型转换(java14及以上 支持instanceof模式匹配)
        if (param instanceof UserPasswordLoginParam loginParam) {
            // 密码登录流程
            userLoginDTO = loginByUserPassword(loginParam);
        } else if (param instanceof ShortPasswordLogin loginParam) {
            // 短信验证码登录
            userLoginDTO = loginByShortMessage(loginParam);
        } else {
            throw new ServiceException(ServiceErrorCodeConstants.LOGIN_INFO_NOT_EXIST);
        }
        return userLoginDTO;
    }

    /**
     * 密码登录流程
     * @param loginParam
     * @return
     */
    private UserLoginDTO loginByUserPassword(UserPasswordLoginParam loginParam) {
        UserDO userDO = null;
        // 1. 根据登录名查询用户信息(登录名可以是手机号或邮箱)
        if (RegexUtil.checkMail(loginParam.getLoginName())) {
            // 邮箱登录
            // 根据邮箱查询用户表
            userDO = userMapper.selectBymail(loginParam.getLoginName());
        } else if (RegexUtil.checkMobile(loginParam.getLoginName())) {
            // 手机号登录
            // 根据手机号查询用户表
            userDO = userMapper.selectByPhoneNumber(new Encrypt(loginParam.getLoginName()));

        } else {
            throw new ServiceException(ServiceErrorCodeConstants.LOGIN_TYPE_NOT_EXIST);
        }
        // 2. 校验用户信息是否存在
        if (null == userDO) {
            throw new ServiceException(ServiceErrorCodeConstants.LOGIN_USERINFO_NOT_EXIST);
        } else if (StringUtils.hasText(loginParam.getMandatoryIdentity())
                && !loginParam.getMandatoryIdentity().equalsIgnoreCase(userDO.getIdentity()) ) {
                // 强制身份登录，身份校验不通过
            throw new ServiceException(ServiceErrorCodeConstants.LOGIN_IDENTITY_ERROR);
        } else if (DigestUtil.sha256Hex(loginParam.getPassword()).equals(userDO.getPassword())) {
            // 校验密码不通过
            throw new ServiceException(ServiceErrorCodeConstants.LOGIN_PASSWORD_ERROR);
        }
        // 塞入返回值 JWT令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", userDO.getId());
        claims.put("identity", userDO.getIdentity());
        String token = JWTUtil.genJwt(claims);

        UserLoginDTO userLoginDTO = new UserLoginDTO();
        userLoginDTO.setToken(token);
        userLoginDTO.setIdentity(UserIdentityEnum.forName(userDO.getIdentity()));
        return userLoginDTO;
    }

    /**
     * 短信验证码登录流程
     * @param loginParam
     * @return
     */
    private UserLoginDTO loginByShortMessage(ShortPasswordLogin loginParam) {
        // TODO 短信验证码登录流程
        return null;
    }
}
