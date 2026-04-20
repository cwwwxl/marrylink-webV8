package com.marrylink.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.marrylink.common.PageResult;
import com.marrylink.common.Result;
import com.marrylink.entity.HostVideo;
import com.marrylink.service.IHostVideoService;
import com.marrylink.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/host-video")
public class HostVideoController {

    @Autowired
    private IHostVideoService hostVideoService;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("mp4", "mov", "avi", "flv", "wmv");

    @GetMapping("/page")
    public Result<PageResult<HostVideo>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Long hostId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer showOnHome,
            @RequestParam(required = false) String keyword) {
        PageResult<HostVideo> pageResult = hostVideoService.pageVideo(current, size, hostId, status, showOnHome, keyword);
        return Result.ok(pageResult);
    }

    @GetMapping("/{id}")
    public Result<HostVideo> getById(@PathVariable Long id) {
        HostVideo video = hostVideoService.getById(id);
        if (video == null) {
            return Result.error("视频不存在");
        }
        return Result.ok(video);
    }

    @PostMapping("/upload")
    public Result<HostVideo> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(required = false) String title,
                                    @RequestParam(required = false) String description) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        Long hostId = SecurityUtils.getCurrentRefId();
        if (hostId == null) {
            return Result.error("未找到主持人信息");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return Result.error("无效的文件名");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return Result.error("不支持的文件格式，仅支持: mp4, mov, avi, flv, wmv");
        }

        try {
            String filename = UUID.randomUUID().toString() + "." + extension;
            String uploadDir = System.getProperty("user.dir") + File.separator + "marrylink-admin" + File.separator + "uploads" + File.separator + "videos";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath.toFile());

            String videoUrl = "/uploads/videos/" + filename;

            HostVideo hostVideo = new HostVideo();
            hostVideo.setHostId(hostId);
            hostVideo.setTitle(title != null ? title : originalFilename);
            hostVideo.setDescription(description);
            hostVideo.setVideoUrl(videoUrl);
            hostVideo.setFileSize(file.getSize());
            hostVideo.setStatus(1);
            hostVideo.setShowOnHome(0);
            hostVideo.setSortOrder(0);

            hostVideoService.save(hostVideo);
            return Result.ok(hostVideo);
        } catch (IOException e) {
            return Result.error("文件上传失败");
        }
    }

    @PutMapping
    public Result<Void> update(@RequestBody HostVideo hostVideo) {
        HostVideo updateVideo = new HostVideo();
        updateVideo.setId(hostVideo.getId());
        updateVideo.setTitle(hostVideo.getTitle());
        updateVideo.setDescription(hostVideo.getDescription());
        updateVideo.setCoverUrl(hostVideo.getCoverUrl());
        updateVideo.setDuration(hostVideo.getDuration());
        updateVideo.setSortOrder(hostVideo.getSortOrder());
        hostVideoService.updateById(updateVideo);
        return Result.ok();
    }

    @PutMapping("/{id}/showOnHome")
    public Result<Void> updateShowOnHome(@PathVariable Long id, @RequestParam Integer showOnHome) {
        HostVideo video = new HostVideo();
        video.setId(id);
        video.setShowOnHome(showOnHome);
        hostVideoService.updateById(video);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        HostVideo video = new HostVideo();
        video.setId(id);
        video.setStatus(status);
        hostVideoService.updateById(video);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        hostVideoService.removeById(id);
        return Result.ok();
    }

    @GetMapping("/home-list")
    public Result<List<HostVideo>> homeList() {
        LambdaQueryWrapper<HostVideo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HostVideo::getShowOnHome, 1)
               .eq(HostVideo::getStatus, 1)
               .orderByAsc(HostVideo::getSortOrder);
        List<HostVideo> list = hostVideoService.list(wrapper);
        return Result.ok(list);
    }
}
