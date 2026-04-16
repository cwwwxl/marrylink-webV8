package com.marrylink.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.marrylink.common.PageResult;
import com.marrylink.common.Result;
import com.marrylink.dto.HostImportDTO;
import com.marrylink.entity.Host;
import com.marrylink.entity.HostTag;
import com.marrylink.entity.Order;
import com.marrylink.listener.HostImportListener;
import com.marrylink.service.IHostService;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.marrylink.service.IHostTagService;
import com.marrylink.service.IOrderService;
import com.marrylink.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/host")
public class HostController {

    @Autowired
    private IHostService hostService;

    @Resource
    private IHostTagService hostTagService;

    @Resource
    private IOrderService orderService;

    @GetMapping("/page")
    public Result<PageResult<Host>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String serviceAreas,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag) {
        PageResult<Host> pageList = hostService.pageHost(current, size, status, serviceAreas, keyword, tag);

        return Result.ok(pageList);
    }

    @GetMapping("/info")
    public Result<Host> getInfo() {
        Long refId = SecurityUtils.getCurrentRefId();
        if (refId == null) {
            return Result.error("未找到主持人信息");
        }

        Host host = hostService.getById(refId);
        if (host == null) {
            return Result.error("主持人不存在");
        }

        List<String> tags = hostTagService.getTagListByHostId(refId);
        host.setTags(tags);
        return Result.ok(host);
    }

    @GetMapping("/{id}")
    public Result<Host> getById(@PathVariable Long id) {
        List<String> tags = hostTagService.getTagListByHostId(id);
        Host host = hostService.getById(id);
        host.setTags(tags);
        return Result.ok(host);
    }

    @PostMapping
    public Result<Void> save(@RequestBody Host host) {
        hostService.saveHost(host);
        return Result.ok();
    }

    @PostMapping("/update")
    public Result<Void> updateInfo(@RequestBody Host host) {
        Long refId = SecurityUtils.getCurrentRefId();
        if (refId == null) {
            return Result.error("未找到主持人信息");
        }

        host.setId(refId);
        hostService.updateById(host);
        return Result.ok();
    }

    @PostMapping("/uploadAvatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        Long refId = SecurityUtils.getCurrentRefId();
        if (refId == null) {
            return Result.error("未找到主持人信息");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID().toString() + extension;

            // 使用绝对路径
            String uploadDir = System.getProperty("user.dir") + File.separator + "marrylink-admin" + File.separator + "uploads" + File.separator + "avatars";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath.toFile());

            String avatarUrl = "/uploads/avatars/" + filename;

            // 更新数据库中的头像字段
            Host host = new Host();
            host.setId(refId);
            host.setAvatar(avatarUrl);
            hostService.updateById(host);

            return Result.ok(avatarUrl);
        } catch (IOException e) {
            return Result.error("文件上传失败");
        }
    }

    @PutMapping
    public Result<Void> update(@RequestBody Host host) {
        hostService.updateHost(host);
        return Result.ok();
    }



    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(@PathVariable Long id) {
        LambdaQueryWrapper<HostTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HostTag::getHostId,id);
        hostTagService.remove(wrapper);

        hostService.removeById(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        Host host = new Host();
        host.setId(id);
        host.setStatus(status);
        hostService.updateById(host);
        return Result.ok();
    }

    @GetMapping("/monthlyOrders")
    public Result<List<Map<String, Object>>> getMonthlyOrders() {
        Long refId = SecurityUtils.getCurrentRefId();
        if (refId == null) {
            return Result.error("未找到主持人信息");
        }

        int currentYear = LocalDate.now().getYear();

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getHostId, refId)
               .ge(Order::getCreateTime, LocalDate.of(currentYear, 1, 1).atStartOfDay())
               .le(Order::getCreateTime, LocalDate.of(currentYear, 12, 31).atTime(23, 59, 59));

        List<Order> orders = orderService.list(wrapper);

        Map<Integer, Long> monthCountMap = orders.stream()
            .collect(Collectors.groupingBy(
                order -> order.getCreateTime().getMonthValue(),
                Collectors.counting()
            ));

        List<Map<String, Object>> result = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            Map<String, Object> item = new HashMap<>();
            item.put("month", month);
            item.put("count", monthCountMap.getOrDefault(month, 0L));
            result.add(item);
        }

        return Result.ok(result);
    }

    /**
     * 获取主持人的用户评价列表（从已完成且有评分的订单中获取）
     * @param id 主持人ID
     * @return 评价列表
     */
    @GetMapping("/{id}/reviews")
    public Result<List<Order>> getHostReviews(@PathVariable Long id) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getHostId, id)
               .eq(Order::getStatus, 4)  // 已完成状态
               .isNotNull(Order::getRating)  // 有评分
               .orderByDesc(Order::getWeddingDate);
        
        List<Order> reviews = orderService.list(wrapper);
        return Result.ok(reviews);
    }

    /**
     * 导入主持人
     */
    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> importHost(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            return Result.error("只支持Excel文件格式(.xlsx, .xls)");
        }

        try {
            HostImportListener listener = new HostImportListener(hostService);
            EasyExcel.read(file.getInputStream(), HostImportDTO.class, listener)
                    .sheet()
                    .headRowNumber(2)  // 表头占1行，从第2行开始读取数据
                    .doRead();

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("successCount", listener.getSuccessCount());
            resultMap.put("errorMessages", listener.getErrorMessages());
            resultMap.put("hasErrors", listener.hasErrors());

            if (listener.hasErrors()) {
                return Result.ok(resultMap, "导入完成，但存在部分错误");
            }
            return Result.ok(resultMap, "导入成功");
        } catch (IOException e) {
            return Result.error("文件读取失败: " + e.getMessage());
        } catch (Exception e) {
            return Result.error("导入失败: " + e.getMessage());
        }
    }

    /**
     * 下载导入模板
     */
    @GetMapping("/template")
    @PreAuthorize("hasRole('ADMIN')")
    public void downloadTemplate(HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("主持人导入模板", StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            // 从 resources 目录读取模板文件
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("excel/hostImport.xlsx");
            if (inputStream == null) {
                throw new RuntimeException("模板文件不存在");
            }

            // 将模板文件内容写入响应输出流
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                response.getOutputStream().write(buffer, 0, bytesRead);
            }
            inputStream.close();
            response.getOutputStream().flush();
        } catch (IOException e) {
            throw new RuntimeException("模板下载失败: " + e.getMessage());
        }
    }
}
