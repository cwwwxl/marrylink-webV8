package com.marrylink.dto;

import lombok.Data;

/**
 * 档期项DTO
 */
@Data
public class ScheduleItemDTO {

    /**
     * 日期（格式：yyyy-MM-dd）
     */
    private String date;

    /**
     * 档期状态
     * available - 可用
     * locked - 已锁定（沟通中）
     * occupied - 已占用（已确认）
     */
    private Integer status;
}
