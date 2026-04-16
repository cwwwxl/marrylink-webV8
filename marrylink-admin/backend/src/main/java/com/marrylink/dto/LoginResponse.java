package com.marrylink.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    /**
     * JWT Token
     */
    private String token;
    
    /**
     * Token类型
     */
    private String tokenType = "Bearer";
    
    /**
     * 权限表ID
     */
    private Long accountId;
    
    /**
     * 业务表ID
     */
    private Long refId;
    
    /**
     * 用户类型
     */
    private String userType;
    
    /**
     * 真实姓名
     */
    private String realName;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 角色列表
     */
    private List<String> roles;
    
    /**
     * 权限列表
     */
    private List<String> permissions;
}