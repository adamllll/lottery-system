package org.adam.lotterysystem.service.impl;

import org.adam.lotterysystem.common.errorcode.ServiceErrorCodeConstants;
import org.adam.lotterysystem.common.exception.ServiceException;
import org.adam.lotterysystem.common.utils.RegexUtil;
import org.adam.lotterysystem.common.utils.SMSUtil;
import org.adam.lotterysystem.service.VerificationCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private static final String VERIFICATION_CODE_TEMPLATE_CODE = "100001";
    private static final String VERIFICATION_TEMPLATE_PARAM = "{\"code\":\"##code##\",\"min\":\"5\"}";

    @Autowired
    private SMSUtil smsUtil;

    @Override
    public void sendVerificationCode(String phoneNumber) {
        // 校验手机号
        if (!RegexUtil.checkMobile(phoneNumber)) {
            throw new ServiceException(ServiceErrorCodeConstants.PHONE_FORMAT_ERROR);
        }
        smsUtil.sendMessage(
                VERIFICATION_CODE_TEMPLATE_CODE,
                phoneNumber,
                VERIFICATION_TEMPLATE_PARAM);
    }


    @Override
    public boolean checkVerificationCode(String phoneNumber, String code) {
        // 校验手机号
        if (!RegexUtil.checkMobile(phoneNumber)) {
            throw new ServiceException(ServiceErrorCodeConstants.PHONE_FORMAT_ERROR);
        }
        // 测试环境：允许使用固定验证码 888888
        if ("888888".equals(code)) {
            return true;
        }
        return smsUtil.checkVerifyCode(phoneNumber, code);
    }
}
