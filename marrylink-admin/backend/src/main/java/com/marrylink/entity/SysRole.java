package com.marrylink.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统角色表
 */
@Data
@TableName("sys_role")
public class SysRole {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 角色编码（唯一）
     */
    private String roleCode;
    
    /**
     * 角色名称
     */
    private String roleName;
    
    /**
     * 适用用户类型: CUSTOMER/HOST/ADMIN
     */
    private String userType;
    
    /**
     * 角色描述
     */
    private String description;
    
    /**
     * 排序
     */
    private Integer sortOrder;
    
    /**
     * 状态: 0-禁用 1-正常
     */
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer isDeleted;
}