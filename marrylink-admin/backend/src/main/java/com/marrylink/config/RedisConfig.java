package com.marrylink.config;

import org.springframework.context.annotation.Configuration;

/**
 * Redis 配置类
 * 使用 Spring Boot 自动配置的 StringRedisTemplate
 * 用户信息以 JSON 字符串形式存储（通过 Hutool JSONUtil 序列化）
 */
@Configuration
public class RedisConfig {
    // StringRedisTemplate 由 Spring Boot 自动配置，无需额外定义 Bean
    // key 和 value 均为 String 类型，value 使用 Hutool JSONUtil 进行 JSON 序列化/反序列化
}