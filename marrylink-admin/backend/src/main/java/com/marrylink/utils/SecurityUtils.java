package com.marrylink.utils;

import com.marrylink.enums.UserType;
import com.marrylink.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Security工具类
 * 用于获取当前登录用户的信息
 *
 * @author MarryLink Team
 */
public class SecurityUtils {

    /**
     * 从SecurityContext获取当前认证信息
     */
    public static Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * 获取当前登录用户的UserDetails
     */
    public static Optional<CustomUserDetails> getCurrentUserDetails() {
        return getAuthentication()
                .filter(auth -> auth.getPrincipal() instanceof CustomUserDetails)
                .map(auth -> (CustomUserDetails) auth.getPrincipal());
    }

    /**
     * 获取当前用户的accountId（权限表ID）
     */
    public static Long getCurrentAccountId() {
        return getCurrentUserDetails()
                .map(CustomUserDetails::getAccountId)
                .orElse(null);
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername() {
        return getCurrentUserDetails()
                .map(CustomUserDetails::getUsername)
                .orElse(null);
    }

    /**
     * 获取当前用户的refId（业务表ID）
     */
    public static Long getCurrentRefId() {
        return getCurrentUserDetails()
                .map(CustomUserDetails::getRefId)
                .orElse(null);
    }

    /**
     * 获取当前用户的用户类型
     */
    public static UserType getCurrentUserType() {
        return getCurrentUserDetails()
                .map(CustomUserDetails::getUserType)
                .map(UserType::fromCode)
                .orElse(null);
    }

    /**
     * 获取当前用户的用户类型字符串
     */
    public static String getCurrentUserTypeString() {
        return getCurrentUserDetails()
                .map(CustomUserDetails::getUserType)
                .orElse(null);
    }

    /**
     * 获取当前用户的真实姓名
     */
    public static String getCurrentRealName() {
        return getCurrentUserDetails()
                .map(CustomUserDetails::getRealName)
                .orElse(null);
    }

    /**
     * 判断当前用户是否为新人
     */
    public static boolean isCustomer() {
        return UserType.CUSTOMER.equals(getCurrentUserType());
    }

    /**
     * 判断当前用户是否为主持人
     */
    public static boolean isHost() {
        return UserType.HOST.equals(getCurrentUserType());
    }

    /**
     * 判断当前用户是否为管理员
     */
    public static boolean isAdmin() {
        return UserType.ADMIN.equals(getCurrentUserType());
    }

    /**
     * 从Request Attributes获取accountId
     * （用于Filter中设置的属性）
     */
    public static Long getAccountIdFromRequest() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            Object accountId = request.getAttribute("accountId");
            if (accountId instanceof Long) {
                return (Long) accountId;
            }
        }
        return null;
    }

    /**
     * 从Request Attributes获取refId
     */
    public static Long getRefIdFromRequest() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            Object refId = request.getAttribute("refId");
            if (refId instanceof Long) {
                return (Long) refId;
            }
        }
        return null;
    }

    /**
     * 从Request Attributes获取userType
     */
    public static UserType getUserTypeFromRequest() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            Object userType = request.getAttribute("userType");
            if (userType instanceof UserType) {
                return (UserType) userType;
            }
        }
        return null;
    }

    /**
     * 获取当前HTTP请求
     */
    private static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 检查当前用户是否拥有指定权限
     */
    public static boolean hasPermission(String permission) {
        return getCurrentUserDetails()
                .map(userDetails -> userDetails.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals(permission)))
                .orElse(false);
    }

    /**
     * 检查当前用户是否拥有指定角色
     */
    public static boolean hasRole(String role) {
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return getCurrentUserDetails()
                .map(userDetails -> userDetails.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals(roleWithPrefix)))
                .orElse(false);
    }

    /**
     * 检查当前用户是否拥有任意一个指定权限
     */
    public static boolean hasAnyPermission(String... permissions) {
        for (String permission : permissions) {
            if (hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查当前用户是否拥有任意一个指定角色
     */
    public static boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }
}
