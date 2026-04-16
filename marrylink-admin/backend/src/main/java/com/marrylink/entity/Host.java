package com.marrylink.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("host")
public class Host {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String hostCode;
    private String name;
    private String stageName;
    private String phone;
    private String email;
    private String avatar;
    private BigDecimal price;
    private String serviceAreas;
    private Integer status;
    private BigDecimal rating;
    private Integer orderCount;
    private LocalDate joinTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;
    //标签类型
    @TableField(exist = false)
    private List<String> tags;
    private String description;
}
