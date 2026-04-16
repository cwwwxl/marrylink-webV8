package com.marrylink.security;

import com.marrylink.entity.*;
import com.marrylink.enums.UserType;
import com.marrylink.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 自定义用户详情服务
 * 支持多用户类型加载（CUSTOMER/HOST/ADMIN）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    
    private final AccountMappingService accountMappingService;
    private final IUserService userService;
    private final IHostService hostService;
    private final ISysAdminService adminService;
    private final RoleService roleService;
    private final PermissionService permissionService;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("Loading user by username: {}", username);
        
        // username 格式: accountId:userType (例如: 13800138000:HOST)
        String[] parts = username.split(":");
        if (parts.length != 2) {
            throw new UsernameNotFoundException("用户名格式错误，应为: accountId:userType");
        }
        
        String accountId = parts[0];
        String userType = parts[1];
        
        log.info("Attempting to load account: accountId={}, userType={}", accountId, userType);

        // 1. 查询账号映射
        AccountMapping account = accountMappingService.getByAccountAndType(accountId, userType);
        if (account == null) {
            log.error("Account not found in database: accountId={}, userType={}", accountId, userType);
            throw new UsernameNotFoundException("用户不存在: " + accountId);
        }
        
        log.info("Account found: id={}, passwordHash={}", account.getId(), account.getPassword());
        
        if (account.getStatus() == 0) {
            throw new UsernameNotFoundException("账号已被禁用");
        }
        
        // 2. 构建用户详情
        CustomUserDetails userDetails = new CustomUserDetails();
        userDetails.setAccountId(account.getId());  // 权限表ID
        userDetails.setRefId(account.getRefId());   // 业务表ID
        userDetails.setUsername(accountId);
        userDetails.setPassword(account.getPassword());
        userDetails.setUserType(userType);
        userDetails.setStatus(account.getStatus());
        
        // 3. 加载用户基本信息
        loadUserInfo(userDetails, userType, account.getRefId());
        
        // 4. 加载角色和权限
        List<String> roles = roleService.getRoleCodesByAccountId(account.getId());
        List<String> permissions = permissionService.getPermissionCodesByAccountId(account.getId());
        
        userDetails.setRoles(roles);
        userDetails.setPermissions(permissions);
        
        log.info("User loaded successfully. AccountId: {}, RefId: {}, UserType: {}, Roles: {}", 
                 account.getId(), account.getRefId(), userType, roles);
        
        return userDetails;
    }
    
    /**
     * 根据用户类型加载用户信息
     */
    private void loadUserInfo(CustomUserDetails userDetails, String userType, Long refId) {
        try {
            UserType type = UserType.fromCode(userType);
            
            switch (type) {
                case CUSTOMER:
                    User user = userService.getById(refId);
                    if (user != null) {
                        userDetails.setRealName(user.getBrideName() + " & " + user.getGroomName());
                        userDetails.setPhone(user.getPhone());
                    }
                    break;
                    
                case HOST:
                    Host host = hostService.getById(refId);
                    if (host != null) {
                        userDetails.setRealName(host.getName());
                        userDetails.setPhone(host.getPhone());
                        userDetails.setEmail(host.getEmail());
                    }
                    break;
                    
                case ADMIN:
                    SysAdmin admin = adminService.getById(refId);
                    if (admin != null) {
                        userDetails.setRealName(admin.getNickname());
                        userDetails.setPhone(admin.getUsername());
                    }
                    break;
            }
        } catch (Exception e) {
            log.error("Failed to load user info for refId: {}, userType: {}", refId, userType, e);
        }
    }
}