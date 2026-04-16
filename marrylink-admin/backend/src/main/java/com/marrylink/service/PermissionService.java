package com.marrylink.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.marrylink.entity.SysPermission;

import java.util.List;

/**
 * 权限服务接口
 */
public interface PermissionService extends IService<SysPermission> {
    
    /**
     * 根据账号ID获取权限编码列表
     */
    List<String> getPermissionCodesByAccountId(Long accountId);
}