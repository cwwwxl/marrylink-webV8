package com.marrylink.service;

import com.marrylink.entity.Message;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IMessageService extends IService<Message> {
    void sendOrderCreatedMessage(Long userId, Long hostId, String userName, String weddingDate);

    /**
     * 发送佣金账单通知给主持人
     */
    void sendCommissionBillMessage(Long hostId, String orderNo, String commissionAmount, String deadline);
}
