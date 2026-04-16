package com.marrylink.service;

import com.marrylink.entity.Order;
import com.marrylink.entity.QuestionnaireSubmission;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author author
 * @since 2026-01-04
 */
public interface IQuestionnaireSubmissionService extends IService<QuestionnaireSubmission> {

    void createQS(Order order);

    void deleteQSByNo(Order order);

    byte[] exportToPdf(Long id);
}
