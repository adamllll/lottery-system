package org.adam.lotterysystem.service;

public interface VerificationCodeService {
    /**
     * 发送验证码
     * @param phoneNumber
     */
    void sendVerificationCode(String phoneNumber);

    /**
     * 核验验证码
     * @param phoneNumber
     * @param code
     * @return
     */
    boolean checkVerificationCode(String phoneNumber, String code);
}
