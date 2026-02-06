package org.adam.lotterysystem.common.utils;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SMSUtil {

    private static final Logger logger = LoggerFactory.getLogger(SMSUtil.class);

    @Value("${sms.access-key-id:}")
    private String accessKeyId;

    @Value("${sms.access-key-secret:}")
    private String accessKeySecret;

    @Value("${sms.endpoint:dypnsapi.aliyuncs.com}")
    private String endpoint;

    @Value("${sms.sign-name:}")
    private String signName;

    @Value("${sms.template-code:}")
    private String defaultTemplateCode;

    @Value("${sms.scheme-name:默认方案}")
    private String schemeName;

    @Value("${sms.country-code:86}")
    private String countryCode;

    @Value("${sms.code-length:4}")
    private Long codeLength;

    @Value("${sms.valid-time:300}")
    private Long validTime;

    @Value("${sms.duplicate-policy:1}")
    private Long duplicatePolicy;

    @Value("${sms.interval:60}")
    private Long interval;

    @Value("${sms.code-type:1}")
    private Long codeType;

    @Value("${sms.auto-retry:1}")
    private Long autoRetry;

    /**
     * 发送短信验证码（号码认证服务）。
     * 兼容旧教程中的 sendMessage 调用方式：
     * 1) 入参 templateCode 优先；
     * 2) 入参为空时回退到配置 sms.template-code。
     */
    public void sendMessage(String templateCode, String phoneNumbers, String templateParam) {
        if (!StringUtils.hasText(phoneNumbers)) {
            logger.error("发送验证码失败，手机号为空");
            return;
        }
        if (!StringUtils.hasText(templateParam)) {
            logger.error("发送验证码失败，模板参数为空，phone={}", phoneNumbers);
            return;
        }
        if (!StringUtils.hasText(signName)) {
            logger.error("发送验证码失败，sms.sign-name 不能为空。请填写控制台“赠送签名”。");
            return;
        }

        String actualTemplateCode = StringUtils.hasText(templateCode) ? templateCode : defaultTemplateCode;
        if (!StringUtils.hasText(actualTemplateCode)) {
            logger.error("发送验证码失败，缺少模板编码。请传入 templateCode 或配置 sms.template-code。");
            return;
        }

        try {
            Client client = createClient();
            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                    .setSchemeName(schemeName)
                    .setCountryCode(countryCode)
                    .setPhoneNumber(phoneNumbers)
                    .setSignName(signName)
                    .setTemplateCode(actualTemplateCode)
                    .setTemplateParam(templateParam)
                    .setCodeLength(codeLength)
                    .setValidTime(validTime)
                    .setDuplicatePolicy(duplicatePolicy)
                    .setInterval(interval)
                    .setAutoRetry(autoRetry);

            if (templateParam.contains("##code##")) {
                request.setCodeType(codeType);
            }

            RuntimeOptions runtime = new RuntimeOptions();
            SendSmsVerifyCodeResponse response = client.sendSmsVerifyCodeWithOptions(request, runtime);
            if (response != null
                    && response.getBody() != null
                    && "OK".equals(response.getBody().getCode())
                    && Boolean.TRUE.equals(response.getBody().getSuccess())) {
                logger.info("向{}发送验证码成功，templateCode={}", phoneNumbers, actualTemplateCode);
                return;
            }

            String message = response == null || response.getBody() == null
                    ? "响应为空"
                    : response.getBody().getMessage();
            logger.error("向{}发送验证码失败，templateCode={}，失败原因：{}", phoneNumbers, actualTemplateCode, message);
        } catch (TeaException error) {
            logger.error("向{}发送验证码失败，templateCode={}", phoneNumbers, actualTemplateCode, error);
        } catch (Exception ex) {
            logger.error("向{}发送验证码异常，templateCode={}", phoneNumbers, actualTemplateCode, ex);
        }
    }

    /**
     * 核验短信验证码。
     */
    public boolean checkVerifyCode(String phoneNumber, String verifyCode) {
        if (!StringUtils.hasText(phoneNumber) || !StringUtils.hasText(verifyCode)) {
            return false;
        }
        try {
            Client client = createClient();
            CheckSmsVerifyCodeRequest request = new CheckSmsVerifyCodeRequest()
                    .setSchemeName(schemeName)
                    .setCountryCode(countryCode)
                    .setPhoneNumber(phoneNumber)
                    .setVerifyCode(verifyCode);
            CheckSmsVerifyCodeResponse response = client.checkSmsVerifyCodeWithOptions(request, new RuntimeOptions());
            return response != null
                    && response.getBody() != null
                    && "OK".equals(response.getBody().getCode())
                    && Boolean.TRUE.equals(response.getBody().getSuccess())
                    && response.getBody().getModel() != null
                    && "PASS".equals(response.getBody().getModel().getVerifyResult());
        } catch (TeaException error) {
            logger.error("核验验证码失败，phone={}", phoneNumber, error);
            return false;
        } catch (Exception ex) {
            logger.error("核验验证码异常，phone={}", phoneNumber, ex);
            return false;
        }
    }

    /**
     * 初始化号码认证服务 Client。
     * 支持两种方式：
     * 1) 配置了 AK/SK：使用配置鉴权；
     * 2) 未配置 AK/SK：走阿里云 Credentials 默认链。
     */
    private Client createClient() throws Exception {
        Config config = new Config();
        if (StringUtils.hasText(accessKeyId) && StringUtils.hasText(accessKeySecret)) {
            config.setAccessKeyId(accessKeyId);
            config.setAccessKeySecret(accessKeySecret);
        } else {
            com.aliyun.credentials.Client credential = new com.aliyun.credentials.Client();
            config.setCredential(credential);
        }
        config.setEndpoint(endpoint);
        return new Client(config);
    }
}
