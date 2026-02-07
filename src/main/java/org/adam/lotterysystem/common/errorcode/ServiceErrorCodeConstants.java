package org.adam.lotterysystem.common.errorcode;

public interface ServiceErrorCodeConstants {
    // 人员模块错误码
    ErrorCode REGISTER_INFO_IS_EMPTY = new ErrorCode(100, "注册信息不能为空");
    ErrorCode REGISTER_MAIL_FORMAT_ERROR = new ErrorCode(101, "注册邮箱格式错误");
    ErrorCode REGISTER_MOBILE_FORMAT_ERROR = new ErrorCode(102, "注册手机号格式错误");
    ErrorCode REGISTER_IDENTITY_ERROR = new ErrorCode(103, "注册身份信息错误");
    ErrorCode REGISTER_PASSWORD_IS_EMPTY = new ErrorCode(104, "管理员注册密码不能为空");
    ErrorCode REGISTER_PASSWORD_ERROR = new ErrorCode(105, "注册密码格式错误，密码长度不能少于6位");
    ErrorCode REGISTER_MAIL_USED = new ErrorCode(106, "注册邮箱已被使用");
    ErrorCode REGISTER_PHONE_USED = new ErrorCode(107, "注册手机号已被使用");

    // 通用校验错误码
    ErrorCode PHONE_FORMAT_ERROR = new ErrorCode(108, "手机号格式错误");

    // 活动模块错误码

    // 奖品模块错误码

    // 抽奖错误码
}
