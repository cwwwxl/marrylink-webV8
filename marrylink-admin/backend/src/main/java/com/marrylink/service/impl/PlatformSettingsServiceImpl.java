package com.marrylink.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.marrylink.entity.PlatformSettings;
import com.marrylink.mapper.PlatformSettingsMapper;
import com.marrylink.service.IPlatformSettingsService;
import org.springframework.stereotype.Service;

@Service
public class PlatformSettingsServiceImpl extends ServiceImpl<PlatformSettingsMapper, PlatformSettings> implements IPlatformSettingsService {

}
