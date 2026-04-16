package com.marrylink.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 自定义用户详情
 * 包含权限表ID（accountId）和业务表ID（refId）
 */
@Data
public class CustomUserDetails implements UserDetails {

    /**
     * 权限表ID（account_mapping.id）
     * 用于权限管理和角色关联
     */
    private Long accountId;

    /**
     * 业务表ID（user.id / host.id / sys_admin.id）
     * 用于业务数据查询
     */
    private Long refId;

    /**
     * 用户名（账号ID：手机号/邮箱）
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 用户类型: CUSTOMER/HOST/ADMIN
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
     * 账号状态: 0-禁用 1-正常
     */
    private Integer status;

    /**
     * 角色列表
     */
    private List<String> roles;

    /**
     * 权限列表
     */
    private List<String> permissions;

    public CustomUserDetails() {
    }

    public CustomUserDetails(Long accountId, Long refId, String username, String password, String userType, List<String> roles, List<String> permissions) {
        this.accountId = accountId;
        this.refId = refId;
        this.username = username;
        this.password = password;
        this.userType = userType;
        this.roles = roles;
        this.permissions = permissions;
    }

    public CustomUserDetails(Long accountId, Long refId, String username, String password, String userType, String realName, String phone, String email, Integer status, List<String> roles, List<String> permissions) {
        this.accountId = accountId;
        this.refId = refId;
        this.username = username;
        this.password = password;
        this.userType = userType;
        this.realName = realName;
        this.phone = phone;
        this.email = email;
        this.status = status;
        this.roles = roles;
        this.permissions = permissions;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 将角色和权限转换为 GrantedAuthority
        // 角色需要加 ROLE_ 前缀（如果还没有的话）
        List<GrantedAuthority> authorities = roles.stream()
            .map(role -> {
                // 如果角色代码已经包含 ROLE_ 前缀，直接使用；否则添加前缀
                String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                return new SimpleGrantedAuthority(roleWithPrefix);
            })
            .collect(Collectors.toList());

        // 权限直接添加
        authorities.addAll(permissions.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList()));

        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status == 1;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == 1;
    }
}
