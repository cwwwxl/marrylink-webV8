package com.marrylink.exception;

import lombok.Getter;

/**
 * 业务异常类
 * 用于处理业务逻辑中的异常情况
 * 
 * @author MarryLink Team
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 构造函数 - 使用默认错误码500
     */
    public BusinessException(String message) {
        super(message);
        this.code = 500;
        this.message = message;
    }

    /**
     * 构造函数 - 指定错误码和消息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造函数 - 指定错误码、消息和原因
     */
    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    /**
     * 常用业务异常 - 参数错误
     */
    public static BusinessException paramError(String message) {
        return new BusinessException(400, message);
    }

    /**
     * 常用业务异常 - 未授权
     */
    public static BusinessException unauthorized(String message) {
        return new BusinessException(401, message);
    }

    /**
     * 常用业务异常 - 禁止访问
     */
    public static BusinessException forbidden(String message) {
        return new BusinessException(403, message);
    }

    /**
     * 常用业务异常 - 资源不存在
     */
    public static BusinessException notFound(String message) {
        return new BusinessException(404, message);
    }

    /**
     * 常用业务异常 - 服务器内部错误
     */
    public static BusinessException serverError(String message) {
        return new BusinessException(500, message);
    }
}