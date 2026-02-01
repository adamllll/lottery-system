package org.adam.lotterysystem.common.errorcode;

public interface GlobalErrorCodeConstants {

    ErrorCode SUCCESS = new ErrorCode(200, "成功");
    ErrorCode INTERNAL_SERVER_ERROR = new ErrorCode(500, "系统异常，请稍后重试");
    ErrorCode UNKOWN = new ErrorCode(999, "未知错误");
}
