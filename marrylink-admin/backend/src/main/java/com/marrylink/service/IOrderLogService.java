package com.marrylink.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.marrylink.entity.OrderLog;

public interface IOrderLogService extends IService<OrderLog> {
    void logOrderStatusChange(String orderNo, Integer oldStatus, Integer newStatus, String operator, String operatorIp);
}