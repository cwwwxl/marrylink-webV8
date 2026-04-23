package com.marrylink.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("commission_order")
public class CommissionOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String commissionNo;
    private Long orderId;
    private String orderNo;
    private Long hostId;
    private String hostName;
    private BigDecimal orderAmount;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    /** 1=待支付 2=已支付 3=逾期 */
    private Integer status;
    private LocalDateTime deadline;
    private LocalDateTime payTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;
}
