package com.marrylink.service;

import com.marrylink.common.PageResult;
import com.marrylink.entity.HostVideo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IHostVideoService extends IService<HostVideo> {

    PageResult<HostVideo> pageVideo(Long current, Long size, Long hostId, Integer status, Integer showOnHome, String keyword);
}
