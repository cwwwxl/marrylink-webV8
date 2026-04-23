package com.marrylink.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.marrylink.common.PageResult;
import com.marrylink.common.Result;
import com.marrylink.entity.Order;
import com.marrylink.entity.OrderLog;
import com.marrylink.entity.PlatformEscrow;
import com.marrylink.service.IOrderLogService;
import com.marrylink.service.IOrderService;
import com.marrylink.service.IPlatformEscrowService;
import com.marrylink.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private IPlatformEscrowService platformEscrowService;
    @Autowired
    private IOrderService orderService;
    @Autowired
    private IOrderLogService orderLogService;

    /**
     * 用户支付订单（模拟支付）
     * 创建平台托管记录，更新订单支付状态和订单状态
     */
    @PostMapping("/pay/{orderId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> pay(@PathVariable Long orderId, HttpServletRequest request) {
        Order order = orderService.getById(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }

        // 检查是否已存在托管记录
        LambdaQueryWrapper<PlatformEscrow> escrowWrapper = new LambdaQueryWrapper<>();
        escrowWrapper.eq(PlatformEscrow::getOrderId, orderId);
        PlatformEscrow existing = platformEscrowService.getOne(escrowWrapper);
        if (existing != null) {
            return Result.error("该订单已支付");
        }

        // 创建平台托管记录
        PlatformEscrow escrow = new PlatformEscrow();
        escrow.setOrderId(orderId);
        escrow.setOrderNo(order.getOrderNo());
        escrow.setAmount(order.getAmount());
        escrow.setStatus(1); // 托管中
        escrow.setPayTime(LocalDateTime.now());
        platformEscrowService.save(escrow);

        // 更新订单状态
        Integer oldStatus = order.getStatus();
        Order updateOrder = new Order();
        updateOrder.setId(orderId);
        updateOrder.setPaymentStatus(1);
        updateOrder.setStatus(3); // 定金已付
        orderService.updateById(updateOrder);

        // 记录日志
        String operator = SecurityUtils.getCurrentUsername();
        String ip = getClientIp(request);
        orderLogService.logOrderStatusChange(order.getOrderNo(), oldStatus, 3, operator, ip);

        return Result.ok();
    }

    /**
     * 管理员分页查询托管记录
     */
    @GetMapping("/escrow/page")
    public Result<PageResult<PlatformEscrow>> escrowPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderNo) {

        Page<PlatformEscrow> page = new Page<>(current, size);
        LambdaQueryWrapper<PlatformEscrow> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(PlatformEscrow::getStatus, status);
        }
        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.like(PlatformEscrow::getOrderNo, orderNo);
        }

        wrapper.orderByDesc(PlatformEscrow::getCreateTime);
        platformEscrowService.page(page, wrapper);

        return Result.ok(PageResult.of(page));
    }

    /**
     * 根据订单ID获取托管信息
     */
    @GetMapping("/escrow/{orderId}")
    public Result<PlatformEscrow> getEscrowByOrderId(@PathVariable Long orderId) {
        LambdaQueryWrapper<PlatformEscrow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlatformEscrow::getOrderId, orderId);
        PlatformEscrow escrow = platformEscrowService.getOne(wrapper);
        return Result.ok(escrow);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
