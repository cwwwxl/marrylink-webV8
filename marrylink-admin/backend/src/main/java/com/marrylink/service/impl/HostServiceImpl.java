package com.marrylink.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.marrylink.common.PageResult;
import com.marrylink.entity.AccountMapping;
import com.marrylink.entity.Host;
import com.marrylink.entity.HostTag;
import com.marrylink.entity.Order;
import com.marrylink.entity.SysRole;
import com.marrylink.entity.SysUserRole;
import com.marrylink.entity.Tag;
import com.marrylink.enums.UserType;
import com.marrylink.mapper.HostMapper;
import com.marrylink.service.AccountMappingService;
import com.marrylink.service.IHostService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.marrylink.service.IHostTagService;
import com.marrylink.service.IOrderService;
import com.marrylink.service.ITagService;
import com.marrylink.service.RoleService;
import com.marrylink.service.UserRoleService;
import com.marrylink.utils.PasswordUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author author
 * @since 2026-01-04
 */
@Service
public class HostServiceImpl extends ServiceImpl<HostMapper, Host> implements IHostService {
    @Resource
    private IHostTagService hostTagService;
    @Resource
    private AccountMappingService accountMappingService;
    @Resource
    private RoleService roleService;
    @Resource
    private UserRoleService userRoleService;
    @Lazy
    @Resource
    private IOrderService orderService;

    @Override
    public PageResult<Host> pageHost(Long current, Long size, Integer status, String serviceAreas, String keyword, String tag) {
        Page<Host> page = new Page<>(current, size);
        LambdaQueryWrapper<Host> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(Host::getStatus, status);
        }
        if (serviceAreas != null && !serviceAreas.isEmpty()) {
            wrapper.like(Host::getServiceAreas, serviceAreas);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Host::getName, keyword)
                    .or().like(Host::getHostCode, keyword)
                    .or().like(Host::getPhone, keyword));
        }

        // 根据标签筛选主持人
        if (tag != null && !tag.isEmpty()) {
            // 先查询拥有该标签的主持人ID列表
            LambdaQueryWrapper<HostTag> tagWrapper = new LambdaQueryWrapper<>();
            tagWrapper.eq(HostTag::getTagCode, tag);
            List<HostTag> hostTags = hostTagService.list(tagWrapper);

            if (hostTags != null && !hostTags.isEmpty()) {
                List<Long> hostIds = hostTags.stream()
                        .map(HostTag::getHostId)
                        .collect(Collectors.toList());
                wrapper.in(Host::getId, hostIds);
            } else {
                // 如果没有找到任何匹配的主持人，返回空结果
                return PageResult.of(page);
            }
        }

        wrapper.orderByAsc(Host::getJoinTime);
        page(page, wrapper);

        List<Host> records = page.getRecords();
        setTagsForHosts(records);  // 自定义方法设置标签
        setOrderCountForHosts(records);  // 设置订单数

        return PageResult.of(page);
    }

    private void setTagsForHosts(List<Host> hosts) {
        if (hosts == null || hosts.isEmpty()) {
            return;
        }

        List<Long> hostIds = hosts.stream()
                .map(Host::getId)
                .collect(Collectors.toList());

        // 批量查询所有标签
        LambdaQueryWrapper<HostTag> tagWrapper = new LambdaQueryWrapper<>();
        tagWrapper.in(HostTag::getHostId, hostIds);
        List<HostTag> allTags = hostTagService.list(tagWrapper);

        // 按 hostId 分组
        Map<Long, HostTag> map = new HashMap<>();
        for (HostTag hostTag : allTags) {
            map.put(hostTag.getHostId(),hostTag);
        }
        Map<Long, List<String>> tagsMap = allTags.stream()
                .collect(Collectors.groupingBy(
                        HostTag::getHostId,
                        Collectors.mapping(HostTag::getTagCode, Collectors.toList())
                ));

        // 设置到每个 Host 对象
        for (Host host : hosts) {
            List<String> tagList = tagsMap.getOrDefault(host.getId(), new ArrayList<>());
            host.setTags(tagList);
        }
    }

    /**
     * 批量设置主持人的订单数
     */
    private void setOrderCountForHosts(List<Host> hosts) {
        if (hosts == null || hosts.isEmpty()) {
            return;
        }

        List<Long> hostIds = hosts.stream()
                .map(Host::getId)
                .collect(Collectors.toList());

        // 批量查询每个主持人的订单数
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.in(Order::getHostId, hostIds)
                   .ne(Order::getStatus, 5);  // 排除已取消的订单

        List<Order> allOrders = orderService.list(orderWrapper);

        // 按 hostId 分组统计订单数
        Map<Long, Long> orderCountMap = allOrders.stream()
                .collect(Collectors.groupingBy(
                        Order::getHostId,
                        Collectors.counting()
                ));

        // 设置到每个 Host 对象
        for (Host host : hosts) {
            Long count = orderCountMap.getOrDefault(host.getId(), 0L);
            host.setOrderCount(count.intValue());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveHost(Host host) {
        handleServiceAreas(host);
        save(host);

        // 创建账号映射
        AccountMapping accountMapping = new AccountMapping();
        accountMapping.setAccountId(host.getPhone());
        accountMapping.setUserType(UserType.HOST.getCode());
        accountMapping.setRefId(host.getId());
        accountMapping.setPassword(PasswordUtils.encode("123456")); // 默认密码
        accountMapping.setStatus(1);
        accountMappingService.save(accountMapping);

        // 分配主持人角色
        SysRole hostRole = roleService.lambdaQuery()
            .eq(SysRole::getRoleCode, "ROLE_HOST")
            .one();
        if (hostRole != null) {
            SysUserRole userRole = new SysUserRole();
            userRole.setAccountId(accountMapping.getId());
            userRole.setRoleId(hostRole.getId());
            userRoleService.save(userRole);
        }

        List<String> tags = host.getTags();
        if (CollUtil.isEmpty(tags)) {
            return;
        }
        List<HostTag> hostTags = new ArrayList<>();
        for (String tagCode : tags) {
            HostTag hostTag = new HostTag();
            hostTag.setHostId(host.getId());
            hostTag.setTagCode(tagCode);
            hostTags.add(hostTag);
        }
        hostTagService.saveBatch(hostTags);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHost(Host host) {
        handleServiceAreas(host);
        updateById(host);

        List<String> tags = host.getTags();
        List<HostTag> hostTags = new ArrayList<>();
        if (CollUtil.isEmpty(tags)) {
            return;
        }
        for (String tagCode : tags) {
            HostTag hostTag = new HostTag();
            hostTag.setHostId(host.getId());
            hostTag.setTagCode(tagCode);
            hostTags.add(hostTag);
        }
        LambdaQueryWrapper<HostTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HostTag::getHostId,host.getId());
        hostTagService.remove(wrapper);
        hostTagService.saveBatch(hostTags);
    }

    @Override
    public void handleServiceAreas(Host host) {
        String areas = host.getServiceAreas();
        if (StrUtil.isBlank(areas)) {
            host.setServiceAreas(null);
            return;
        }
        // 如果不是JSON格式，则尝试转换为JSON数组
        if (!JSONUtil.isTypeJSON(areas)) {
            host.setServiceAreas(JSONUtil.toJsonStr(StrUtil.split(areas, ',')));
        }
    }
}
