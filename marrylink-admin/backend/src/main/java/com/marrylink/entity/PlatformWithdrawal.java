package com.marrylink.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("platform_withdrawal")
public class PlatformWithdrawal {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String withdrawalNo;
    private BigDecimal amount;
    private String accountType;
    private String accountNo;
    private String accountName;
    /** 1=待处理 2=已完成 */
    private Integer status;
    private String remark;
    private String operator;
    private LocalDateTime completeTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
