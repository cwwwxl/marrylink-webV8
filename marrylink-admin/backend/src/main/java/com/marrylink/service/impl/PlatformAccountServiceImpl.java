package com.marrylink.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.marrylink.entity.PlatformAccount;
import com.marrylink.mapper.PlatformAccountMapper;
import com.marrylink.service.IPlatformAccountService;
import org.springframework.stereotype.Service;

@Service
public class PlatformAccountServiceImpl extends ServiceImpl<PlatformAccountMapper, PlatformAccount> implements IPlatformAccountService {
}
