package com.marrylink.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("platform_escrow")
public class PlatformEscrow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String orderNo;
    private BigDecimal amount;
    /** 订单全额（用于佣金计算） */
    private BigDecimal totalOrderAmount;
    /** 1=托管中 2=已结算 3=已退款 */
    private Integer status;
    private LocalDateTime payTime;
    private LocalDateTime settleTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;
}
