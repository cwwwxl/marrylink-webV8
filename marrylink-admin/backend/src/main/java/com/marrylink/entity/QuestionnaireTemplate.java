package com.marrylink.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("questionnaire_template")
public class QuestionnaireTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String type;
    private Integer questionCount;
    private Integer useCount;
    private String content;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;

}
