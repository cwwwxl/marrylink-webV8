package com.marrylink.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.marrylink.common.PageResult;
import com.marrylink.common.Result;
import com.marrylink.entity.QuestionnaireTemplate;
import com.marrylink.service.IQuestionnaireTemplateService;
import com.marrylink.utils.FieldProcessorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/questionnaire-template")
public class QuestionnaireTemplateController {

    @Autowired
    private IQuestionnaireTemplateService templateService;

    @GetMapping("/page")
    public Result<PageResult<QuestionnaireTemplate>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status) {

        Page<QuestionnaireTemplate> page = new Page<>(current, size);
        LambdaQueryWrapper<QuestionnaireTemplate> wrapper = new LambdaQueryWrapper<>();

        if (type != null && !type.isEmpty()) {
            wrapper.eq(QuestionnaireTemplate::getType, type);
        }
        if (status != null) {
            wrapper.eq(QuestionnaireTemplate::getStatus, status);
        }

        wrapper.orderByDesc(QuestionnaireTemplate::getCreateTime);
        templateService.page(page, wrapper);

        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/list")
    public Result<List<QuestionnaireTemplate>> list() {
        LambdaQueryWrapper<QuestionnaireTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionnaireTemplate::getStatus, 1);
        return Result.ok(templateService.list(wrapper));
    }

    @GetMapping("/{id}")
    public Result<QuestionnaireTemplate> getById(@PathVariable Long id) {
        return Result.ok(templateService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody QuestionnaireTemplate template) {
        FieldProcessorUtil.convertToJsonArray(
                template::getContent,
                template::setContent
        );
        templateService.save(template);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody QuestionnaireTemplate template) {
        FieldProcessorUtil.convertToJsonArray(
                template::getContent,
                template::setContent
        );
        templateService.updateById(template);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        templateService.removeById(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        QuestionnaireTemplate template = new QuestionnaireTemplate();
        template.setId(id);
        template.setStatus(status);
        templateService.updateById(template);
        return Result.ok();
    }
}
