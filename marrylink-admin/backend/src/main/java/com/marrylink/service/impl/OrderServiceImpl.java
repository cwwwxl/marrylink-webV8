package com.marrylink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.marrylink.entity.Host;
import com.marrylink.entity.Order;
import com.marrylink.exception.BusinessException;
import com.marrylink.mapper.OrderMapper;
import com.marrylink.service.IHostService;
import com.marrylink.service.IOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author author
 * @since 2026-01-04
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    @Resource
    private IHostService hostService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rateOrder(Long orderId, Integer rating, String comment, Long userId) {
        // 校验评分范围
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException("评分必须在1-5之间");
        }

        // 查询订单
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 校验是否是该用户的订单
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权评价此订单");
        }

        // 校验订单状态必须是已完成
        if (order.getStatus() != 4) {
            throw new BusinessException("只能对已完成的订单进行评价");
        }

        // 校验是否已评分
        if (order.getRating() != null) {
            throw new BusinessException("该订单已评价，不可重复评价");
        }

        // 更新订单评分和评价
        Order updateOrder = new Order();
        updateOrder.setId(orderId);
        updateOrder.setRating(rating);
        updateOrder.setComment(comment);
        updateById(updateOrder);

        // 重新计算主持人平均评分
        recalculateHostRating(order.getHostId());
    }

    /**
     * 重新计算主持人的平均评分
     * 基于该主持人所有已评分的已完成订单
     */
    private void recalculateHostRating(Long hostId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getHostId, hostId)
               .eq(Order::getStatus, 4)
               .isNotNull(Order::getRating);

        List<Order> ratedOrders = list(wrapper);

        if (ratedOrders.isEmpty()) {
            return;
        }

        // 计算平均分，保留1位小数
        double avg = ratedOrders.stream()
                .mapToInt(Order::getRating)
                .average()
                .orElse(0.0);

        BigDecimal avgRating = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);

        Host host = new Host();
        host.setId(hostId);
        host.setRating(avgRating);
        hostService.updateById(host);
    }
}
