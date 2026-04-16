package com.marrylink.service;

import com.marrylink.entity.Order;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author author
 * @since 2026-01-04
 */
public interface IOrderService extends IService<Order> {

    /**
     * 新人对已完成订单进行评分和评价，并重新计算主持人平均评分
     * @param orderId 订单ID
     * @param rating 评分 1-5
     * @param comment 评价内容（可选）
     * @param userId 当前用户ID（新人refId）
     */
    void rateOrder(Long orderId, Integer rating, String comment, Long userId);
}
