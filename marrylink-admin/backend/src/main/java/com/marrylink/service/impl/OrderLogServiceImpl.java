package com.marrylink.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.marrylink.entity.OrderLog;
import com.marrylink.mapper.OrderLogMapper;
import com.marrylink.service.IOrderLogService;
import org.springframework.stereotype.Service;

@Service
public class OrderLogServiceImpl extends ServiceImpl<OrderLogMapper, OrderLog> implements IOrderLogService {
    
    @Override
    public void logOrderStatusChange(String orderNo, Integer oldStatus, Integer newStatus, String operator, String operatorIp) {
        OrderLog log = new OrderLog();
        log.setOrderNo(orderNo);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setOperator(operator);
        log.setOperatorIp(operatorIp);
        save(log);
    }
}