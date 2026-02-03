package org.adam.lotterysystem;

import org.adam.lotterysystem.dao.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SqlTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    void mailCount() {
        int count = userMapper.countByMail("123@mail.com");
        System.out.println("mailCount = " + count);
    }
}
