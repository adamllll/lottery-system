package org.adam.lotterysystem.service.impl;

import org.adam.lotterysystem.common.errorcode.ServiceErrorCodeConstants;
import org.adam.lotterysystem.common.exception.ServiceException;
import org.adam.lotterysystem.common.utils.RegexUtil;
import org.adam.lotterysystem.service.VerificationCodeService;
import org.springframework.stereotype.Service;

@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {
    @Override
    public void sendVerificationCode(String phoneNumber) {
        // 校验手机号
        if (!RegexUtil.checkMobile(phoneNumber)) {
            throw new ServiceException(ServiceErrorCodeConstants.PHONE_FORMAT_ERROR);
        }
        // 生成随机验证码
        

        // 发送验证码

        // 缓存验证码
    }

    @Override
    public String getVerificationCode(String phoneNumber) {
        return "";
    }
}
