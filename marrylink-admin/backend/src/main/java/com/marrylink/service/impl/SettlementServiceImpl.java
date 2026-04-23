package com.marrylink.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.marrylink.entity.Settlement;
import com.marrylink.mapper.SettlementMapper;
import com.marrylink.service.ISettlementService;
import org.springframework.stereotype.Service;

@Service
public class SettlementServiceImpl extends ServiceImpl<SettlementMapper, Settlement> implements ISettlementService {

}
