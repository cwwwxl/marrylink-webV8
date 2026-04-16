package com.marrylink.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.marrylink.entity.Message;
import com.marrylink.mapper.MessageMapper;
import com.marrylink.service.IMessageService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements IMessageService {

    @Async
    @Override
    public void sendOrderCreatedMessage(Long userId,Long hostId, String userName, String weddingDate) {
        Message message = new Message();
        message.setUserId(userId);
        message.setContent(userName + " " + weddingDate + " 的订单已创建，请与新人进行沟通");
        message.setHostId(hostId);
        message.setStatus(1);
        this.save(message);
    }
}
