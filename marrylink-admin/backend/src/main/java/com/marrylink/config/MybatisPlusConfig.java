package com.marrylink.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis Plus配置类
 * 配置分页插件和自动填充功能
 *
 * @author MarryLink Team
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 配置MyBatis Plus拦截器（分页插件）
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    /**
     * 配置自动填充处理器
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            /**
             * 插入时自动填充
             */
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();

                // 自动填充创建时间
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);

                // 自动填充更新时间
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);

                // 自动填充删除标记（默认为0-未删除）
                this.strictInsertFill(metaObject, "isDeleted", Integer.class, 0);
            }

            /**
             * 更新时自动填充
             */
            @Override
            public void updateFill(MetaObject metaObject) {
                // 自动填充更新时间
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
