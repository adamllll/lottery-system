package org.adam.lotterysystem;

import org.adam.lotterysystem.service.UserService;
import org.adam.lotterysystem.service.dto.UserDTO;
import org.adam.lotterysystem.service.enums.UserIdentityEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class UserTest {

    @Autowired
    private UserService userService;

    @Test
    void findBaseUserLsit() {
        List<UserDTO> userDTOList = userService.findUserInfo(UserIdentityEnum.ADMIN);
        for (UserDTO userDTO : userDTOList) {
            System.out.println(userDTO.getUserId() + " " + userDTO.getUserName() + " " + userDTO.getIdentity());
        }
    }
}
