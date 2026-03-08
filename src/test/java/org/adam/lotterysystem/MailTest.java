package org.adam.lotterysystem;

import org.adam.lotterysystem.common.utils.MailUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MailTest {

    @Autowired
    private MailUtil mailUtil;

    @Test
    void sendMailTest() {
        mailUtil.sendSampleMail("luojj0401@gmail.com", "测试邮件", "这是一封测试邮件，请忽略！");
    }
}
