package com.marrylink.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("`order`")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private String userName;
    private Long hostId;
    private String hostName;
    private LocalDate weddingDate;
    private String weddingType;
    private BigDecimal amount;
    /**
     * 定金金额（订单总额的30%）
     */
    private BigDecimal depositAmount;
    //1: '待确认', 3: '定金已付', 4: '已完成', 5: '已取消'
    private Integer status;
    /** 用户评分 1-5星，null表示未评分 */
    private Integer rating;
    /** 用户评价内容 */
    private String comment;
    /**
     * 支付状态 0=未支付 1=已支付 2=已退款
     */
    private Integer paymentStatus;
    /**
     * 支付时间
     */
    private LocalDateTime payTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;
}
