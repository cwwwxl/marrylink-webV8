package com.marrylink.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.marrylink.common.PageResult;
import com.marrylink.common.Result;
import com.marrylink.entity.PlatformAccount;
import com.marrylink.entity.PlatformWithdrawal;
import com.marrylink.service.IPlatformAccountService;
import com.marrylink.service.IPlatformWithdrawalService;
import com.marrylink.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/platform-finance")
public class PlatformFinanceController {

    @Autowired
    private IPlatformAccountService platformAccountService;
    @Autowired
    private IPlatformWithdrawalService platformWithdrawalService;

    /**
     * 获取平台账户信息
     */
    @GetMapping("/account")
    public Result<PlatformAccount> getAccount() {
        PlatformAccount account = platformAccountService.getById(1L);
        if (account == null) {
            // 自动初始化
            account = new PlatformAccount();
            account.setId(1L);
            account.setBalance(BigDecimal.ZERO);
            account.setTotalCommissionIncome(BigDecimal.ZERO);
            account.setTotalWithdrawn(BigDecimal.ZERO);
            platformAccountService.save(account);
        }
        return Result.ok(account);
    }

    /**
     * 平台提现申请
     */
    @PostMapping("/withdraw")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> withdraw(@RequestBody Map<String, Object> params) {
        Object amountObj = params.get("amount");
        if (amountObj == null) {
            return Result.error("请输入提现金额");
        }
        BigDecimal amount = new BigDecimal(amountObj.toString());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error("提现金额必须大于0");
        }

        String accountType = params.get("accountType") != null ? params.get("accountType").toString() : "";
        String accountNo = params.get("accountNo") != null ? params.get("accountNo").toString() : "";
        String accountName = params.get("accountName") != null ? params.get("accountName").toString() : "";
        String remark = params.get("remark") != null ? params.get("remark").toString() : "";

        if (accountType.isEmpty() || accountNo.isEmpty() || accountName.isEmpty()) {
            return Result.error("请填写完整的账户信息");
        }

        // 检查余额
        PlatformAccount account = platformAccountService.getById(1L);
        if (account == null || account.getBalance().compareTo(amount) < 0) {
            return Result.error("平台余额不足");
        }

        // 扣减余额
        account.setBalance(account.getBalance().subtract(amount));
        platformAccountService.updateById(account);

        // 创建提现记录
        PlatformWithdrawal withdrawal = new PlatformWithdrawal();
        withdrawal.setWithdrawalNo("PW" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
        withdrawal.setAmount(amount);
        withdrawal.setAccountType(accountType);
        withdrawal.setAccountNo(accountNo);
        withdrawal.setAccountName(accountName);
        withdrawal.setRemark(remark);
        withdrawal.setStatus(1); // 待处理
        withdrawal.setOperator(SecurityUtils.getCurrentUsername());
        platformWithdrawalService.save(withdrawal);

        return Result.ok();
    }

    /**
     * 确认提现完成
     */
    @PutMapping("/withdrawal/{id}/complete")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> completeWithdrawal(@PathVariable Long id) {
        PlatformWithdrawal withdrawal = platformWithdrawalService.getById(id);
        if (withdrawal == null) {
            return Result.error("提现记录不存在");
        }
        if (withdrawal.getStatus() != 1) {
            return Result.error("只能完成待处理的提现");
        }

        // 更新提现状态
        PlatformWithdrawal update = new PlatformWithdrawal();
        update.setId(id);
        update.setStatus(2); // 已完成
        update.setCompleteTime(LocalDateTime.now());
        update.setOperator(SecurityUtils.getCurrentUsername());
        platformWithdrawalService.updateById(update);

        // 更新累计提现
        PlatformAccount account = platformAccountService.getById(1L);
        if (account != null) {
            account.setTotalWithdrawn(account.getTotalWithdrawn().add(withdrawal.getAmount()));
            platformAccountService.updateById(account);
        }

        return Result.ok();
    }

    /**
     * 取消提现（退回余额）
     */
    @PutMapping("/withdrawal/{id}/cancel")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> cancelWithdrawal(@PathVariable Long id) {
        PlatformWithdrawal withdrawal = platformWithdrawalService.getById(id);
        if (withdrawal == null) {
            return Result.error("提现记录不存在");
        }
        if (withdrawal.getStatus() != 1) {
            return Result.error("只能取消待处理的提现");
        }

        // 退回余额
        PlatformAccount account = platformAccountService.getById(1L);
        if (account != null) {
            account.setBalance(account.getBalance().add(withdrawal.getAmount()));
            platformAccountService.updateById(account);
        }

        // 删除提现记录
        platformWithdrawalService.removeById(id);

        return Result.ok();
    }

    /**
     * 平台提现记录列表
     */
    @GetMapping("/withdrawal/page")
    public Result<PageResult<PlatformWithdrawal>> withdrawalPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status) {

        Page<PlatformWithdrawal> page = new Page<>(current, size);
        LambdaQueryWrapper<PlatformWithdrawal> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(PlatformWithdrawal::getStatus, status);
        }
        wrapper.orderByDesc(PlatformWithdrawal::getCreateTime);
        platformWithdrawalService.page(page, wrapper);

        return Result.ok(PageResult.of(page));
    }
}
