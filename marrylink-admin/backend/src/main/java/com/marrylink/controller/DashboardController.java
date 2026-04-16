package com.marrylink.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.marrylink.common.PageResult;
import com.marrylink.common.Result;
import com.marrylink.dto.HostDashboardStatsDTO;
import com.marrylink.dto.ScheduleItemDTO;
import com.marrylink.dto.UpcomingWeddingDTO;
import com.marrylink.entity.Host;
import com.marrylink.entity.Order;
import com.marrylink.entity.QuestionnaireSubmission;
import com.marrylink.entity.User;
import com.marrylink.service.IHostService;
import com.marrylink.service.IOrderService;
import com.marrylink.service.IQuestionnaireSubmissionService;
import com.marrylink.service.IUserService;
import com.marrylink.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工作台控制器
 * 提供管理员和主持人的工作台数据接口
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private IHostService hostService;

    @Autowired
    private IUserService userService;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private IQuestionnaireSubmissionService questionnaireSubmissionService;

    /**
     * 获取管理员工作台统计数据
     * @return 统计数据
     */
    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getAdminStats() {
        Map<String, Object> stats = new HashMap<>();

        // 总注册主持人
        long hostCount = hostService.count(new LambdaQueryWrapper<Host>().eq(Host::getStatus, 1));

        // 总注册用户
        long userCount = userService.count();

        // 本月订单量
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());
        long monthOrderCount = orderService.count(
            new LambdaQueryWrapper<Order>().between(Order::getWeddingDate, monthStart, monthEnd)
        );

        // 平台交易额
        BigDecimal totalAmount = orderService.list().stream()
                .map(Order::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.put("hostCount", hostCount);
        stats.put("userCount", userCount);
        stats.put("orderCount", monthOrderCount);
        stats.put("totalAmount", totalAmount);

        return Result.ok(stats);
    }

    /**
     * 获取订单趋势数据（按月度）
     * @param year 年份
     * @return 月度订单趋势
     */
    @GetMapping("/order-trend/monthly")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Map<String, Object>>> getMonthlyOrderTrend(@RequestParam Integer year) {
        List<Map<String, Object>> trendData = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            YearMonth yearMonth = YearMonth.of(year, month);
            LocalDate start = yearMonth.atDay(1);
            LocalDate end = yearMonth.atEndOfMonth();

            long count = orderService.count(
                new LambdaQueryWrapper<Order>().between(Order::getWeddingDate, start, end)
            );

            Map<String, Object> data = new HashMap<>();
            data.put("month", month + "月");
            data.put("count", count);
            trendData.add(data);
        }

        return Result.ok(trendData);
    }

    /**
     * 获取订单趋势数据（按年度）
     * @return 年度订单趋势
     */
    @GetMapping("/order-trend/yearly")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Map<String, Object>>> getYearlyOrderTrend() {
        List<Map<String, Object>> trendData = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();

        for (int year = currentYear - 4; year <= currentYear; year++) {
            LocalDate start = LocalDate.of(year, 1, 1);
            LocalDate end = LocalDate.of(year, 12, 31);

            long count = orderService.count(
                new LambdaQueryWrapper<Order>().between(Order::getWeddingDate, start, end)
            );

            Map<String, Object> data = new HashMap<>();
            data.put("year", year + "年");
            data.put("count", count);
            trendData.add(data);
        }

        return Result.ok(trendData);
    }

    /**
     * 获取订单状态分布
     * @return 订单状态分布数据
     */
    @GetMapping("/order-status-distribution")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Map<String, Object>>> getOrderStatusDistribution() {
        List<Map<String, Object>> distribution = new ArrayList<>();

        // 状态映射: 1-待确认, 3-定金已付, 4-已完成, 5-已取消
        Map<Integer, String> statusMap = new HashMap<>();
        statusMap.put(1, "待确认");
        statusMap.put(3, "定金已付");
        statusMap.put(4, "已完成");
        statusMap.put(5, "已取消");

        for (Map.Entry<Integer, String> entry : statusMap.entrySet()) {
            long count = orderService.count(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, entry.getKey())
            );

            Map<String, Object> data = new HashMap<>();
            data.put("status", entry.getValue());
            data.put("count", count);
            distribution.add(data);
        }

        return Result.ok(distribution);
    }

    /**
     * 获取最近订单列表
     * @return 最近5条订单
     */
    @GetMapping("/recent-orders")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Order>> getRecentOrders() {
        Page<Order> page = new Page<>(1, 5);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Order::getCreateTime);

        orderService.page(page, wrapper);

        return Result.ok(page.getRecords());
    }

    /**
     * 获取主持人工作台统计数据
     * @return 主持人统计数据
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('HOST')")
    public Result<HostDashboardStatsDTO> getHostDashboardStats() {
        Long hostId = SecurityUtils.getCurrentRefId();
        if (hostId == null) {
            return Result.error("未找到主持人信息");
        }

        HostDashboardStatsDTO stats = new HostDashboardStatsDTO();

        // 获取本月日期范围
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());

        // 获取上月日期范围
        LocalDate lastMonthStart = monthStart.minusMonths(1);
        LocalDate lastMonthEnd = lastMonthStart.withDayOfMonth(lastMonthStart.lengthOfMonth());

        // 本月订单数
        LambdaQueryWrapper<Order> monthOrderWrapper = new LambdaQueryWrapper<>();
        monthOrderWrapper.eq(Order::getHostId, hostId)
                .between(Order::getWeddingDate, monthStart, monthEnd);
        long monthOrders = orderService.count(monthOrderWrapper);
        stats.setMonthOrders((int) monthOrders);

        // 上月订单数（计算环比）
        LambdaQueryWrapper<Order> lastMonthOrderWrapper = new LambdaQueryWrapper<>();
        lastMonthOrderWrapper.eq(Order::getHostId, hostId)
                .between(Order::getWeddingDate, lastMonthStart, lastMonthEnd);
        long lastMonthOrders = orderService.count(lastMonthOrderWrapper);
        stats.setOrderTrend((int) (monthOrders - lastMonthOrders));

        // 待处理问卷数
        LambdaQueryWrapper<QuestionnaireSubmission> qsWrapper = new LambdaQueryWrapper<>();
        qsWrapper.eq(QuestionnaireSubmission::getHostId, hostId)
                .eq(QuestionnaireSubmission::getIsDeleted, 0)
                .eq(QuestionnaireSubmission::getStatus, 2); // 2表示待处理
        long pendingQuestionnaires = questionnaireSubmissionService.count(qsWrapper);
        stats.setPendingQuestionnaires((int) pendingQuestionnaires);

        // 本月收入
        List<Order> monthOrderList = orderService.list(monthOrderWrapper);
        BigDecimal monthIncome = monthOrderList.stream()
                .map(Order::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setMonthIncome(monthIncome);

        // 上月收入（计算环比）
        List<Order> lastMonthOrderList = orderService.list(lastMonthOrderWrapper);
        BigDecimal lastMonthIncome = lastMonthOrderList.stream()
                .map(Order::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算收入增长率
        if (lastMonthIncome.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal incomeTrend = monthIncome.subtract(lastMonthIncome)
                    .divide(lastMonthIncome, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal(100));
            stats.setIncomeTrend(incomeTrend.intValue());
        } else {
            stats.setIncomeTrend(monthIncome.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0);
        }

        // 假设月目标为10万，计算完成率
        BigDecimal monthTarget = new BigDecimal("100000");
        if (monthTarget.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal incomeRate = monthIncome.divide(monthTarget, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal(100));
            stats.setIncomeRate(incomeRate.intValue());
        }

        // 获取主持人评分信息
        Host host = hostService.getById(hostId);
        if (host != null) {
            stats.setRating(host.getRating() != null ? host.getRating().doubleValue() : 5.0);
            // 评价数量可以从订单中统计已完成的订单数
            LambdaQueryWrapper<Order> completedWrapper = new LambdaQueryWrapper<>();
            completedWrapper.eq(Order::getHostId, hostId)
                    .eq(Order::getStatus, 6); // 假设6表示已完成
            long ratingCount = orderService.count(completedWrapper);
            stats.setRatingCount((int) ratingCount);
        }

        return Result.ok(stats);
    }

    /**
     * 获取主持人待处理问卷列表
     * @param current 当前页
     * @param size 每页大小
     * @return 问卷列表
     */
    @GetMapping("/questionnaires")
    @PreAuthorize("hasAnyRole('HOST', 'ADMIN')")
    public Result<PageResult<QuestionnaireSubmission>> getHostPendingQuestionnaires(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {

        Long hostId = SecurityUtils.getCurrentRefId();
        if (hostId == null) {
            return Result.error("未找到主持人信息");
        }

        Page<QuestionnaireSubmission> page = new Page<>(current, size);
        LambdaQueryWrapper<QuestionnaireSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionnaireSubmission::getHostId, hostId)
                .eq(QuestionnaireSubmission::getStatus, 2) // 2表示待处理
                .orderByAsc(QuestionnaireSubmission::getCreateTime);

        questionnaireSubmissionService.page(page, wrapper);

        // 为每个问卷设置submissionName（根据userId获取用户名）
        List<QuestionnaireSubmission> records = page.getRecords();
        if (records != null && !records.isEmpty()) {
            // 收集所有userId
            List<Long> userIds = records.stream()
                    .map(QuestionnaireSubmission::getUserId)
                    .filter(userId -> userId != null)
                    .distinct()
                    .collect(Collectors.toList());

            // 批量查询用户信息
            if (!userIds.isEmpty()) {
                LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
                userWrapper.in(User::getId, userIds);
                List<User> users = userService.list(userWrapper);

                // 构建userId到User的映射
                Map<Long, User> userMap = users.stream()
                        .collect(Collectors.toMap(User::getId, user -> user));

                // 为每个问卷设置submissionName
                records.forEach(submission -> {
                    if (submission.getSubmissionName() == null && submission.getUserId() != null) {
                        User user = userMap.get(submission.getUserId());
                        if (user != null) {
                            String brideName = user.getBrideName() != null ? user.getBrideName() : "";
                            String groomName = user.getGroomName() != null ? user.getGroomName() : "";
                            submission.setSubmissionName(groomName + "&" + brideName);
                        }
                    }
                });
            }
        }

        return Result.ok(PageResult.of(page));
    }

    /**
     * 获取主持人本月档期
     * @param year 年份
     * @param month 月份
     * @return 档期列表
     */
    @GetMapping("/schedule")
    @PreAuthorize("hasAnyRole('HOST', 'ADMIN')")
    public Result<List<ScheduleItemDTO>> getHostMonthSchedule(
            @RequestParam Integer year,
            @RequestParam Integer month) {

        Long hostId = SecurityUtils.getCurrentRefId();
        if (hostId == null) {
            return Result.error("未找到主持人信息");
        }

        // 构建月份的开始和结束日期
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getHostId, hostId)
                .between(Order::getWeddingDate, startDate, endDate)
                .orderByAsc(Order::getWeddingDate);

        List<Order> orders = orderService.list(wrapper);

        // 转换为ScheduleItemDTO
        List<ScheduleItemDTO> scheduleItems = orders.stream()
                .map(order -> {
                    ScheduleItemDTO item = new ScheduleItemDTO();
                    item.setDate(order.getWeddingDate().toString());
                    item.setStatus(order.getStatus());
                    return item;
                })
                .collect(Collectors.toList());

        return Result.ok(scheduleItems);
    }

    /**
     * 获取即将到来的婚礼列表
     * @param current 当前页
     * @param size 每页大小
     * @return 婚礼列表
     */
    @GetMapping("/upcoming-weddings")
    @PreAuthorize("hasAnyRole('HOST', 'ADMIN')")
    public Result<PageResult<UpcomingWeddingDTO>> getUpcomingWeddings(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {

        Long hostId = SecurityUtils.getCurrentRefId();
        if (hostId == null) {
            return Result.error("未找到主持人信息");
        }

        Page<Order> page = new Page<>(current, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getHostId, hostId)
                .ge(Order::getWeddingDate, LocalDate.now()) // 大于等于今天
                .in(Order::getStatus, 2, 3, 6) // 沟通中、已确认、已完成
                .orderByAsc(Order::getWeddingDate);

        orderService.page(page, wrapper);

        // 转换为UpcomingWeddingDTO
        List<UpcomingWeddingDTO> weddingList = page.getRecords().stream()
                .map(order -> {
                    UpcomingWeddingDTO dto = new UpcomingWeddingDTO();
                    dto.setId(order.getId());
                    dto.setCoupleName(order.getUserName());
                    dto.setWeddingDate(order.getWeddingDate().toString());
                    dto.setWeddingType(order.getWeddingType());
//                    dto.setVenue(order); // 需要从订单扩展字段或其他表获取

                    // 设置状态
                    switch (order.getStatus()) {
                        case 2:
                            dto.setStatus("communicating");
                            break;
                        case 3:
                            dto.setStatus("confirmed");
                            break;
                        case 6:
                            dto.setStatus("completed");
                            break;
                        default:
                            dto.setStatus("pending");
                    }

                    return dto;
                })
                .collect(Collectors.toList());

        PageResult<UpcomingWeddingDTO> result = new PageResult<>();
        result.setRecords(weddingList);
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());

        return Result.ok(result);
    }

    /**
     * 获取主持人本月订单列表
     * @param current 当前页
     * @param size 每页大小
     * @return 订单列表
     */
    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('HOST', 'ADMIN')")
    public Result<PageResult<Order>> getHostMonthOrders(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {

        Long hostId = SecurityUtils.getCurrentRefId();
        if (hostId == null) {
            return Result.error("未找到主持人信息");
        }

        // 获取本月日期范围
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());

        Page<Order> page = new Page<>(current, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getHostId, hostId)
                .between(Order::getWeddingDate, monthStart, monthEnd)
                .orderByDesc(Order::getCreateTime);

        orderService.page(page, wrapper);

        return Result.ok(PageResult.of(page));
    }
}
