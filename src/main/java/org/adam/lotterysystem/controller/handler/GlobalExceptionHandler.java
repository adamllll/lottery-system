package org.adam.lotterysystem.controller.handler;

import org.adam.lotterysystem.common.errorcode.GlobalErrorCodeConstants;
import org.adam.lotterysystem.common.exception.ControllerException;
import org.adam.lotterysystem.common.exception.ServiceException;
import org.adam.lotterysystem.common.pojo.CommonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.rmi.ServerException;

@RestControllerAdvice // 可以捕获全局异常
public class GlobalExceptionHandler {
    private final static Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(value = ServiceException.class)
    public CommonResult<?> serviceException(ServiceException e) {
        // 打印错误日志
        logger.error("serviceException:", e); // 不需要占位符
        // 构造错误结果
        return CommonResult.error(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode(), e.getMessage());
    }

    @ExceptionHandler(value = ControllerException.class)
    public CommonResult<?> controllerException(ControllerException e) {
        // 打印错误日志
        logger.error("controllerException:", e); // 不需要占位符
        // 构造错误结果
        return CommonResult.error(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode(), e.getMessage());
    }

    @ExceptionHandler(value = Exception.class)
    public CommonResult<?> Exception(Exception e) {
        // 打印错误日志
        logger.error("服务异常:", e); // 不需要占位符
        // 构造错误结果
        return CommonResult.error(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode(), e.getMessage());
    }
}
