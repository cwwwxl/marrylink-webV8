package com.marrylink.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("withdrawal_request")
public class WithdrawalRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String withdrawalNo;
    private Long hostId;
    private String hostName;
    private BigDecimal amount;
    private String accountType;
    private String accountNo;
    private String accountName;
    /** 1=待审核 2=已通过 3=已拒绝 4=已打款 */
    private Integer status;
    private String rejectReason;
    private LocalDateTime auditTime;
    private LocalDateTime payTime;
    private String operator;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;
}
