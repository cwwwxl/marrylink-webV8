package com.marrylink.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.marrylink.entity.AccountMapping;

/**
 * 账号映射服务接口
 */
public interface AccountMappingService extends IService<AccountMapping> {
    
    /**
     * 根据账号ID和用户类型查询
     */
    AccountMapping getByAccountAndType(String accountId, String userType);
    
    /**
     * 创建账号映射
     */
    AccountMapping createAccount(String accountId, String userType, Long refId, String password);
    
    /**
     * 修改密码
     */
    boolean changePassword(Long accountId, String newPassword);
}