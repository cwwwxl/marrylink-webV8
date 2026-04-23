package com.marrylink.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.marrylink.entity.CommissionOrder;
import com.marrylink.entity.Host;
import com.marrylink.entity.Message;
import com.marrylink.service.ICommissionOrderService;
import com.marrylink.service.IHostService;
import com.marrylink.service.IMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 佣金定时任务
 * 1. 每小时检查逾期未支付的佣金订单，自动标记为逾期
 * 2. 逾期后自动限制主持人接单
 */
@Component
public class CommissionScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(CommissionScheduledTask.class);

    @Autowired
    private ICommissionOrderService commissionOrderService;
    @Autowired
    private IHostService hostService;
    @Autowired
    private IMessageService messageService;

    /**
     * 每小时执行一次：检查逾期佣金并自动限制接单
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void checkOverdueCommissions() {
        log.info("开始检查逾期佣金订单...");

        // 查找所有已过截止日期且状态仍为"待支付"的佣金订单
        LambdaQueryWrapper<CommissionOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommissionOrder::getStatus, 1); // 待支付
        wrapper.lt(CommissionOrder::getDeadline, LocalDateTime.now()); // 已过截止时间
        List<CommissionOrder> overdueList = commissionOrderService.list(wrapper);

        if (overdueList.isEmpty()) {
            log.info("没有逾期佣金订单");
            return;
        }

        log.info("发现 {} 笔逾期佣金订单", overdueList.size());

        // 标记为逾期
        for (CommissionOrder commission : overdueList) {
            CommissionOrder update = new CommissionOrder();
            update.setId(commission.getId());
            update.setStatus(3); // 逾期
            commissionOrderService.updateById(update);
        }

        // 获取涉及的主持人ID列表（去重）
        List<Long> hostIds = overdueList.stream()
                .map(CommissionOrder::getHostId)
                .distinct()
                .collect(Collectors.toList());

        // 限制这些主持人接单
        for (Long hostId : hostIds) {
            Host host = hostService.getById(hostId);
            if (host != null && (host.getCanAcceptOrder() == null || host.getCanAcceptOrder() == 1)) {
                Host updateHost = new Host();
                updateHost.setId(hostId);
                updateHost.setCanAcceptOrder(0); // 禁止接单
                hostService.updateById(updateHost);

                // 发送通知给主持人
                Message message = new Message();
                message.setHostId(hostId);
                message.setContent("您有佣金逾期未支付，已被限制接单。请尽快支付佣金以恢复接单资格。");
                message.setStatus(1);
                messageService.save(message);

                log.info("主持人 {} 因佣金逾期已被限制接单", hostId);
            }
        }

        log.info("逾期佣金检查完成");
    }
}
