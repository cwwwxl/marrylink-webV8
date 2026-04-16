package com.marrylink.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.marrylink.common.PageResult;
import com.marrylink.common.Result;
import com.marrylink.entity.Message;
import com.marrylink.service.IMessageService;
import com.marrylink.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {
    @Resource
    private IMessageService messageService;

    @GetMapping("/unread")
    public Result<Map<String, Object>> getUnreadMessages() {
        Long userId = SecurityUtils.getCurrentRefId();
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getStatus, 1)
               .orderByDesc(Message::getCreateTime);
        if (SecurityUtils.hasRole("CUSTOMER")) {
            wrapper.eq(Message::getUserId, userId);
        } else {
            wrapper.eq(Message::getHostId, userId);
        }

        List<Message> messages = messageService.list(wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("messages", messages);
        result.put("count", messages.size());
        return Result.ok(result);
    }

    @GetMapping("/unread/count")
    public Result<Map<String, Object>> getUnreadCount() {
        Long userId = SecurityUtils.getCurrentRefId();
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getStatus, 1);

        if (SecurityUtils.hasRole("CUSTOMER")) {
            wrapper.eq(Message::getUserId, userId);
        } else {
            wrapper.eq(Message::getHostId, userId);
        }
        long count = messageService.count(wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return Result.ok(result);
    }

    @PostMapping("/mark-read")
    public Result<Void> markAsRead(@RequestBody(required = false) List<Long> messageIds) {
        Long userId = SecurityUtils.getCurrentRefId();
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        
        if (messageIds != null && !messageIds.isEmpty()) {
            wrapper.in(Message::getId, messageIds);
        } else {
            wrapper.eq(Message::getStatus, 1);
        }
        
        if (SecurityUtils.hasRole("CUSTOMER")) {
            wrapper.eq(Message::getUserId, userId);
        } else {
            wrapper.eq(Message::getHostId, userId);
        }

        Message update = new Message();
        update.setStatus(2);
        messageService.update(update, wrapper);
        return Result.ok();
    }

    @GetMapping("/list")
    public Result<PageResult<Message>> getMessageList(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status) {
        Long userId = SecurityUtils.getCurrentRefId();

        Page<Message> page = new Page<>(current, size);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(Message::getStatus, status);
        }

        if (SecurityUtils.hasRole("CUSTOMER")) {
            wrapper.eq(Message::getUserId, userId);
        } else {
            wrapper.eq(Message::getHostId, userId);
        }

        wrapper.orderByDesc(Message::getCreateTime);
        messageService.page(page, wrapper);

        return Result.ok(PageResult.of(page));
    }
}
