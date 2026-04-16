package com.marrylink.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("order_log")
public class OrderLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Integer oldStatus;
    private Integer newStatus;
    private String operator;
    private String operatorIp;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}