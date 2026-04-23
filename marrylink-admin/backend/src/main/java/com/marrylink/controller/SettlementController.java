package com.marrylink.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.marrylink.common.PageResult;
import com.marrylink.common.Result;
import com.marrylink.entity.*;
import com.marrylink.service.*;
import com.marrylink.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@RestController
@RequestMapping("/settlement")
public class SettlementController {

    @Autowired
    private ISettlementService settlementService;
    @Autowired
    private IOrderService orderService;
    @Autowired
    private IPlatformEscrowService platformEscrowService;
    @Autowired
    private IPlatformSettingsService platformSettingsService;
    @Autowired
    private IHostWalletService hostWalletService;
    @Autowired
    private ICommissionOrderService commissionOrderService;
    @Autowired
    private IOrderLogService orderLogService;
    @Autowired
    private IMessageService messageService;

    /**
     * 管理员结算订单
     * 流程：全额打给主持人 → 发送佣金账单 → 主持人支付佣金
     */
    @PostMapping("/settle/{orderId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> settle(@PathVariable Long orderId) {
        // 1. 验证订单状态
        Order order = orderService.getById(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (order.getStatus() != 4) {
            return Result.error("订单未完成，无法结算");
        }

        // 检查是否有托管记录
        LambdaQueryWrapper<PlatformEscrow> escrowWrapper = new LambdaQueryWrapper<>();
        escrowWrapper.eq(PlatformEscrow::getOrderId, orderId);
        PlatformEscrow escrow = platformEscrowService.getOne(escrowWrapper);
        if (escrow == null) {
            return Result.error("未找到托管记录");
        }
        if (escrow.getStatus() != 1) {
            return Result.error("托管记录状态异常");
        }

        // 检查是否已结算
        LambdaQueryWrapper<Settlement> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(Settlement::getOrderId, orderId);
        Settlement existSettlement = settlementService.getOne(existWrapper);
        if (existSettlement != null) {
            return Result.error("该订单已结算");
        }

        // 2. 获取佣金比例
        LambdaQueryWrapper<PlatformSettings> rateWrapper = new LambdaQueryWrapper<>();
        rateWrapper.eq(PlatformSettings::getSettingKey, "commission_rate");
        PlatformSettings rateSetting = platformSettingsService.getOne(rateWrapper);
        BigDecimal commissionRate = new BigDecimal("10.00"); // 默认10%
        if (rateSetting != null) {
            commissionRate = new BigDecimal(rateSetting.getSettingValue());
        }

        // 3. 使用订单全额计算佣金，全额释放托管金额给主持人
        BigDecimal totalOrderAmount = escrow.getTotalOrderAmount() != null
                ? escrow.getTotalOrderAmount() : order.getAmount();
        BigDecimal escrowAmount = escrow.getAmount(); // 实际托管的定金金额

        BigDecimal commissionAmount = totalOrderAmount.multiply(commissionRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal netAmount = escrowAmount; // 全额释放定金给主持人

        // 4. 创建结算记录
        String settlementNo = "SE" + System.currentTimeMillis() + new Random().nextInt(1000);
        Settlement settlement = new Settlement();
        settlement.setSettlementNo(settlementNo);
        settlement.setOrderId(orderId);
        settlement.setOrderNo(order.getOrderNo());
        settlement.setHostId(order.getHostId());
        settlement.setHostName(order.getHostName());
        settlement.setAmount(totalOrderAmount);
        settlement.setCommissionAmount(commissionAmount);
        settlement.setNetAmount(netAmount);
        settlement.setStatus(2); // 已结算
        settlement.setSettleTime(LocalDateTime.now());
        settlement.setOperator(SecurityUtils.getCurrentUsername());
        settlementService.save(settlement);

        // 5. 更新托管记录状态
        PlatformEscrow updateEscrow = new PlatformEscrow();
        updateEscrow.setId(escrow.getId());
        updateEscrow.setStatus(2); // 已结算
        updateEscrow.setSettleTime(LocalDateTime.now());
        platformEscrowService.updateById(updateEscrow);

        // 6. 创建/更新主持人钱包 - 全额释放托管金额
        LambdaQueryWrapper<HostWallet> walletWrapper = new LambdaQueryWrapper<>();
        walletWrapper.eq(HostWallet::getHostId, order.getHostId());
        HostWallet wallet = hostWalletService.getOne(walletWrapper);
        if (wallet == null) {
            wallet = new HostWallet();
            wallet.setHostId(order.getHostId());
            wallet.setBalance(escrowAmount);
            wallet.setFrozenAmount(commissionAmount);
            wallet.setTotalIncome(totalOrderAmount);
            wallet.setTotalCommission(BigDecimal.ZERO);
            wallet.setTotalWithdrawn(BigDecimal.ZERO);
            hostWalletService.save(wallet);
        } else {
            wallet.setBalance(wallet.getBalance().add(escrowAmount));
            wallet.setFrozenAmount(wallet.getFrozenAmount().add(commissionAmount));
            wallet.setTotalIncome(wallet.getTotalIncome().add(totalOrderAmount));
            hostWalletService.updateById(wallet);
        }

        // 7. 创建佣金订单
        LambdaQueryWrapper<PlatformSettings> deadlineWrapper = new LambdaQueryWrapper<>();
        deadlineWrapper.eq(PlatformSettings::getSettingKey, "commission_deadline_days");
        PlatformSettings deadlineSetting = platformSettingsService.getOne(deadlineWrapper);
        int deadlineDays = 7;
        if (deadlineSetting != null) {
            deadlineDays = Integer.parseInt(deadlineSetting.getSettingValue());
        }

        LocalDateTime deadline = LocalDateTime.now().plusDays(deadlineDays);

        String commissionNo = "CM" + System.currentTimeMillis() + new Random().nextInt(1000);
        CommissionOrder commissionOrder = new CommissionOrder();
        commissionOrder.setCommissionNo(commissionNo);
        commissionOrder.setOrderId(orderId);
        commissionOrder.setOrderNo(order.getOrderNo());
        commissionOrder.setHostId(order.getHostId());
        commissionOrder.setHostName(order.getHostName());
        commissionOrder.setOrderAmount(totalOrderAmount);
        commissionOrder.setCommissionRate(commissionRate);
        commissionOrder.setCommissionAmount(commissionAmount);
        commissionOrder.setStatus(1); // 待支付
        commissionOrder.setDeadline(deadline);
        commissionOrderService.save(commissionOrder);

        // 8. 发送佣金账单通知给主持人
        String deadlineStr = deadline.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        messageService.sendCommissionBillMessage(
                order.getHostId(),
                order.getOrderNo(),
                commissionAmount.toString(),
                deadlineStr
        );

        // 9. 记录日志
        orderLogService.logOrderStatusChange(order.getOrderNo(), order.getStatus(), order.getStatus(),
                SecurityUtils.getCurrentUsername(), "settlement");

        return Result.ok();
    }

    /**
     * 管理员分页查询结算记录
     */
    @GetMapping("/page")
    public Result<PageResult<Settlement>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Long hostId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderNo) {

        Page<Settlement> page = new Page<>(current, size);
        LambdaQueryWrapper<Settlement> wrapper = new LambdaQueryWrapper<>();

        if (hostId != null) {
            wrapper.eq(Settlement::getHostId, hostId);
        }
        if (status != null) {
            wrapper.eq(Settlement::getStatus, status);
        }
        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.like(Settlement::getOrderNo, orderNo);
        }

        wrapper.orderByDesc(Settlement::getCreateTime);
        settlementService.page(page, wrapper);

        return Result.ok(PageResult.of(page));
    }

    /**
     * 获取结算详情
     */
    @GetMapping("/{id}")
    public Result<Settlement> getById(@PathVariable Long id) {
        return Result.ok(settlementService.getById(id));
    }
}
