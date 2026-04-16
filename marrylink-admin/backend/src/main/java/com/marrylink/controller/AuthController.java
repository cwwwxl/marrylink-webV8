package com.marrylink.controller;

import com.marrylink.common.Result;
import com.marrylink.dto.ChangePasswordRequest;
import com.marrylink.dto.LoginRequest;
import com.marrylink.dto.LoginResponse;
import com.marrylink.dto.RegisterRequest;
import com.marrylink.entity.AccountMapping;
import com.marrylink.entity.SysRole;
import com.marrylink.entity.SysUserRole;
import com.marrylink.entity.User;
import com.marrylink.enums.UserType;
import com.marrylink.security.CustomUserDetails;
import com.marrylink.security.JwtTokenProvider;
import com.marrylink.service.AccountMappingService;
import com.marrylink.service.IUserService;
import com.marrylink.service.PermissionService;
import com.marrylink.service.RoleService;
import com.marrylink.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 认证控制器
 * 处理登录、注册、修改密码等操作
 * 登录时将用户信息存入Redis（key=token），登出时删除Redis中的token
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AccountMappingService accountMappingService;
    private final IUserService userService;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * Web端登录（管理员和主持人）
     */
    @PostMapping("/admin/login")
    public Result<LoginResponse> webLogin(@RequestParam String username,
                                         @RequestParam String password,
                                         @RequestParam(required = false) String userType,
                                         @RequestParam(required = false) String captchaKey,
                                         @RequestParam(required = false) String captchaCode) {
        log.info("Web login attempt: username={}, userType={}", username, userType);
        
        try {
            // 默认为管理员，也支持主持人登录
            String type = (userType != null && !userType.isEmpty()) ? userType : "ADMIN";
            String accountId = username + ":" + type;
            
            // 执行认证
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(accountId, password)
            );
            
            // 获取用户详情
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            
            // 生成 Token
            String token = tokenProvider.generateToken(userDetails);
            
            // 将用户信息存入Redis，key为token
            tokenProvider.saveTokenToRedis(token, userDetails);
            
            // 构建响应（兼容Web端旧格式）
            LoginResponse response = LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .accountId(userDetails.getAccountId())
                .refId(userDetails.getRefId())
                .userType(userDetails.getUserType())
                .realName(userDetails.getRealName())
                .phone(userDetails.getPhone())
                .email(userDetails.getEmail())
                .roles(userDetails.getRoles())
                .permissions(userDetails.getPermissions())
                .build();
            
            log.info("Web login success: accountId={}, refId={}, userType={}",
                     userDetails.getAccountId(), userDetails.getRefId(), userDetails.getUserType());
            
            return Result.ok(response);
            
        } catch (Exception e) {
            log.error("Web login failed: username={}, userType={}", username, userType, e);
            return Result.error("登录失败：用户名或密码错误");
        }
    }
    
    /**
     * 用户登录（App端）
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        log.info("User login attempt: accountId={}, userType={}", 
                 request.getAccountId(), request.getUserType());
        
        try {
            // 去除首尾空格
            String accountId = request.getAccountId().trim();
            String userType = request.getUserType().trim();
            String password = request.getPassword().trim();

            // 构建用户名（格式: accountId:userType）
            String username = accountId + ":" + userType;
            
            // 执行认证
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );
            
            // 获取用户详情
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            
            // 生成 Token
            String token = tokenProvider.generateToken(userDetails);
            
            // 将用户信息存入Redis，key为token
            tokenProvider.saveTokenToRedis(token, userDetails);
            
            // 构建响应
            LoginResponse response = LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .accountId(userDetails.getAccountId())
                .refId(userDetails.getRefId())
                .userType(userDetails.getUserType())
                .realName(userDetails.getRealName())
                .phone(userDetails.getPhone())
                .email(userDetails.getEmail())
                .roles(userDetails.getRoles())
                .permissions(userDetails.getPermissions())
                .build();
            
            log.info("User login success: accountId={}, refId={}, userType={}", 
                     userDetails.getAccountId(), userDetails.getRefId(), userDetails.getUserType());
            
            return Result.ok(response);
            
        } catch (Exception e) {
            log.error("Login failed: accountId={}, userType={}", 
                     request.getAccountId(), request.getUserType(), e);
            return Result.error("登录失败：用户名或密码错误");
        }
    }
    
    /**
     * 用户注册（新人端）
     */
    @PostMapping("/register")
    public Result<LoginResponse> register(@Validated @RequestBody RegisterRequest request) {
        log.info("User register attempt: phone={}", request.getPhone());
        
        try {
            // 1. 检查手机号是否已注册
            AccountMapping existingAccount = accountMappingService.getByAccountAndType(
                request.getPhone(), UserType.CUSTOMER.getCode()
            );
            if (existingAccount != null) {
                return Result.error("该手机号已注册");
            }
            
            // 2. 解析姓名（支持"新娘&新郎"格式）
            String[] names = request.getName().split("&");
            String brideName = names[0].trim();
            String groomName = names.length > 1 ? names[1].trim() : "";
            
            // 3. 创建新人用户
            User user = new User();
            user.setPhone(request.getPhone());
            user.setBrideName(brideName);
            user.setGroomName(groomName);
            user.setStatus(1);
            userService.save(user);
            
            // 4. 创建账号映射
            AccountMapping accountMapping = new AccountMapping();
            accountMapping.setAccountId(request.getPhone());
            accountMapping.setUserType(UserType.CUSTOMER.getCode());
            accountMapping.setRefId(user.getId());
            accountMapping.setPassword(passwordEncoder.encode(request.getPassword()));
            accountMapping.setStatus(1);
            accountMappingService.save(accountMapping);
            
            // 5. 分配默认角色（ROLE_CUSTOMER）
            SysRole customerRole = roleService.lambdaQuery()
                .eq(SysRole::getRoleCode, "ROLE_CUSTOMER")
                .one();
            if (customerRole != null) {
                SysUserRole userRole = new SysUserRole();
                userRole.setAccountId(accountMapping.getId());
                userRole.setRoleId(customerRole.getId());
                userRoleService.save(userRole);
            }
            
            // 6. 查询角色和权限
            List<String> roles = roleService.getRoleCodesByAccountId(accountMapping.getId());
            List<String> permissions = permissionService.getPermissionCodesByAccountId(accountMapping.getId());
            
            // 7. 生成JWT Token
            CustomUserDetails userDetails = new CustomUserDetails();
            userDetails.setAccountId(accountMapping.getId());
            userDetails.setRefId(accountMapping.getRefId());
            userDetails.setUsername(accountMapping.getAccountId());
            userDetails.setUserType(accountMapping.getUserType());
            userDetails.setRealName(brideName + " & " + groomName);
            userDetails.setPhone(request.getPhone());
            userDetails.setRoles(roles);
            userDetails.setPermissions(permissions);
            
            String token = tokenProvider.generateToken(userDetails);
            
            // 将用户信息存入Redis，key为token
            tokenProvider.saveTokenToRedis(token, userDetails);
            
            // 8. 构建响应
            LoginResponse response = LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .accountId(accountMapping.getId())
                .refId(accountMapping.getRefId())
                .userType(accountMapping.getUserType())
                .realName(brideName + " & " + groomName)
                .phone(request.getPhone())
                .roles(roles)
                .permissions(permissions)
                .build();
            
            log.info("User register success: phone={}, refId={}", request.getPhone(), user.getId());
            
            return Result.ok(response);
            
        } catch (Exception e) {
            log.error("Register failed: phone={}", request.getPhone(), e);
            return Result.error("注册失败：" + e.getMessage());
        }
    }
    
    /**
     * 修改密码
     * 验证旧密码正确后更新为新密码
     */
    @PostMapping("/change-password")
    public Result<Void> changePassword(@Validated @RequestBody ChangePasswordRequest request) {
        log.info("Change password attempt");

        try {
            // 1. 校验新密码和确认密码是否一致
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return Result.error("新密码与确认密码不一致");
            }

            // 2. 从SecurityContext获取当前登录用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
                return Result.error("用户未登录");
            }
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long accountId = userDetails.getAccountId();

            // 3. 查询账号信息
            AccountMapping account = accountMappingService.getById(accountId);
            if (account == null) {
                return Result.error("账号不存在");
            }

            // 4. 验证旧密码是否正确
            if (!passwordEncoder.matches(request.getOldPassword(), account.getPassword())) {
                return Result.error("旧密码不正确");
            }

            // 5. 更新密码
            boolean success = accountMappingService.changePassword(accountId, request.getNewPassword());
            if (!success) {
                return Result.error("密码修改失败");
            }

            log.info("Password changed successfully for accountId: {}", accountId);
            return Result.ok();

        } catch (Exception e) {
            log.error("Change password failed", e);
            return Result.error("密码修改失败：" + e.getMessage());
        }
    }

    /**
     * 退出登录
     * 从Redis中删除token，使token立即失效
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            // 从Redis中删除token，使其立即失效
            tokenProvider.removeTokenFromRedis(token);
            log.info("User logout, token removed from Redis");
        } else {
            log.warn("Logout request without valid Authorization header");
        }
        return Result.ok();
    }
}