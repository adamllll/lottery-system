package org.adam.lotterysystem;

import org.adam.lotterysystem.service.VerificationCodeService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class VerificationCodeServiceITest {

    private static final String TEST_PHONE_PROP = "sms.test.phone";
    private static final String TEST_CODE_PROP = "sms.code";
    private static final String TEST_OLD_CODE_PROP = "sms.old.code";
    private static final String TEST_NEW_CODE_PROP = "sms.new.code";
    private static final String DEFAULT_TEST_PHONE = "18813566041";

    @Autowired
    private VerificationCodeService verificationCodeService;

    /**
     * 正例：发送验证码后，使用手机收到的真实验证码进行核验。
     *
     * 运行方式（示例）：
     * mvn -Dtest=VerificationCodeServiceITest#testSendAndCheckWithManualCode -Dsms.test.phone=188xxxx6041 -Dsms.code=1234 test
     */
    @Test
    void testSendAndCheckWithManualCode() {
        String phoneNumber = getTestPhone();
        verificationCodeService.sendVerificationCode(phoneNumber);

        String code = System.getProperty(TEST_CODE_PROP);
        Assumptions.assumeTrue(hasText(code),
                "未提供 -D" + TEST_CODE_PROP + "，本次仅执行发送；提供验证码后会继续做断言。");

        boolean result = verificationCodeService.checkVerificationCode(phoneNumber, code.trim());
        Assertions.assertTrue(result, "真实验证码应校验通过");
    }

    /**
     * 反例1：错误验证码应校验失败。
     * 说明：该用例不依赖手工输入，传入明显错误值 "0000"。
     */
    @Test
    void testCheckWithWrongCodeShouldFail() {
        String phoneNumber = getTestPhone();
        verificationCodeService.sendVerificationCode(phoneNumber);
        boolean result = verificationCodeService.checkVerificationCode(phoneNumber, "0000");
        Assertions.assertFalse(result, "错误验证码必须校验失败");
    }

    /**
     * 反例2：验证码过期后应失败（手工用例）。
     *
     * 步骤：
     * 1) 先发送验证码并记录收到的验证码；
     * 2) 等待超过 sms.valid-time（默认 300 秒）；
     * 3) 使用 -Dsms.code=收到的验证码 进行核验，预期失败。
     *
     * 运行方式（示例）：
     * mvn -Dtest=VerificationCodeServiceITest#testCheckWithExpiredCodeShouldFailManual -Dsms.test.phone=188xxxx6041 -Dsms.code=1234 test
     */
    @Disabled("手工用例：需要等待超过验证码有效期")
    @Test
    void testCheckWithExpiredCodeShouldFailManual() {
        String phoneNumber = getTestPhone();
        String code = System.getProperty(TEST_CODE_PROP);
        Assumptions.assumeTrue(hasText(code), "请通过 -D" + TEST_CODE_PROP + " 提供待核验验证码");

        boolean result = verificationCodeService.checkVerificationCode(phoneNumber, code.trim());
        Assertions.assertFalse(result, "过期验证码应校验失败");
    }

    /**
     * 反例3：重复发送后旧验证码应失效（手工用例，依赖 duplicate-policy=1）。
     *
     * 步骤：
     * 1) 第一次发送，记录旧验证码 oldCode；
     * 2) 第二次发送，记录新验证码 newCode；
     * 3) oldCode 核验应失败；
     * 4) newCode 核验应成功。
     *
     * 运行方式（示例）：
     * mvn -Dtest=VerificationCodeServiceITest#testCheckOldCodeAfterResendShouldFailManual -Dsms.test.phone=188xxxx6041 -Dsms.old.code=1111 -Dsms.new.code=2222 test
     */
    @Disabled("手工用例：需要两次发码并手工输入旧/新验证码")
    @Test
    void testCheckOldCodeAfterResendShouldFailManual() {
        String phoneNumber = getTestPhone();
        String oldCode = System.getProperty(TEST_OLD_CODE_PROP);
        String newCode = System.getProperty(TEST_NEW_CODE_PROP);
        Assumptions.assumeTrue(hasText(oldCode), "请通过 -D" + TEST_OLD_CODE_PROP + " 提供旧验证码");
        Assumptions.assumeTrue(hasText(newCode), "请通过 -D" + TEST_NEW_CODE_PROP + " 提供新验证码");

        boolean oldResult = verificationCodeService.checkVerificationCode(phoneNumber, oldCode.trim());
        boolean newResult = verificationCodeService.checkVerificationCode(phoneNumber, newCode.trim());

        Assertions.assertFalse(oldResult, "重发后旧验证码应失效");
        Assertions.assertTrue(newResult, "重发后的新验证码应校验通过");
    }

    /**
     * 反例4：频控验证（手工用例）。
     *
     * 步骤：
     * 1) 在 sms.interval 时间内连续发送两次验证码；
     * 2) 第二次发送预期触发频控（日志中会出现频控错误码，如 FREQUENCY_FAIL）。
     *
     * 说明：当前 send 接口返回 void，频控结果主要通过日志观察。
     */
    @Disabled("手工用例：需要观察日志中的频控错误")
    @Test
    void testSendTwiceWithinIntervalShouldBeRateLimitedManual() {
        String phoneNumber = getTestPhone();
        verificationCodeService.sendVerificationCode(phoneNumber);
        verificationCodeService.sendVerificationCode(phoneNumber);
    }

    private String getTestPhone() {
        String phoneNumber = System.getProperty(TEST_PHONE_PROP);
        return hasText(phoneNumber) ? phoneNumber.trim() : DEFAULT_TEST_PHONE;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
