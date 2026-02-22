package org.adam.lotterysystem.controller.result;

import lombok.Data;

import java.io.Serializable;

@Data
public class BaseUserInfoResult implements Serializable {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 身份信息
     */
    private String identity;
}
