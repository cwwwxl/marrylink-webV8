package com.marrylink.controller;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.marrylink.common.PageResult;
import com.marrylink.common.Result;
import com.marrylink.entity.Tag;
import com.marrylink.entity.TagCategory;
import com.marrylink.service.IHostTagService;
import com.marrylink.service.ITagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/tag")
public class TagController {

    @Autowired
    private ITagService tagService;
    @Resource
    private IHostTagService hostTagService;


    @GetMapping("/page")
    public Result<PageResult<Tag>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) Integer status) {

        Page<Tag> page = new Page<>(current, size);
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();

        if (categoryCode != null) {
            wrapper.eq(Tag::getCategoryCode, categoryCode);
        }
        if (status != null) {
            wrapper.eq(Tag::getStatus, status);
        }

        wrapper.orderByDesc(Tag::getCreateTime);
        tagService.page(page, wrapper);

        return Result.ok(PageResult.of(page));
    }

    @GetMapping("/list")
    public Result<List<Tag>> list(@RequestParam(required = false) String categoryCode) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tag::getStatus, 1);
        if (categoryCode != null) {
            wrapper.eq(Tag::getCategoryCode, categoryCode);
        }
        return Result.ok(tagService.list(wrapper));
    }

    @GetMapping("/{id}")
    public Result<Tag> getById(@PathVariable Long id) {
        return Result.ok(tagService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Tag tag) {
        LambdaQueryWrapper<Tag> nameQuery = new LambdaQueryWrapper<>();
        nameQuery.eq(Tag::getName, tag.getName())
                .eq(Tag::getCategoryCode,tag.getCategoryCode());
        long nameCount = tagService.count(nameQuery);
        if (nameCount > 0) {
            return Result.error("标签名称已存在");
        }

        // 检查标签编码是否重复（排除自身）
        LambdaQueryWrapper<Tag> codeQuery = new LambdaQueryWrapper<>();
        codeQuery.eq(Tag::getCode, tag.getCode())
                .eq(Tag::getCategoryCode,tag.getCategoryCode());
        long codeCount = tagService.count(codeQuery);
        if (codeCount > 0) {
            return Result.error("标签编码已存在");
        }
        tagService.save(tag);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Tag tag) {
        LambdaQueryWrapper<Tag> nameQuery = new LambdaQueryWrapper<>();
        nameQuery.eq(Tag::getName, tag.getName())
                .eq(Tag::getCategoryCode,tag.getCategoryCode())
                .ne(Tag::getId, tag.getId()); // 排除当前记录
        long nameCount = tagService.count(nameQuery);
        if (nameCount > 0) {
            return Result.error("标签名称已存在");
        }

        // 检查标签编码是否重复（排除自身）
        LambdaQueryWrapper<Tag> codeQuery = new LambdaQueryWrapper<>();
        codeQuery.eq(Tag::getCode, tag.getCode())
                .eq(Tag::getCategoryCode,tag.getCategoryCode())
                .ne(Tag::getId, tag.getId()); // 排除当前记录
        long codeCount = tagService.count(codeQuery);
        if (codeCount > 0) {
            return Result.error("标签编码已存在");
        }
        tagService.updateById(tag);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.removeById(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setStatus(status);
        tagService.updateById(tag);
        return Result.ok();
    }
}
