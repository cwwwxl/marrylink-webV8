package com.marrylink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.marrylink.common.PageResult;
import com.marrylink.entity.HostVideo;
import com.marrylink.mapper.HostVideoMapper;
import com.marrylink.service.IHostVideoService;
import org.springframework.stereotype.Service;

@Service
public class HostVideoServiceImpl extends ServiceImpl<HostVideoMapper, HostVideo> implements IHostVideoService {

    @Override
    public PageResult<HostVideo> pageVideo(Long current, Long size, Long hostId, Integer status, Integer showOnHome, String keyword) {
        Page<HostVideo> page = new Page<>(current, size);
        LambdaQueryWrapper<HostVideo> wrapper = new LambdaQueryWrapper<>();

        if (hostId != null) {
            wrapper.eq(HostVideo::getHostId, hostId);
        }
        if (status != null) {
            wrapper.eq(HostVideo::getStatus, status);
        }
        if (showOnHome != null) {
            wrapper.eq(HostVideo::getShowOnHome, showOnHome);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(HostVideo::getTitle, keyword);
        }

        wrapper.orderByDesc(HostVideo::getCreateTime);
        page(page, wrapper);

        return PageResult.of(page);
    }
}
