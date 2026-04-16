package com.marrylink.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 注册请求DTO
 */
@Data
public class RegisterRequest {
    
    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    /**
     * 姓名（新人端可以是"新娘&新郎"格式）
     */
    @NotBlank(message = "姓名不能为空")
    private String name;
    
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}