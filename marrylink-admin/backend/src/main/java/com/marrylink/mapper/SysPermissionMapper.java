package com.marrylink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.marrylink.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统权限 Mapper
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 根据账号ID查询权限编码列表
     */
    List<String> selectPermissionCodesByAccountId(@Param("accountId") Long accountId);

    /**
     * 根据账号ID + 资源类型查询权限编码列表（MENU/API/BUTTON）
     */
    List<String> selectPermissionCodesByAccountIdAndType(@Param("accountId") Long accountId,
                                                         @Param("resourceType") String resourceType);
}