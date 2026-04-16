package com.marrylink.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.marrylink.entity.SysUserRole;
import com.marrylink.mapper.SysUserRoleMapper;
import com.marrylink.service.UserRoleService;
import org.springframework.stereotype.Service;

/**
 * 用户角色关联服务实现
 */
@Service
public class UserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> 
    implements UserRoleService {
}