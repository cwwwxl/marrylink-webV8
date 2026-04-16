package com.marrylink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.marrylink.entity.AccountMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 账号映射 Mapper
 */
@Mapper
public interface AccountMappingMapper extends BaseMapper<AccountMapping> {
    
    /**
     * 根据账号ID和用户类型查询
     */
    AccountMapping selectByAccountAndType(@Param("accountId") String accountId, 
                                         @Param("userType") String userType);
}