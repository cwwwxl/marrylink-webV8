package com.marrylink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.marrylink.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统角色 Mapper
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
    
    /**
     * 根据账号ID查询角色编码列表
     */
    List<String> selectRoleCodesByAccountId(@Param("accountId") Long accountId);
}