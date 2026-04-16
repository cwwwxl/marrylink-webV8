package com.marrylink.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登录请求DTO
 */
@Data
public class LoginRequest {
    
    /**
     * 账号ID（手机号/邮箱）
     */
    @NotBlank(message = "账号不能为空")
    private String accountId;
    
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
    
    /**
     * 用户类型: CUSTOMER/HOST/ADMIN
     */
    @NotBlank(message = "用户类型不能为空")
    private String userType;
}