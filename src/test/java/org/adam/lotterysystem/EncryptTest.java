package org.adam.lotterysystem;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.symmetric.AES;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;

@SpringBootTest
public class EncryptTest {

    // 密码 hash加密 sha256
    @Test
    void sha256Test() {
        String encrypt = DigestUtil.sha256Hex("123456789");
        System.out.println("加密后的结果:" + encrypt);

    }
    // 手机号 对称加密 aes
    @Test
    void aesTest() {
        // 密钥 16(128) 24(192) 32(256) 位
        byte[] key = "1234567890123456".getBytes(StandardCharsets.UTF_8);
        // 加密
        AES aes = SecureUtil.aes(key);
        String encrypt = aes.encryptHex("123456789");
        System.out.println("经过aes加密后的结果:" + encrypt);
        // 解密
        System.out.println("经过aes解密后的结果:" + aes.decryptStr(encrypt));


    }
}
