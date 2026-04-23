package com.marrylink.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("platform_account")
public class PlatformAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 账户余额（可提现） */
    private BigDecimal balance;
    /** 累计佣金收入 */
    private BigDecimal totalCommissionIncome;
    /** 累计提现 */
    private BigDecimal totalWithdrawn;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
