package org.adam.lotterysystem;

import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;

import java.security.Key;

@SpringBootTest
public class JWTtest {

    @Test
    public void genKey() {
        // 创建了一个密钥对象，使用HS256签名算法。
        Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

        // 将密钥编码为Base64字符串。
        String secretString = Encoders.BASE64.encode(key.getEncoded());

        System.out.println(secretString);
    }

}
