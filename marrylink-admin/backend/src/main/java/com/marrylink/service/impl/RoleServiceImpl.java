package com.marrylink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.marrylink.entity.SysRole;
import com.marrylink.entity.SysUserRole;
import com.marrylink.mapper.SysRoleMapper;
import com.marrylink.service.RoleService;
import com.marrylink.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色服务实现
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements RoleService {
    
    private final UserRoleService userRoleService;
    
    @Override
    public List<String> getRoleCodesByAccountId(Long accountId) {
        return baseMapper.selectRoleCodesByAccountId(accountId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRolesToUser(Long accountId, List<Long> roleIds) {
        // 删除旧的角色关联
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getAccountId, accountId);
        userRoleService.remove(wrapper);
        
        // 添加新的角色关联
        List<SysUserRole> userRoles = roleIds.stream()
            .map(roleId -> {
                SysUserRole userRole = new SysUserRole();
                userRole.setAccountId(accountId);
                userRole.setRoleId(roleId);
                return userRole;
            })
            .collect(Collectors.toList());
        
        return userRoleService.saveBatch(userRoles);
    }
}