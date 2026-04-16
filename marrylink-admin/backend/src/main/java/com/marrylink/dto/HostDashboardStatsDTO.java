package com.marrylink.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 主持人工作台统计数据DTO
 */
@Data
public class HostDashboardStatsDTO {
    
    /**
     * 本月订单数
     */
    private Integer monthOrders;
    
    /**
     * 订单环比趋势（与上月对比）
     */
    private Integer orderTrend;
    
    /**
     * 待处理问卷数
     */
    private Integer pendingQuestionnaires;
    
    /**
     * 本月收入
     */
    private BigDecimal monthIncome;
    
    /**
     * 收入环比趋势（百分比）
     */
    private Integer incomeTrend;
    
    /**
     * 收入目标完成率（百分比）
     */
    private Integer incomeRate;
    
    /**
     * 满意度评分
     */
    private Double rating;
    
    /**
     * 评价数量
     */
    private Integer ratingCount;
}