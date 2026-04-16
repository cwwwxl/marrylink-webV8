package com.marrylink.controller;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.marrylink.common.PageResult;
import com.marrylink.common.Result;
import com.marrylink.entity.Tag;
import com.marrylink.entity.TagCategory;
import com.marrylink.service.ITagCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tag-category")
public class TagCategoryController {

    @Autowired
    private ITagCategoryService tagCategoryService;

    @GetMapping("/page")
    public Result<PageResult<TagCategory>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status) {

        Page<TagCategory> page = new Page<>(current, size);
        LambdaQueryWrapper<TagCategory> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(TagCategory::getStatus, status);
        }

        wrapper.orderByDesc(TagCategory::getCreateTime);
        tagCategoryService.page(page, wrapper);

        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/list")
    public Result<List<TagCategory>> list() {
        LambdaQueryWrapper<TagCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TagCategory::getStatus, 1);
        return Result.ok(tagCategoryService.list(wrapper));
    }

    @GetMapping("/{id}")
    public Result<TagCategory> getById(@PathVariable Long id) {
        return Result.ok(tagCategoryService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody TagCategory tagCategory) {
        LambdaQueryWrapper<TagCategory> nameQuery = new LambdaQueryWrapper<>();
        nameQuery.eq(TagCategory::getName, tagCategory.getName());
        long nameCount = tagCategoryService.count(nameQuery);
        if (nameCount > 0) {
            return Result.error("标签名称已存在");
        }

        // 检查标签编码是否重复（排除自身）
        LambdaQueryWrapper<TagCategory> codeQuery = new LambdaQueryWrapper<>();
        codeQuery.eq(TagCategory::getCode, tagCategory.getCode());
        long codeCount = tagCategoryService.count(codeQuery);
        if (codeCount > 0) {
            return Result.error("标签编码已存在");
        }
        tagCategoryService.save(tagCategory);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody TagCategory tagCategory) {
        LambdaQueryWrapper<TagCategory> nameQuery = new LambdaQueryWrapper<>();
        nameQuery.eq(TagCategory::getName, tagCategory.getName())
                .ne(TagCategory::getId, tagCategory.getId()); // 排除当前记录
        long nameCount = tagCategoryService.count(nameQuery);
        if (nameCount > 0) {
            return Result.error("标签名称已存在");
        }

        // 检查标签编码是否重复（排除自身）
        LambdaQueryWrapper<TagCategory> codeQuery = new LambdaQueryWrapper<>();
        codeQuery.eq(TagCategory::getCode, tagCategory.getCode())
                .ne(TagCategory::getId, tagCategory.getId()); // 排除当前记录
        long codeCount = tagCategoryService.count(codeQuery);
        if (codeCount > 0) {
            return Result.error("标签编码已存在");
        }
        tagCategoryService.updateById(tagCategory);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagCategoryService.removeById(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        TagCategory tagCategory = new TagCategory();
        tagCategory.setId(id);
        tagCategory.setStatus(status);
        tagCategoryService.updateById(tagCategory);
        return Result.ok();
    }
}
