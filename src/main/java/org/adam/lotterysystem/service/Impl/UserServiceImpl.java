package org.adam.lotterysystem.service.Impl;

import jakarta.validation.constraints.NotBlank;
import org.adam.lotterysystem.common.errorcode.ServiceErrorCodeConstants;
import org.adam.lotterysystem.common.exception.ServiceException;
import org.adam.lotterysystem.common.utils.RegexUtil;
import org.adam.lotterysystem.controller.param.UserRegisterParam;
import org.adam.lotterysystem.dao.mapper.UserMapper;
import org.adam.lotterysystem.service.UserService;
import org.adam.lotterysystem.service.dto.UserRegisterDTO;
import org.adam.lotterysystem.service.enums.UserIdentityEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Override
    public UserRegisterDTO registerDTO(UserRegisterParam param) {
        // 校验注册信息
        checkRegisterInfo(param);
        // 加密私密数据(构造dao层对象，调用dao层保存数据)

        // 保存数据

        // 构造返回结果


        UserRegisterDTO userRegisterDTO = new UserRegisterDTO();
        userRegisterDTO.setUserId(12L);
        return userRegisterDTO;
    }

    private void checkRegisterInfo(UserRegisterParam param) {
        if (null == param) {
            throw new ServiceException(ServiceErrorCodeConstants.REGISTER_INFO_IS_EMPTY);
        }
        // 校验邮箱格式 xxx@xxx.xxx
        if (RegexUtil.checkMail(param.getMail())) {
            throw new ServiceException(ServiceErrorCodeConstants.REGISTER_MAIL_FORMAT_ERROR);
        }
        // 校验手机号格式
        if (RegexUtil.checkMobile(param.getPhoneNumber())) {
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
        if (StringUtils.hasText(param.getPassword()) && RegexUtil.checkPassword(param.getPassword())) {
            throw new ServiceException(ServiceErrorCodeConstants.REGISTER_PASSWORD_ERROR);
        }
        // 校验邮箱是否被使用
        if (checkMailUsed(param.getMail())) {
            throw new ServiceException(ServiceErrorCodeConstants.REGISTER_MAIL_USED);
        }
        // 校验手机号是否被使用
        if (checkPhoneNumberUsed(param.getPhoneNumber())) {
            throw new ServiceException(ServiceErrorCodeConstants.REGISTER_Phone_USED);
        }
    }

    private boolean checkMailUsed(String mail) {
        int count = userMapper.countByMail(mail);
        return count > 0;
    }

    private boolean checkPhoneNumberUsed(@NotBlank(message = "手机号不能为空！") String phoneNumber) {

    }
}
