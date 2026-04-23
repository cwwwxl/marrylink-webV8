package com.marrylink.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.marrylink.entity.CommissionOrder;
import com.marrylink.mapper.CommissionOrderMapper;
import com.marrylink.service.ICommissionOrderService;
import org.springframework.stereotype.Service;

@Service
public class CommissionOrderServiceImpl extends ServiceImpl<CommissionOrderMapper, CommissionOrder> implements ICommissionOrderService {

}
