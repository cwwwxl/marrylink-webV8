package com.marrylink.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.marrylink.common.PageResult;
import com.marrylink.common.Result;
import com.marrylink.entity.HostWallet;
import com.marrylink.entity.WithdrawalRequest;
import com.marrylink.service.IHostWalletService;
import com.marrylink.service.IWithdrawalRequestService;
import com.marrylink.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/host-wallet")
public class HostWalletController {

    @Autowired
    private IHostWalletService hostWalletService;
    @Autowired
    private IWithdrawalRequestService withdrawalRequestService;

    /**
     * 主持人获取自己的钱包信息
     */
    @GetMapping("/my")
    public Result<HostWallet> getMyWallet() {
        if (!SecurityUtils.isHost()) {
            return Result.error("仅主持人可访问");
        }
        Long hostId = SecurityUtils.getCurrentRefId();
        LambdaQueryWrapper<HostWallet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HostWallet::getHostId, hostId);
        HostWallet wallet = hostWalletService.getOne(wrapper);
        return Result.ok(wallet);
    }

    /**
     * 管理员获取主持人钱包信息
     */
    @GetMapping("/{hostId}")
    public Result<HostWallet> getByHostId(@PathVariable Long hostId) {
        LambdaQueryWrapper<HostWallet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HostWallet::getHostId, hostId);
        HostWallet wallet = hostWalletService.getOne(wrapper);
        return Result.ok(wallet);
    }

    /**
     * 管理员分页查询所有主持人钱包
     */
    @GetMapping("/page")
    public Result<PageResult<HostWallet>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {

        Page<HostWallet> page = new Page<>(current, size);
        LambdaQueryWrapper<HostWallet> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(HostWallet::getCreateTime);
        hostWalletService.page(page, wrapper);

        return Result.ok(PageResult.of(page));
    }

    /**
     * 主持人提交提现申请
     */
    @PostMapping("/withdraw")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> withdraw(@RequestBody Map<String, Object> params) {
        if (!SecurityUtils.isHost()) {
            return Result.error("仅主持人可提现");
        }

        Long hostId = SecurityUtils.getCurrentRefId();
        BigDecimal amount = new BigDecimal(params.get("amount").toString());
        String accountType = params.get("accountType").toString();
        String accountNo = params.get("accountNo").toString();
        String accountName = params.get("accountName").toString();

        // 获取钱包
        LambdaQueryWrapper<HostWallet> walletWrapper = new LambdaQueryWrapper<>();
        walletWrapper.eq(HostWallet::getHostId, hostId);
        HostWallet wallet = hostWalletService.getOne(walletWrapper);
        if (wallet == null) {
            return Result.error("钱包信息不存在");
        }

        // 验证可用余额
        BigDecimal availableBalance = wallet.getBalance().subtract(wallet.getFrozenAmount());
        if (availableBalance.compareTo(amount) < 0) {
            return Result.error("可用余额不足");
        }

        // 创建提现申请
        String withdrawalNo = "WD" + System.currentTimeMillis() + new Random().nextInt(1000);
        WithdrawalRequest request = new WithdrawalRequest();
        request.setWithdrawalNo(withdrawalNo);
        request.setHostId(hostId);
        request.setHostName(SecurityUtils.getCurrentRealName());
        request.setAmount(amount);
        request.setAccountType(accountType);
        request.setAccountNo(accountNo);
        request.setAccountName(accountName);
        request.setStatus(1); // 待审核
        withdrawalRequestService.save(request);

        // 扣减钱包余额
        wallet.setBalance(wallet.getBalance().subtract(amount));
        hostWalletService.updateById(wallet);

        return Result.ok();
    }

    /**
     * 分页查询提现记录
     * 管理员查看全部，主持人只能查看自己的
     */
    @GetMapping("/withdrawal/page")
    public Result<PageResult<WithdrawalRequest>> withdrawalPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long hostId) {

        Page<WithdrawalRequest> page = new Page<>(current, size);
        LambdaQueryWrapper<WithdrawalRequest> wrapper = new LambdaQueryWrapper<>();

        if (SecurityUtils.isHost()) {
            Long currentHostId = SecurityUtils.getCurrentRefId();
            wrapper.eq(WithdrawalRequest::getHostId, currentHostId);
        } else if (hostId != null) {
            wrapper.eq(WithdrawalRequest::getHostId, hostId);
        }

        if (status != null) {
            wrapper.eq(WithdrawalRequest::getStatus, status);
        }

        wrapper.orderByDesc(WithdrawalRequest::getCreateTime);
        withdrawalRequestService.page(page, wrapper);

        return Result.ok(PageResult.of(page));
    }

    /**
     * 管理员审核提现申请
     * action: approve(通过) / reject(拒绝) / confirm_payment(确认打款)
     */
    @PutMapping("/withdrawal/{id}/audit")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> auditWithdrawal(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        String action = params.get("action").toString();
        WithdrawalRequest request = withdrawalRequestService.getById(id);
        if (request == null) {
            return Result.error("提现申请不存在");
        }

        String operator = SecurityUtils.getCurrentUsername();

        if ("approve".equals(action)) {
            if (request.getStatus() != 1) {
                return Result.error("只能审核待审核的申请");
            }
            WithdrawalRequest update = new WithdrawalRequest();
            update.setId(id);
            update.setStatus(2); // 已通过
            update.setAuditTime(LocalDateTime.now());
            update.setOperator(operator);
            withdrawalRequestService.updateById(update);

        } else if ("reject".equals(action)) {
            if (request.getStatus() != 1) {
                return Result.error("只能审核待审核的申请");
            }
            String rejectReason = params.get("rejectReason") != null ? params.get("rejectReason").toString() : "";
            WithdrawalRequest update = new WithdrawalRequest();
            update.setId(id);
            update.setStatus(3); // 已拒绝
            update.setRejectReason(rejectReason);
            update.setAuditTime(LocalDateTime.now());
            update.setOperator(operator);
            withdrawalRequestService.updateById(update);

            // 退回金额到钱包
            LambdaQueryWrapper<HostWallet> walletWrapper = new LambdaQueryWrapper<>();
            walletWrapper.eq(HostWallet::getHostId, request.getHostId());
            HostWallet wallet = hostWalletService.getOne(walletWrapper);
            if (wallet != null) {
                wallet.setBalance(wallet.getBalance().add(request.getAmount()));
                hostWalletService.updateById(wallet);
            }

        } else if ("confirm_payment".equals(action)) {
            if (request.getStatus() != 2) {
                return Result.error("只能对已通过的申请确认打款");
            }
            WithdrawalRequest update = new WithdrawalRequest();
            update.setId(id);
            update.setStatus(4); // 已打款
            update.setPayTime(LocalDateTime.now());
            update.setOperator(operator);
            withdrawalRequestService.updateById(update);

            // 更新钱包已提现总额
            LambdaQueryWrapper<HostWallet> walletWrapper = new LambdaQueryWrapper<>();
            walletWrapper.eq(HostWallet::getHostId, request.getHostId());
            HostWallet wallet = hostWalletService.getOne(walletWrapper);
            if (wallet != null) {
                wallet.setTotalWithdrawn(wallet.getTotalWithdrawn().add(request.getAmount()));
                hostWalletService.updateById(wallet);
            }

        } else {
            return Result.error("无效的操作类型");
        }

        return Result.ok();
    }
}
