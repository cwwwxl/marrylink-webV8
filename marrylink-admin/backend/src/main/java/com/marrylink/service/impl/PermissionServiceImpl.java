package com.marrylink.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.marrylink.entity.SysPermission;
import com.marrylink.mapper.SysPermissionMapper;
import com.marrylink.service.PermissionService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 权限服务实现
 */
@Service
public class PermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission> 
    implements PermissionService {
    
    @Override
    public List<String> getPermissionCodesByAccountId(Long accountId) {
        return baseMapper.selectPermissionCodesByAccountId(accountId);
    }
}