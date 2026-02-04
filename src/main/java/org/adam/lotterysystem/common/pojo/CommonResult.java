package org.adam.lotterysystem.common.pojo;

import org.adam.lotterysystem.common.errorcode.ErrorCode;
import org.adam.lotterysystem.common.errorcode.GlobalErrorCodeConstants;
import org.springframework.util.Assert;

import java.io.Serializable;

public class CommonResult<T> implements Serializable {
    /**
     * 返回的错误码
     */
    private Integer code;

    /**
     * 正常返回数据
     */
    private T data;

    /**
     * 错误码描述
     */
    private String msg;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public static <T> CommonResult<T> success(T data) {
        CommonResult<T> result = new CommonResult<>();
        result.code = GlobalErrorCodeConstants.SUCCESS.getCode();
        result.data = data;
        result.msg = "";
        return result;
    }

    public static <T> CommonResult<T> error(Integer code, String msg) {
        Assert.isTrue(!GlobalErrorCodeConstants.SUCCESS.getCode().equals(code), "code不是错误异常");
        CommonResult<T> result = new CommonResult<>();
        result.code = code;
        result.msg = msg;
        return result;
    }

    public static <T> CommonResult<T> error(String msg) {
        return error(GlobalErrorCodeConstants.UNKOWN.getCode(), msg);
    }

    public static <T> CommonResult<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMsg());
    }
}
