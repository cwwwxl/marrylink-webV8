package com.marrylink.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("host_tag")
public class HostTag implements Serializable {

    private static final long serialVersionUID = 1L;

    // ============== 数据库字段 ==============

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("host_id")
    private Long hostId;

    @TableField("tag_id")
    private Long tagId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // ============== 业务关联字段（不在数据库） ==============

    /**
     * 标签名称 - 关联查询用
     */
    private String tagCode;

    /**
     * 标签类型 - 关联查询用
     */
    @TableField(exist = false)
    private String tagType;

    /**
     * 标签值 - 关联查询用
     */
    @TableField(exist = false)
    private String tagValue;

    /**
     * 主持人姓名 - 关联查询用
     */
    @TableField(exist = false)
    private String hostName;
}
