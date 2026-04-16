package com.marrylink.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.marrylink.entity.AccountMapping;
import com.marrylink.mapper.AccountMappingMapper;
import com.marrylink.service.AccountMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 账号映射服务实现
 */
@Service
public class AccountMappingServiceImpl extends ServiceImpl<AccountMappingMapper, AccountMapping>
    implements AccountMappingService {
    
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public AccountMappingServiceImpl(@Lazy PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    public AccountMapping getByAccountAndType(String accountId, String userType) {
        return baseMapper.selectByAccountAndType(accountId, userType);
    }
    
    @Override
    public AccountMapping createAccount(String accountId, String userType, Long refId, String password) {
        AccountMapping account = new AccountMapping();
        account.setAccountId(accountId);
        account.setUserType(userType);
        account.setRefId(refId);
        account.setPassword(passwordEncoder.encode(password));
        account.setStatus(1);
        
        save(account);
        return account;
    }
    
    @Override
    public boolean changePassword(Long accountId, String newPassword) {
        AccountMapping account = getById(accountId);
        if (account == null) {
            return false;
        }
        
        account.setPassword(passwordEncoder.encode(newPassword));
        return updateById(account);
    }
}