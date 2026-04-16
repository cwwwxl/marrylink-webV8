package com.marrylink.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 主持人导入DTO
 * 注意：使用索引方式匹配列，确保Excel模板列顺序与此一致
 */
@Data
public class HostImportDTO {

    @ExcelProperty(value = "姓名")
    @ColumnWidth(15)
    private String name;

    @ExcelProperty(value = "艺名")
    @ColumnWidth(15)
    private String stageName;

    @ExcelProperty(value = "手机号")
    @ColumnWidth(15)
    private String phone;

    @ExcelProperty(value = "邮箱")
    @ColumnWidth(25)
    private String email;

    @ExcelProperty(value = "服务价格(元/场)")
    @ColumnWidth(15)
    private BigDecimal price;

    @ExcelProperty(value = "描述")
    @ColumnWidth(30)
    private String description;

    @ExcelProperty(value = "入驻时间(yyyy-MM-dd)")
    @ColumnWidth(20)
    private LocalDate joinTime;

}
