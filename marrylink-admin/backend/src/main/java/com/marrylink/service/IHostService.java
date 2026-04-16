package com.marrylink.service;

import com.marrylink.common.PageResult;
import com.marrylink.entity.Host;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author author
 * @since 2026-01-04
 */
public interface IHostService extends IService<Host> {

    PageResult<Host> pageHost(Long current, Long size, Integer status, String serviceAreas, String keyword, String tag);

    void saveHost(Host host);

    void updateHost(Host host);

    void handleServiceAreas(Host host);

}
