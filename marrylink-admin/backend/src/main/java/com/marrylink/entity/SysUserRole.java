package com.marrylink.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户角色关联表
 */
@Data
@TableName("sys_user_role")
public class SysUserRole {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 账号映射ID（account_mapping.id）
     */
    private Long accountId;
    
    /**
     * 角色ID
     */
    private Long roleId;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}