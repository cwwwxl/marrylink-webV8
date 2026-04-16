package com.marrylink.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("questionnaire_submission")
public class QuestionnaireSubmission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String submissionCode;
    //问卷名称
    @TableField(exist = false)
    private String submissionName;
    //问卷数量
    @TableField(exist = false)
    private Integer questionCount;
    private Long userId;
    private Long templateId;
    private Long hostId;
    private String answers;
    //1:待处理 2:已提交-待查看 3:已查看
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;
    private String orderNo;
}
