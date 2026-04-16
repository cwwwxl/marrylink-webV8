package com.marrylink.security;

import cn.hutool.json.JSONUtil;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * JWT Token 提供者
 * Token中包含权限表ID（accountId）和业务表ID（refId）
 * 登录时将用户信息存入Redis（key=token, value=用户信息），登出时删除
 */
@Slf4j
@Component
public class JwtTokenProvider {

    /**
     * Redis中token的key前缀
     */
    private static final String TOKEN_PREFIX = "auth:token:";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 生成 Token
     * 包含 accountId（权限表ID）和 refId（业务表ID）
     */
    public String generateToken(CustomUserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("accountId", userDetails.getAccountId());  // 权限表ID
        claims.put("refId", userDetails.getRefId());          // 业务表ID
        claims.put("userType", userDetails.getUserType());
        claims.put("realName", userDetails.getRealName());
        claims.put("roles", userDetails.getRoles());
        claims.put("permissions", userDetails.getPermissions());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
            .setClaims(claims)
            .setSubject(userDetails.getUsername())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS512)
            .compact();

        log.info("Token generated for user: {}, accountId: {}, refId: {}",
                 userDetails.getUsername(), userDetails.getAccountId(), userDetails.getRefId());

        return token;
    }

    /**
     * 将用户信息存入Redis，key为token，过期时间与JWT一致
     *
     * @param token       JWT token
     * @param userDetails 用户详情信息
     */
    public void saveTokenToRedis(String token, CustomUserDetails userDetails) {
        String redisKey = TOKEN_PREFIX + token;
        // 构建存储到Redis的用户信息Map
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("accountId", userDetails.getAccountId());
        userInfo.put("refId", userDetails.getRefId());
        userInfo.put("username", userDetails.getUsername());
        userInfo.put("userType", userDetails.getUserType());
        userInfo.put("realName", userDetails.getRealName());
        userInfo.put("phone", userDetails.getPhone());
        userInfo.put("email", userDetails.getEmail());
        userInfo.put("roles", userDetails.getRoles());
        userInfo.put("permissions", userDetails.getPermissions());

        // 使用Hutool JSONUtil序列化为JSON字符串存入Redis
        String jsonValue = JSONUtil.toJsonStr(userInfo);
        stringRedisTemplate.opsForValue().set(redisKey, jsonValue, expiration, TimeUnit.MILLISECONDS);
        log.info("Token saved to Redis for user: {}, accountId: {}", userDetails.getUsername(), userDetails.getAccountId());
    }

    /**
     * 从Redis中删除token（登出时调用）
     *
     * @param token JWT token
     */
    public void removeTokenFromRedis(String token) {
        String redisKey = TOKEN_PREFIX + token;
        Boolean deleted = stringRedisTemplate.delete(redisKey);
        log.info("Token removed from Redis, deleted: {}", deleted);
    }

    /**
     * 检查token是否存在于Redis中
     *
     * @param token JWT token
     * @return true-存在（有效），false-不存在（已登出或过期）
     */
    public boolean isTokenInRedis(String token) {
        String redisKey = TOKEN_PREFIX + token;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(redisKey));
    }

    /**
     * 从Redis中获取用户信息
     *
     * @param token JWT token
     * @return 用户信息Map，不存在则返回null
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserInfoFromRedis(String token) {
        String redisKey = TOKEN_PREFIX + token;
        String jsonValue = stringRedisTemplate.opsForValue().get(redisKey);
        if (jsonValue != null && !jsonValue.isEmpty()) {
            return JSONUtil.toBean(jsonValue, Map.class);
        }
        return null;
    }

    /**
     * 从 Token 中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * 从 Token 中获取权限表ID
     */
    public Long getAccountIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object accountId = claims.get("accountId");
        return accountId != null ? Long.valueOf(accountId.toString()) : null;
    }

    /**
     * 从 Token 中获取业务表ID
     */
    public Long getRefIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object refId = claims.get("refId");
        return refId != null ? Long.valueOf(refId.toString()) : null;
    }

    /**
     * 从 Token 中获取用户类型
     */
    public String getUserTypeFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userType", String.class);
    }

    /**
     * 从 Token 中获取角色列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return (List<String>) claims.get("roles");
    }

    /**
     * 从 Token 中获取权限列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getPermissionsFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return (List<String>) claims.get("permissions");
    }

    /**
     * 验证 Token（仅验证JWT签名和过期时间，不检查Redis）
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }

    /**
     * 完整验证Token：JWT签名 + Redis存在性检查
     * 用于过滤器中的token验证
     *
     * @param token JWT token
     * @return true-有效，false-无效或已登出
     */
    public boolean validateTokenWithRedis(String token) {
        // 1. 先验证JWT本身的有效性（签名、过期等）
        if (!validateToken(token)) {
            return false;
        }
        // 2. 再检查Redis中是否存在该token（是否已登出）
        boolean existsInRedis = isTokenInRedis(token);
        if (!existsInRedis) {
            log.warn("Token is valid but not found in Redis (user may have logged out)");
        }
        return existsInRedis;
    }

    /**
     * 获取 Claims
     */
    private Claims getClaimsFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}
