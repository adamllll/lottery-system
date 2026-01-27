package org.adam.lotterysystem.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.adam.lotterysystem.common.errorcode.ErrorCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ControllerException extends RuntimeException{
    /**
     * 异常码
     * @see org.adam.lotterysystem.common.errorcode.ControllerErrorCodeConstants
     */
    private Integer code;
    // 异常消息
    private String message;

    // 无参构造方法 为了序列化
    public ControllerException() {

    }
    public ControllerException(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public ControllerException(ErrorCode errorCode) {
        this.code = errorCode.getCode();
        this.message = errorCode.getMsg();
    }

}
