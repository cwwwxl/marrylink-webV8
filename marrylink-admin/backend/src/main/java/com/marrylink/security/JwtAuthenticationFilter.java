package com.marrylink.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * JWT 认证过滤器
 * 从Token中提取用户信息并设置到SecurityContext
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        // 跳过认证接口的JWT验证（但修改密码、登出等需要认证的接口除外）
        String requestURI = request.getRequestURI();
        boolean isAuthPath = requestURI.startsWith("/api/v1/auth/") || requestURI.startsWith("/api/v1/public/");
        boolean needsAuth = requestURI.equals("/api/v1/auth/change-password") || requestURI.equals("/api/v1/auth/logout");
        if (isAuthPath && !needsAuth) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                // 使用带Redis检查的完整验证：JWT签名有效 + Redis中存在（未登出）
                if (tokenProvider.validateTokenWithRedis(jwt)) {
                    // 从 Token 中提取信息
                String username = tokenProvider.getUsernameFromToken(jwt);
                Long accountId = tokenProvider.getAccountIdFromToken(jwt);
                Long refId = tokenProvider.getRefIdFromToken(jwt);
                String userType = tokenProvider.getUserTypeFromToken(jwt);
                List<String> roles = tokenProvider.getRolesFromToken(jwt);
                List<String> permissions = tokenProvider.getPermissionsFromToken(jwt);

                // 构建权限列表
                // 角色需要加 ROLE_ 前缀（如果还没有的话）
                List<SimpleGrantedAuthority> authorities = Stream.concat(
                    roles.stream().map(role -> {
                        // 如果角色代码已经包含 ROLE_ 前缀，直接使用；否则添加前缀
                        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                        return new SimpleGrantedAuthority(roleWithPrefix);
                    }),
                    permissions.stream().map(SimpleGrantedAuthority::new)
                ).collect(Collectors.toList());

                // 创建CustomUserDetails对象
                CustomUserDetails userDetails = new CustomUserDetails(
                    accountId, refId, username, "", userType, roles, permissions
                );

                // 创建认证对象，使用CustomUserDetails作为principal
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 设置到 SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 将用户信息存入请求属性，方便后续使用
                request.setAttribute("accountId", accountId);  // 权限表ID
                request.setAttribute("refId", refId);          // 业务表ID
                request.setAttribute("userType", userType);
                request.setAttribute("roles", roles);
                request.setAttribute("permissions", permissions);

                log.debug("Set authentication for user: {}, accountId: {}, refId: {}",
                         username, accountId, refId);
                } else {
                    log.warn("Token validation failed for: {}", jwt);
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中获取 JWT Token
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
