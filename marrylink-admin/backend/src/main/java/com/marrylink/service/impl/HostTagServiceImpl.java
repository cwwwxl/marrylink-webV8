package com.marrylink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.marrylink.entity.HostTag;
import com.marrylink.mapper.HostTagMapper;
import com.marrylink.service.IHostTagService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
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
public class HostTagServiceImpl extends ServiceImpl<HostTagMapper, HostTag> implements IHostTagService {

    @Override
    public List<String> getTagListByHostId(Long hostId) {
        LambdaQueryWrapper<HostTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HostTag::getHostId,hostId);
        List<HostTag> list = list(wrapper);
        List<String> tagIds = list.stream()
                .map(HostTag::getTagCode)
                .collect(Collectors.toList());
        return tagIds;
    }
}
