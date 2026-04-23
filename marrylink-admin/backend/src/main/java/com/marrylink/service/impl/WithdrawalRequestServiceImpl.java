package com.marrylink.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.marrylink.entity.WithdrawalRequest;
import com.marrylink.mapper.WithdrawalRequestMapper;
import com.marrylink.service.IWithdrawalRequestService;
import org.springframework.stereotype.Service;

@Service
public class WithdrawalRequestServiceImpl extends ServiceImpl<WithdrawalRequestMapper, WithdrawalRequest> implements IWithdrawalRequestService {

}
