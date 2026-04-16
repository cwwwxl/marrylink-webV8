package com.marrylink.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.marrylink.common.PageResult;
import com.marrylink.common.Result;
import com.marrylink.entity.*;
import com.marrylink.enums.UserType;
import com.marrylink.service.IQuestionnaireSubmissionService;
import com.marrylink.service.IQuestionnaireTemplateService;
import com.marrylink.service.IUserService;
import com.marrylink.utils.FieldProcessorUtil;
import com.marrylink.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/questionnaire-submission")
public class QuestionnaireSubmissionController {

    @Autowired
    private IQuestionnaireSubmissionService submissionService;

    @Resource
    private IQuestionnaireTemplateService templateService;

    @Resource
    private IUserService userService;

    @GetMapping("/page")
    public Result<PageResult<QuestionnaireSubmission>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) Long hostId,
            @RequestParam(required = false) Integer status) {

        Page<QuestionnaireSubmission> page = new Page<>(current, size);
        LambdaQueryWrapper<QuestionnaireSubmission> wrapper = new LambdaQueryWrapper<>();

        if (templateId != null) {
            wrapper.eq(QuestionnaireSubmission::getTemplateId, templateId);
        }
        if (hostId != null) {
            wrapper.eq(QuestionnaireSubmission::getHostId, hostId);
        }
        if (status != null) {
            wrapper.eq(QuestionnaireSubmission::getStatus, status);
        }
        if (SecurityUtils.hasRole(UserType.CUSTOMER.getCode())) {
            wrapper.eq(QuestionnaireSubmission::getUserId, SecurityUtils.getCurrentRefId());
        } else if (SecurityUtils.hasRole(UserType.HOST.getCode())) {
            wrapper.in(QuestionnaireSubmission::getStatus,Arrays.asList(2,3));
            wrapper.eq(QuestionnaireSubmission::getHostId, SecurityUtils.getCurrentRefId());
        }




        wrapper.orderByDesc(QuestionnaireSubmission::getCreateTime);
        submissionService.page(page, wrapper);

        // 设置问卷名称
        List<QuestionnaireSubmission> records = page.getRecords();
        setSubName(records);

        return Result.ok(PageResult.of(page));
    }

    private void setSubName(List<QuestionnaireSubmission> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        // 收集所有模板ID
        List<Long> tempIds = records.stream()
                .map(QuestionnaireSubmission::getTemplateId)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询所有问卷模板
        LambdaQueryWrapper<QuestionnaireTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(QuestionnaireTemplate::getId, tempIds);
        List<QuestionnaireTemplate> templates = templateService.list(wrapper);

        // 构建模板ID到模板名称的映射
        HashMap<Long, QuestionnaireTemplate> templateMap = new HashMap<>();
        for (QuestionnaireTemplate entity : templates) {
            templateMap.put(entity.getId(),entity);
        }
        // 为每个提交记录设置问卷名称
        records.forEach(record -> {
            QuestionnaireTemplate template = templateMap.get(record.getTemplateId());
            record.setSubmissionName(template.getName());
            record.setQuestionCount(template.getQuestionCount());
        });
    }

    @GetMapping("/{id}")
    public Result<QuestionnaireSubmission> getById(@PathVariable Long id) {
        return Result.ok(submissionService.getById(id));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        QuestionnaireSubmission submission = new QuestionnaireSubmission();
        submission.setId(id);
        submission.setStatus(status);
        submissionService.updateById(submission);
        return Result.ok();
    }

    /**
     * 更新问卷状态（POST方式）
     */
    @PostMapping("/updateStatus")
    public Result<Void> updateStatusPost(@RequestBody QuestionnaireSubmission submission) {
        QuestionnaireSubmission update = new QuestionnaireSubmission();
        update.setId(submission.getId());
        update.setStatus(submission.getStatus());
        submissionService.updateById(update);
        return Result.ok();
    }

    /**
     * 用户端：获取用户的问卷列表
     */
    @GetMapping("/userQuestion")
    public Result<PageResult<QuestionnaireSubmission>> getUserSubmissions(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status) {
        // 从token中获取当前用户ID
        Long userId = SecurityUtils.getCurrentRefId();

        Page<QuestionnaireSubmission> page = new Page<>(current, size);
        LambdaQueryWrapper<QuestionnaireSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionnaireSubmission::getUserId, userId);

        if (status != null) {
            wrapper.eq(QuestionnaireSubmission::getStatus, status);
        }

        wrapper.orderByDesc(QuestionnaireSubmission::getCreateTime);
        submissionService.page(page, wrapper);

        // 设置问卷名称
        List<QuestionnaireSubmission> records = page.getRecords();
        setSubName(records);

        return Result.ok(PageResult.of(page));
    }

    /**
     * 用户端：提交问卷答案
     */
    @PostMapping("/submit")
    public Result<QuestionnaireSubmission> submitQuestionnaire(@RequestBody QuestionnaireSubmission submission) {
        // 生成提交编号
//        submission.setSubmissionCode("QS" + IdUtil.getSnowflakeNextIdStr());

        // 设置状态为已提交
        submission.setStatus(2);

        // 处理JSON格式的答案
        FieldProcessorUtil.convertToJsonArray(
                submission::getAnswers,
                submission::setAnswers
        );
        submission.setUserId(SecurityUtils.getCurrentRefId());
        submission.setUpdateTime(DateUtil.toLocalDateTime(DateUtil.date()));

        User user = userService.getById(SecurityUtils.getCurrentRefId());
        submission.setSubmissionName(user.getBrideName() + "&" + user.getBrideName() +"的问卷");

        submissionService.updateById(submission);
        return Result.ok(submission);
    }

    /**
     * 用户端：更新问卷答案
     */
    @PutMapping("/update")
    public Result<Void> updateQuestionnaire(@RequestBody QuestionnaireSubmission submission) {
        // 处理JSON格式的答案
        FieldProcessorUtil.convertToJsonArray(
                submission::getAnswers,
                submission::setAnswers
        );

        submissionService.updateById(submission);
        return Result.ok();
    }

    /**
     * 导出问卷为PDF
     */
    @GetMapping("/{id}/export-pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) throws Exception {
        byte[] pdfBytes = submissionService.exportToPdf(id);

        QuestionnaireSubmission submission = submissionService.getById(id);
        String fileName = "问卷_" + submission.getSubmissionCode() + ".pdf";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
