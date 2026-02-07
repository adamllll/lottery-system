package org.adam.lotterysystem.service;

public interface VerificationCodeService {
    /**
     * 发送验证码
     * @param phoneNumber
     */
    void sendVerificationCode(String phoneNumber);

    /**
     * 获取验证码
     * @param phoneNumber
     * @return
     */
    String getVerificationCode(String phoneNumber);
}
