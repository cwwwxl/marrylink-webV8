package com.marrylink.controller;

import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.marrylink.common.PageResult;
import com.marrylink.common.Result;
import com.marrylink.entity.*;
import com.marrylink.service.*;
import com.marrylink.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private IOrderService orderService;
    @Autowired
    private IOrderLogService orderLogService;
    @Resource
    private IHostService hostService;
    @Resource
    private IUserService userService;
    @Resource
    private IQuestionnaireSubmissionService qsService;
    @Resource
    private IMessageService messageService;
    @Autowired
    private IPlatformEscrowService platformEscrowService;
    @Autowired
    private ISettlementService settlementService;
    @Autowired
    private IPlatformSettingsService platformSettingsService;
    @Autowired
    private IHostWalletService hostWalletService;
    @Autowired
    private ICommissionOrderService commissionOrderService;

    @GetMapping("/page")
    public Result<PageResult<Order>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate weddingDate,
            @RequestParam(required = false) Long hostId,
            @RequestParam(required = false) String keyword) {

        Page<Order> page = new Page<>(current, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();

        // 如果当前用户是主持人，只能查看自己的订单
        if (SecurityUtils.isHost()) {
            Long currentHostId = SecurityUtils.getCurrentRefId();
            wrapper.eq(Order::getHostId, currentHostId);
        } else if (SecurityUtils.isCustomer()) {
            Long currentHostId = SecurityUtils.getCurrentRefId();
            wrapper.eq(Order::getUserId, currentHostId);
        }else if (hostId != null) {
            // 非主持人角色可以按hostId筛选
            wrapper.eq(Order::getHostId, hostId);
        }

        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        if (weddingDate != null) {
            wrapper.eq(Order::getWeddingDate, weddingDate);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Order::getOrderNo, keyword);
        }

        wrapper.orderByDesc(Order::getWeddingDate);
        orderService.page(page, wrapper);

        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/{id}")
    public Result<Order> getById(@PathVariable Long id) {
        return Result.ok(orderService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Order order) {
        order.setOrderNo(UUID.fastUUID().toString());
        orderService.save(order);
        return Result.ok();
    }

    @PostMapping("/create")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> create(@RequestBody Map<String, Object> params) {
        Long hostId = Long.valueOf(params.get("hostId").toString());
        Long userId = SecurityUtils.getCurrentRefId();

        Order order = new Order();
        order.setOrderNo(UUID.fastUUID().toString());
        Host host = hostService.getById(hostId);
        order.setAmount(host.getPrice());
        order.setHostId(hostId);
        order.setHostName(host.getName());
        order.setUserId(userId);
        User user = userService.getById(userId);
        order.setUserName(user.getBrideName() + "&" + user.getGroomName());
        order.setStatus(1);
        order.setWeddingType(params.get("weddingType").toString());

        String dateStr = params.get("weddingDate").toString();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE; // 或自定义格式
        LocalDate weddingDate = LocalDate.parse(dateStr, formatter);
        order.setWeddingDate(weddingDate);
        orderService.save(order);

        messageService.sendOrderCreatedMessage(userId,hostId, order.getUserName(), weddingDate.toString());

        return Result.ok();
    }

    @PutMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(@RequestBody Order order, HttpServletRequest request) {
        Order oldOrder = orderService.getById(order.getId());
        orderService.updateById(order);

        String operator = SecurityUtils.getCurrentUsername();
        String ip = getClientIp(request);
        orderLogService.logOrderStatusChange(order.getOrderNo(), oldOrder.getStatus(), order.getStatus(), operator, ip);

        if (3 == order.getStatus()) {
            qsService.createQS(order);
            autoCreateEscrow(oldOrder); // 支付 → 创建托管记录
        } else if (5 == order.getStatus()){
            qsService.deleteQSByNo(order);
        } else if (4 == order.getStatus()) {
            autoSettleOrder(oldOrder); // 完成 → 自动结算+佣金
        }
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status, HttpServletRequest request) {
        Order oldOrder = orderService.getById(id);
        if (oldOrder == null) {
            return Result.error("订单不存在");
        }

        Order updateOrder = new Order();
        updateOrder.setId(id);
        updateOrder.setStatus(status);
        orderService.updateById(updateOrder);

        // App端仅传状态，后续流程依赖完整订单数据，需使用旧订单补全状态
        Order orderForQs = new Order();
        orderForQs.setId(oldOrder.getId());
        orderForQs.setOrderNo(oldOrder.getOrderNo());
        orderForQs.setUserId(oldOrder.getUserId());
        orderForQs.setUserName(oldOrder.getUserName());
        orderForQs.setHostId(oldOrder.getHostId());
        orderForQs.setHostName(oldOrder.getHostName());
        orderForQs.setWeddingDate(oldOrder.getWeddingDate());
        orderForQs.setWeddingType(oldOrder.getWeddingType());
        orderForQs.setAmount(oldOrder.getAmount());
        orderForQs.setStatus(status);

        String operator = SecurityUtils.getCurrentUsername();
        String ip = getClientIp(request);
        orderLogService.logOrderStatusChange(oldOrder.getOrderNo(), oldOrder.getStatus(), status, operator, ip);

        if (3 == status) {
            qsService.createQS(orderForQs);
            autoCreateEscrow(oldOrder); // 支付 → 创建托管记录
        } else if (5 == status) {
            qsService.deleteQSByNo(orderForQs);
        } else if (4 == status) {
            autoSettleOrder(oldOrder); // 完成 → 自动结算+佣金
        }
        return Result.ok();
    }

    @GetMapping("/log/page")
    public Result<PageResult<OrderLog>> getLogPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String orderNo) {

        Page<OrderLog> page = new Page<>(current, size);
        LambdaQueryWrapper<OrderLog> wrapper = new LambdaQueryWrapper<>();

        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.like(OrderLog::getOrderNo, orderNo);
        }

        wrapper.orderByDesc(OrderLog::getCreateTime);
        orderLogService.page(page, wrapper);

        return Result.ok(PageResult.of(page));
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

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        orderService.removeById(id);
        return Result.ok();
    }

    /**
     * 新人对已完成订单进行评分和评价
     * @param id 订单ID
     * @param params 包含 rating (1-5) 和 comment (评价内容，可选)
     */
    @PostMapping("/{id}/rate")
    public Result<Void> rateOrder(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Integer rating = Integer.valueOf(params.get("rating").toString());
        String comment = params.get("comment") != null ? params.get("comment").toString() : null;
        Long userId = SecurityUtils.getCurrentRefId();
        orderService.rateOrder(id, rating, comment, userId);
        return Result.ok();
    }

    /**
     * 获取主持人档期（按月份查询）
     * @param year 年份
     * @param month 月份
     * @return 该月份的所有订单
     */
    @GetMapping("/mySchedule")
    public Result<java.util.List<Order>> getMySchedule(
            @RequestParam Integer year,
            @RequestParam Integer month) {

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        Long currentRefId = SecurityUtils.getCurrentRefId();
        wrapper.eq(Order::getHostId, currentRefId);

        // 构建月份的开始和结束日期
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        wrapper.between(Order::getWeddingDate, startDate, endDate);
        wrapper.orderByAsc(Order::getWeddingDate);

        return Result.ok(orderService.list(wrapper));
    }

    /**
     * 获取主持人档期（按月份查询）
     * @param hostId 主持人ID
     * @param year 年份
     * @param month 月份
     * @return 该月份的所有订单
     */
    @GetMapping("/schedule")
    public Result<java.util.List<Order>> getSchedule(
            @RequestParam Long hostId,
            @RequestParam Integer year,
            @RequestParam Integer month) {

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getHostId, hostId);

        // 构建月份的开始和结束日期
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        wrapper.between(Order::getWeddingDate, startDate, endDate);
        wrapper.orderByAsc(Order::getWeddingDate);

        return Result.ok(orderService.list(wrapper));
    }

    /**
     * 订单支付时自动创建平台托管记录
     * 在订单状态变为3（定金已付）时调用
     */
    private void autoCreateEscrow(Order order) {
        // 检查是否已有托管记录
        LambdaQueryWrapper<PlatformEscrow> escrowWrapper = new LambdaQueryWrapper<>();
        escrowWrapper.eq(PlatformEscrow::getOrderId, order.getId());
        PlatformEscrow existing = platformEscrowService.getOne(escrowWrapper);
        if (existing != null) {
            return; // 已存在，跳过
        }

        // 创建平台托管记录
        PlatformEscrow escrow = new PlatformEscrow();
        escrow.setOrderId(order.getId());
        escrow.setOrderNo(order.getOrderNo());
        escrow.setAmount(order.getAmount());
        escrow.setStatus(1); // 托管中
        escrow.setPayTime(LocalDateTime.now());
        platformEscrowService.save(escrow);

        // 更新订单支付状态
        Order updateOrder = new Order();
        updateOrder.setId(order.getId());
        updateOrder.setPaymentStatus(1); // 已支付
        updateOrder.setPayTime(LocalDateTime.now());
        orderService.updateById(updateOrder);
    }

    /**
     * 订单完成时自动结算并生成佣金订单
     * 在订单状态变为4（已完成）时调用
     */
    private void autoSettleOrder(Order order) {
        // 检查是否已结算
        LambdaQueryWrapper<Settlement> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(Settlement::getOrderId, order.getId());
        Settlement existSettlement = settlementService.getOne(existWrapper);
        if (existSettlement != null) {
            return; // 已结算，跳过
        }

        // 获取托管记录
        LambdaQueryWrapper<PlatformEscrow> escrowWrapper = new LambdaQueryWrapper<>();
        escrowWrapper.eq(PlatformEscrow::getOrderId, order.getId());
        PlatformEscrow escrow = platformEscrowService.getOne(escrowWrapper);
        if (escrow == null || escrow.getStatus() != 1) {
            return; // 无托管记录或状态异常，跳过
        }

        // 获取佣金比例
        LambdaQueryWrapper<PlatformSettings> rateWrapper = new LambdaQueryWrapper<>();
        rateWrapper.eq(PlatformSettings::getSettingKey, "commission_rate");
        PlatformSettings rateSetting = platformSettingsService.getOne(rateWrapper);
        BigDecimal commissionRate = new BigDecimal("10.00"); // 默认10%
        if (rateSetting != null) {
            commissionRate = new BigDecimal(rateSetting.getSettingValue());
        }

        // 计算佣金
        BigDecimal orderAmount = escrow.getAmount();
        BigDecimal commissionAmount = orderAmount.multiply(commissionRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal netAmount = orderAmount.subtract(commissionAmount);

        // 创建结算记录
        String settlementNo = "SE" + System.currentTimeMillis() + new Random().nextInt(1000);
        Settlement settlement = new Settlement();
        settlement.setSettlementNo(settlementNo);
        settlement.setOrderId(order.getId());
        settlement.setOrderNo(order.getOrderNo());
        settlement.setHostId(order.getHostId());
        settlement.setHostName(order.getHostName());
        settlement.setAmount(orderAmount);
        settlement.setCommissionAmount(commissionAmount);
        settlement.setNetAmount(netAmount);
        settlement.setStatus(2); // 已结算
        settlement.setSettleTime(LocalDateTime.now());
        settlement.setOperator("system-auto");
        settlementService.save(settlement);

        // 更新托管记录状态
        PlatformEscrow updateEscrow = new PlatformEscrow();
        updateEscrow.setId(escrow.getId());
        updateEscrow.setStatus(2); // 已结算
        updateEscrow.setSettleTime(LocalDateTime.now());
        platformEscrowService.updateById(updateEscrow);

        // 创建/更新主持人钱包
        LambdaQueryWrapper<HostWallet> walletWrapper = new LambdaQueryWrapper<>();
        walletWrapper.eq(HostWallet::getHostId, order.getHostId());
        HostWallet wallet = hostWalletService.getOne(walletWrapper);
        if (wallet == null) {
            wallet = new HostWallet();
            wallet.setHostId(order.getHostId());
            wallet.setBalance(netAmount);
            wallet.setFrozenAmount(commissionAmount);
            wallet.setTotalIncome(orderAmount);
            wallet.setTotalCommission(BigDecimal.ZERO);
            wallet.setTotalWithdrawn(BigDecimal.ZERO);
            hostWalletService.save(wallet);
        } else {
            wallet.setBalance(wallet.getBalance().add(netAmount));
            wallet.setFrozenAmount(wallet.getFrozenAmount().add(commissionAmount));
            wallet.setTotalIncome(wallet.getTotalIncome().add(orderAmount));
            hostWalletService.updateById(wallet);
        }

        // 获取佣金支付截止天数
        LambdaQueryWrapper<PlatformSettings> deadlineWrapper = new LambdaQueryWrapper<>();
        deadlineWrapper.eq(PlatformSettings::getSettingKey, "commission_deadline_days");
        PlatformSettings deadlineSetting = platformSettingsService.getOne(deadlineWrapper);
        int deadlineDays = 7;
        if (deadlineSetting != null) {
            deadlineDays = Integer.parseInt(deadlineSetting.getSettingValue());
        }

        // 创建佣金订单
        String commissionNo = "CM" + System.currentTimeMillis() + new Random().nextInt(1000);
        CommissionOrder commissionOrder = new CommissionOrder();
        commissionOrder.setCommissionNo(commissionNo);
        commissionOrder.setOrderId(order.getId());
        commissionOrder.setOrderNo(order.getOrderNo());
        commissionOrder.setHostId(order.getHostId());
        commissionOrder.setHostName(order.getHostName());
        commissionOrder.setOrderAmount(orderAmount);
        commissionOrder.setCommissionRate(commissionRate);
        commissionOrder.setCommissionAmount(commissionAmount);
        commissionOrder.setStatus(1); // 待支付
        commissionOrder.setDeadline(LocalDateTime.now().plusDays(deadlineDays));
        commissionOrderService.save(commissionOrder);
    }
}
