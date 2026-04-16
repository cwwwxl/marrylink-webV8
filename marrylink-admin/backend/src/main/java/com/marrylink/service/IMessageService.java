package com.marrylink.service;

import com.marrylink.entity.Message;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IMessageService extends IService<Message> {
    void sendOrderCreatedMessage(Long userId, Long hostId, String userName, String weddingDate);
}
