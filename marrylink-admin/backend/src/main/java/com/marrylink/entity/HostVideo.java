package com.marrylink.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("host_video")
public class HostVideo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long hostId;
    private String title;
    private String description;
    private String videoUrl;
    private String coverUrl;
    private Integer duration;
    private Long fileSize;
    private Integer status;
    private Integer showOnHome;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;
}
