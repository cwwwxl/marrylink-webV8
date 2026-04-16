package com.marrylink.dto;

import lombok.Data;

import java.util.List;

/**
 * 路由VO
 */
@Data
public class RouteVO {
    
    /** 路由路径 */
    private String path;
    
    /** 组件路径 */
    private String component;
    
    /** 路由名称 */
    private String name;
    
    /** 重定向路径 */
    private String redirect;
    
    /** 路由元信息 */
    private Meta meta;
    
    /** 子路由 */
    private List<RouteVO> children;
    
    /**
     * 路由元信息
     */
    @Data
    public static class Meta {
        /** 路由标题 */
        private String title;
        
        /** 图标 */
        private String icon;
        
        /** 是否隐藏 */
        private Boolean hidden;
        
        /** 是否始终显示 */
        private Boolean alwaysShow;
        
        /** 是否缓存 */
        private Boolean keepAlive;
    }
}