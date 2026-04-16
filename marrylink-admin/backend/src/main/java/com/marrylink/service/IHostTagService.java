package com.marrylink.service;

import com.marrylink.entity.Host;
import com.marrylink.entity.HostTag;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author author
 * @since 2026-01-04
 */
public interface IHostTagService extends IService<HostTag> {

    List<String> getTagListByHostId(Long hostId);

}
