package org.adam.lotterysystem.common.errorcode;

public interface ControllerErrorCodeConstants {
    // 人员模块错误码
    ErrorCode REGISTER_ERROR = new ErrorCode(100, "用户注册失败");
    ErrorCode VERIFICATION_CODE_ERROR = new ErrorCode(101, "验证码错误或已过期");
    ErrorCode LOGIN_ERROR = new ErrorCode(102, "登录失败，用户名或密码错误");

    // 活动模块错误码

    // 奖品模块错误码

    // 抽奖错误码
}
