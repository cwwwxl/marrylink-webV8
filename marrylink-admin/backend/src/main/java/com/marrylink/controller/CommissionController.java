package com.marrylink.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.marrylink.common.PageResult;
import com.marrylink.common.Result;
import com.marrylink.entity.CommissionOrder;
import com.marrylink.entity.Host;
import com.marrylink.entity.HostWallet;
import com.marrylink.service.ICommissionOrderService;
import com.marrylink.service.IHostService;
import com.marrylink.service.IHostWalletService;
import com.marrylink.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/commission")
public class CommissionController {

    @Autowired
    private ICommissionOrderService commissionOrderService;
    @Autowired
    private IHostWalletService hostWalletService;
    @Autowired
    private IHostService hostService;

    /**
     * 分页查询佣金订单
     * 管理员查看全部，主持人只能查看自己的
     */
    @GetMapping("/page")
    public Result<PageResult<CommissionOrder>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long hostId,
            @RequestParam(required = false) String orderNo) {

        Page<CommissionOrder> page = new Page<>(current, size);
        LambdaQueryWrapper<CommissionOrder> wrapper = new LambdaQueryWrapper<>();

        if (SecurityUtils.isHost()) {
            Long currentHostId = SecurityUtils.getCurrentRefId();
            wrapper.eq(CommissionOrder::getHostId, currentHostId);
        } else if (hostId != null) {
            wrapper.eq(CommissionOrder::getHostId, hostId);
        }

        if (status != null) {
            wrapper.eq(CommissionOrder::getStatus, status);
        }
        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.like(CommissionOrder::getOrderNo, orderNo);
        }

        wrapper.orderByDesc(CommissionOrder::getCreateTime);
        commissionOrderService.page(page, wrapper);

        return Result.ok(PageResult.of(page));
    }

    /**
     * 获取佣金订单详情
     */
    @GetMapping("/{id}")
    public Result<CommissionOrder> getById(@PathVariable Long id) {
        return Result.ok(commissionOrderService.getById(id));
    }

    /**
     * 主持人支付佣金
     */
    @PostMapping("/pay/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> payCommission(@PathVariable Long id) {
        CommissionOrder commission = commissionOrderService.getById(id);
        if (commission == null) {
            return Result.error("佣金订单不存在");
        }

        // 验证佣金订单属于当前主持人
        Long currentHostId = SecurityUtils.getCurrentRefId();
        if (!commission.getHostId().equals(currentHostId)) {
            return Result.error("无权操作此佣金订单");
        }

        // 验证状态：待支付(1)或逾期(3)
        if (commission.getStatus() != 1 && commission.getStatus() != 3) {
            return Result.error("佣金订单状态异常，无法支付");
        }

        // 获取主持人钱包
        LambdaQueryWrapper<HostWallet> walletWrapper = new LambdaQueryWrapper<>();
        walletWrapper.eq(HostWallet::getHostId, currentHostId);
        HostWallet wallet = hostWalletService.getOne(walletWrapper);
        if (wallet == null) {
            return Result.error("钱包信息不存在");
        }

        // 从冻结金额扣除佣金
        wallet.setFrozenAmount(wallet.getFrozenAmount().subtract(commission.getCommissionAmount()));
        wallet.setTotalCommission(wallet.getTotalCommission().add(commission.getCommissionAmount()));
        hostWalletService.updateById(wallet);

        // 更新佣金订单状态
        CommissionOrder updateCommission = new CommissionOrder();
        updateCommission.setId(id);
        updateCommission.setStatus(2); // 已支付
        updateCommission.setPayTime(LocalDateTime.now());
        commissionOrderService.updateById(updateCommission);

        // 如果主持人被禁止接单，检查是否还有逾期佣金
        Host host = hostService.getById(currentHostId);
        if (host != null && host.getCanAcceptOrder() != null && host.getCanAcceptOrder() == 0) {
            LambdaQueryWrapper<CommissionOrder> overdueWrapper = new LambdaQueryWrapper<>();
            overdueWrapper.eq(CommissionOrder::getHostId, currentHostId);
            overdueWrapper.eq(CommissionOrder::getStatus, 3); // 逾期
            long overdueCount = commissionOrderService.count(overdueWrapper);
            if (overdueCount == 0) {
                Host updateHost = new Host();
                updateHost.setId(currentHostId);
                updateHost.setCanAcceptOrder(1);
                hostService.updateById(updateHost);
            }
        }

        return Result.ok();
    }

    /**
     * 管理员标记佣金为逾期
     */
    @PostMapping("/mark-overdue/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> markOverdue(@PathVariable Long id) {
        CommissionOrder commission = commissionOrderService.getById(id);
        if (commission == null) {
            return Result.error("佣金订单不存在");
        }
        if (commission.getStatus() != 1) {
            return Result.error("只能将待支付的佣金标记为逾期");
        }

        CommissionOrder updateCommission = new CommissionOrder();
        updateCommission.setId(id);
        updateCommission.setStatus(3); // 逾期
        commissionOrderService.updateById(updateCommission);

        return Result.ok();
    }

    /**
     * 管理员禁止主持人接单
     */
    @PostMapping("/ban-host/{hostId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> banHost(@PathVariable Long hostId) {
        Host host = hostService.getById(hostId);
        if (host == null) {
            return Result.error("主持人不存在");
        }

        Host updateHost = new Host();
        updateHost.setId(hostId);
        updateHost.setCanAcceptOrder(0);
        hostService.updateById(updateHost);

        return Result.ok();
    }

    /**
     * 管理员解禁主持人接单（需无逾期佣金）
     */
    @PostMapping("/unban-host/{hostId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> unbanHost(@PathVariable Long hostId) {
        Host host = hostService.getById(hostId);
        if (host == null) {
            return Result.error("主持人不存在");
        }

        // 检查是否还有逾期佣金
        LambdaQueryWrapper<CommissionOrder> overdueWrapper = new LambdaQueryWrapper<>();
        overdueWrapper.eq(CommissionOrder::getHostId, hostId);
        overdueWrapper.eq(CommissionOrder::getStatus, 3); // 逾期
        long overdueCount = commissionOrderService.count(overdueWrapper);
        if (overdueCount > 0) {
            return Result.error("该主持人还有" + overdueCount + "笔逾期佣金，无法解禁");
        }

        Host updateHost = new Host();
        updateHost.setId(hostId);
        updateHost.setCanAcceptOrder(1);
        hostService.updateById(updateHost);

        return Result.ok();
    }
}
