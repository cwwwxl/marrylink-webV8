package com.marrylink.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("settlement")
public class Settlement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String settlementNo;
    private Long orderId;
    private String orderNo;
    private Long hostId;
    private String hostName;
    private BigDecimal amount;
    private BigDecimal commissionAmount;
    private BigDecimal netAmount;
    /** 1=待结算 2=已结算 */
    private Integer status;
    private LocalDateTime settleTime;
    private String operator;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;
}
