package com.marrylink.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.marrylink.common.Result;
import com.marrylink.entity.PlatformSettings;
import com.marrylink.service.IPlatformSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/platform-settings")
public class PlatformSettingsController {

    @Autowired
    private IPlatformSettingsService platformSettingsService;

    /**
     * 获取所有平台设置（管理员）
     */
    @GetMapping("/list")
    public Result<List<PlatformSettings>> list() {
        LambdaQueryWrapper<PlatformSettings> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(PlatformSettings::getId);
        return Result.ok(platformSettingsService.list(wrapper));
    }

    /**
     * 根据key更新设置（管理员）
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody Map<String, Object> params) {
        String settingKey = params.get("settingKey").toString();
        String settingValue = params.get("settingValue").toString();

        LambdaQueryWrapper<PlatformSettings> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlatformSettings::getSettingKey, settingKey);
        PlatformSettings setting = platformSettingsService.getOne(wrapper);

        if (setting == null) {
            return Result.error("设置项不存在");
        }

        setting.setSettingValue(settingValue);
        platformSettingsService.updateById(setting);

        return Result.ok();
    }

    /**
     * 根据key获取设置
     */
    @GetMapping("/{key}")
    public Result<PlatformSettings> getByKey(@PathVariable String key) {
        LambdaQueryWrapper<PlatformSettings> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlatformSettings::getSettingKey, key);
        PlatformSettings setting = platformSettingsService.getOne(wrapper);
        return Result.ok(setting);
    }
}
