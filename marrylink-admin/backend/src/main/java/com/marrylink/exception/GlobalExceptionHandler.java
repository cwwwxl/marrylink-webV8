package com.marrylink.exception;

import com.marrylink.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理系统中的各类异常，返回标准的JSON响应
 *
 * @author MarryLink Team
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: URI={}, Code={}, Message={}",
                request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.error(e.getCode().toString(), e.getMessage());
    }

    /**
     * 处理认证异常（登录失败等）
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthenticationException(AuthenticationException e, HttpServletRequest request) {
        log.warn("认证异常: URI={}, Message={}", request.getRequestURI(), e.getMessage());

        String message = "认证失败";
        if (e instanceof BadCredentialsException) {
            message = "用户名或密码错误";
        }

        return Result.error("401", message);
    }

    /**
     * 处理授权异常（无权限访问）
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("授权异常: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        return Result.error("403", "无权限访问该资源");
    }

    /**
     * 处理参数校验异常（@Valid注解）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("参数校验异常: URI={}, Message={}", request.getRequestURI(), errorMessage);
        return Result.error("400", "参数校验失败: " + errorMessage);
    }

    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e, HttpServletRequest request) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("参数绑定异常: URI={}, Message={}", request.getRequestURI(), errorMessage);
        return Result.error("400", "参数绑定失败: " + errorMessage);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法参数异常: URI={}, Message={}", request.getRequestURI(), e.getMessage());
        return Result.error("400", "参数错误: " + e.getMessage());
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常: URI={}", request.getRequestURI(), e);
        return Result.error("500", "系统内部错误，请联系管理员");
    }

    /**
     * 处理其他未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: URI={}", request.getRequestURI(), e);
        return Result.error("500", "系统内部错误: " + e.getMessage());
    }
}
