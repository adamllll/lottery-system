package org.adam.lotterysystem;

import org.adam.lotterysystem.common.utils.SMSUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SMSUtilTest {

    @Autowired
    private SMSUtil smsUtil;

    @Test
    void smsTest() {
        smsUtil.sendMessage("100001", "18813566041", "{\"code\":\"##code##\",\"min\":\"5\"}");
    }
}
