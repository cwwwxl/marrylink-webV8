package com.marrylink.dto;

import lombok.Data;

/**
 * 用户信息响应DTO
 */
@Data
public class UserInfoResponse {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 新娘姓名
     */
    private String brideName;

    /**
     * 新郎姓名
     */
    private String groomName;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 婚礼类型
     */
    private String weddingType;

    /**
     * 状态
     */
    private Integer status;
}
