package com.marrylink.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统权限表
 */
@Data
@TableName("sys_permission")
public class SysPermission {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 权限编码（唯一）
     */
    private String permissionCode;
    
    /**
     * 权限名称
     */
    private String permissionName;
    
    /**
     * 资源类型: MENU/API/BUTTON
     */
    private String resourceType;
    
    /**
     * 资源路径
     */
    private String resourcePath;
    
    /**
     * 父权限ID
     */
    private Long parentId;
    
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