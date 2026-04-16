package com.marrylink.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 通用字段处理工具类
 */
public class FieldProcessorUtil {

    /**
     * 将逗号分隔的字符串转换为JSON数组格式
     *
     * @param getter 字段获取函数
     * @param setter 字段设置函数
     * @param <T> 对象类型
     */
    public static <T> void convertToJsonArray(
            Supplier<String> getter,
            Consumer<String> setter) {

        String value = getter.get();

        // 如果为空，设置为null
        if (StrUtil.isBlank(value)) {
            setter.accept(null);
            return;
        }

        // 如果不是JSON格式，则转换为JSON数组
        if (!JSONUtil.isTypeJSON(value)) {
            setter.accept(JSONUtil.toJsonStr(StrUtil.split(value, ',')));
        }
    }
}

