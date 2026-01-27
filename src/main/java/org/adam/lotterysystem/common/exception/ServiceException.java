package org.adam.lotterysystem.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.adam.lotterysystem.common.errorcode.ErrorCode;

// @Data 生成自己的 equals hashcode不生成继承的属性
@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceException extends RuntimeException{
    /**
     * 异常码
     * @see org.adam.lotterysystem.common.errorcode.ControllerErrorCodeConstants
     */
    private Integer code;
    // 异常消息
    private String message;

    public ServiceException() {
    }

    public ServiceException(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public ServiceException(ErrorCode errorCode) {
        this.code = errorCode.getCode();
        this.message = errorCode.getMsg();
    }
}
