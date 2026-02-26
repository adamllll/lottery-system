package org.adam.lotterysystem.controller.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class CreateUserByActivityParam implements Serializable {

    // 活动关联的用户 id
    @NotNull(message = "活动关联的用户 id 不能为空!")
    private Long userId;

    // 活动关联的用户名称
    @NotBlank(message = "活动关联的用户名称不能为空!")
    private String userName;
}
