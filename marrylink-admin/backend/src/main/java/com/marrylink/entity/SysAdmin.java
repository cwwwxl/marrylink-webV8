package com.marrylink.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_admin")
public class SysAdmin implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 管理员用户名（唯一）
     */
    @TableField("username")
    private String username;

    /**
     * 密码（JSON序列化时忽略）
     */
    @TableField("password")
    @JsonIgnore
    private String password;

    /**
     * 昵称
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 头像URL
     */
    @TableField("avatar")
    private String avatar;

//    /**
//     * 手机号
//     */
//    @TableField("phone")
//    private String phone;

//    /**
//     * 邮箱
//     */
//    @TableField("email")
//    private String email;

    /**
     * 管理员状态（1:正常 0:禁用）
     */
    @TableField("status")
    private Integer status;

//    /**
//     * 管理员类型（1:普通管理员 2:超级管理员）
//     */
//    @TableField("admin_type")
//    private Integer adminType;

//    /**
//     * 最后登录时间
//     */
//    @TableField("last_login_time")
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
//    private LocalDateTime lastLoginTime;

//    /**
//     * 最后登录IP
//     */
//    @TableField("last_login_ip")
//    private String lastLoginIp;

    /**
     * 创建时间（自动填充）
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间（自动填充）
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记（0:未删除 1:已删除）
     */
    @TableField(value = "is_deleted", fill = FieldFill.INSERT)
    @TableLogic
    private Integer isDeleted;
}
