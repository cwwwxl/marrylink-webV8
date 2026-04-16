package com.marrylink.dto;

import lombok.Data;

/**
 * 即将到来的婚礼DTO
 */
@Data
public class UpcomingWeddingDTO {

    /**
     * 订单ID
     */
    private Long id;

    /**
     * 新人姓名
     */
    private String coupleName;

    /**
     * 婚礼日期（格式：yyyy-MM-dd）
     */
    private String weddingDate;

    /**
     * 婚礼类型
     */
    private String weddingType;

    /**
     * 婚礼场地
     */
    private String venue;

    /**
     * 婚礼时间
     */
    private String weddingTime;

    /**
     * 订单状态
     * pending - 待确认
     * communicating - 沟通中
     * confirmed - 已确认
     * completed - 已完成
     */
    private String status;
}
