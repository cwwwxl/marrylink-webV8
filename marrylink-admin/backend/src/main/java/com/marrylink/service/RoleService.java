package com.marrylink.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.marrylink.entity.SysRole;

import java.util.List;

/**
 * 角色服务接口
 */
public interface RoleService extends IService<SysRole> {
    
    /**
     * 根据账号ID获取角色编码列表
     */
    List<String> getRoleCodesByAccountId(Long accountId);
    
    /**
     * 为用户分配角色
     */
    boolean assignRolesToUser(Long accountId, List<Long> roleIds);
}