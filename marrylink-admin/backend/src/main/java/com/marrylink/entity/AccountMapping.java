package com.marrylink.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账号映射表
 * 统一管理所有用户类型的账号和权限
 */
@Data
@TableName("account_mapping")
public class AccountMapping {
    
    /**
     * 权限表ID（主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 账号ID（手机号/邮箱）
     */
    private String accountId;
    
    /**
     * 用户类型: CUSTOMER/HOST/ADMIN
     */
    private String userType;
    
    /**
     * 业务表ID（user.id / host.id / sys_admin.id）
     */
    private Long refId;
    
    /**
     * 密码（BCrypt加密）
     */
    private String password;
    
    /**
     * 账号状态: 0-禁用 1-正常
     */
    private Integer status;
    
    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 逻辑删除: 0-未删除 1-已删除
     */
    @TableLogic
    private Integer isDeleted;
}