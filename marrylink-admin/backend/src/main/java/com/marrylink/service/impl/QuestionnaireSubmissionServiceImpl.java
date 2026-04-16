package com.marrylink.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.marrylink.entity.Order;
import com.marrylink.entity.QuestionnaireSubmission;
import com.marrylink.entity.QuestionnaireTemplate;
import com.marrylink.mapper.QuestionnaireSubmissionMapper;
import com.marrylink.service.IQuestionnaireSubmissionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.marrylink.service.IQuestionnaireTemplateService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author author
 * @since 2026-01-04
 */
@Service
public class QuestionnaireSubmissionServiceImpl extends ServiceImpl<QuestionnaireSubmissionMapper, QuestionnaireSubmission> implements IQuestionnaireSubmissionService {

    @Resource
    private IQuestionnaireTemplateService qtService;

    @Override
    public void createQS(Order order) {
        //如果有就不创建
        String orderNo = order.getOrderNo();
        LambdaQueryWrapper<QuestionnaireSubmission> noWrapper = new LambdaQueryWrapper<>();
        noWrapper.eq(QuestionnaireSubmission::getOrderNo,orderNo);
        List<QuestionnaireSubmission> listByNo = list(noWrapper);
        if (CollUtil.isNotEmpty(listByNo)){
            return;
        }

        QuestionnaireSubmission submission = new QuestionnaireSubmission();
        submission.setUserId(order.getUserId());
        submission.setHostId(order.getHostId());
        submission.setOrderNo(order.getOrderNo());
        submission.setSubmissionCode("QS" + IdUtil.getSnowflakeNextIdStr());
        LambdaQueryWrapper<QuestionnaireTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionnaireTemplate::getType,order.getWeddingType());
        List<QuestionnaireTemplate> list = qtService.list(wrapper);
        QuestionnaireTemplate template = list.get(0);
        submission.setTemplateId(template.getId());
        save(submission);
    }

    @Override
    public void deleteQSByNo(Order order) {
        LambdaQueryWrapper<QuestionnaireSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionnaireSubmission::getOrderNo,order.getOrderNo());
        remove(wrapper);
    }

    @Override
    public byte[] exportToPdf(Long id) {
        QuestionnaireSubmission submission = getById(id);
        if (submission == null) {
            throw new RuntimeException("问卷提交记录不存在");
        }

        QuestionnaireTemplate template = qtService.getById(submission.getTemplateId());
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            
            // 使用中文字体
            PdfFont font = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H");
            document.setFont(font);
            
            // 标题
            DeviceRgb blueColor = new DeviceRgb(29, 78, 216);
            Paragraph title = new Paragraph("婚礼问卷答案")
                    .setFontSize(20)
                    .setBold()
                    .setFontColor(blueColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(title);
            
            // 基本信息表格
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
                    .useAllAvailableWidth()
                    .setMarginBottom(20);
            
            addInfoRow(infoTable, "提交编号", submission.getSubmissionCode(), font);
            addInfoRow(infoTable, "模板名称", template.getName(), font);
            addInfoRow(infoTable, "提交时间", submission.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), font);
            
            document.add(infoTable);
            
            // 问卷答案
            Paragraph answerTitle = new Paragraph("问卷答案")
                    .setFontSize(16)
                    .setBold()
                    .setFontColor(blueColor)
                    .setMarginBottom(10);
            document.add(answerTitle);
            
            // 解析答案JSON
            JSONObject answersJson = JSONUtil.parseObj(submission.getAnswers());
            int index = 1;
            
            for (Map.Entry<String, Object> entry : answersJson.entrySet()) {
                JSONObject item = JSONUtil.parseObj(entry.getValue());
                String question = item.getStr("question", "未知问题");
                Object answerObj = item.get("answer");
                String answer = answerObj instanceof List ?
                        String.join("、", (List<String>) answerObj) :
                        String.valueOf(answerObj);
                
                // 问题
                Paragraph questionPara = new Paragraph(index + ". " + question)
                        .setFontSize(12)
                        .setBold()
                        .setMarginTop(10)
                        .setMarginBottom(5);
                document.add(questionPara);
                
                // 答案
                Paragraph answerPara = new Paragraph(answer)
                        .setFontSize(11)
                        .setMarginLeft(20)
                        .setMarginBottom(10);
                document.add(answerPara);
                
                index++;
            }
            
            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("生成PDF失败: " + e.getMessage(), e);
        }
    }
    
    private void addInfoRow(Table table, String label, String value, PdfFont font) {
        DeviceRgb lightBlue = new DeviceRgb(245, 247, 250);
        table.addCell(new Cell()
                .add(new Paragraph(label).setFont(font).setBold())
                .setBackgroundColor(lightBlue)
                .setPadding(8));
        table.addCell(new Cell()
                .add(new Paragraph(value).setFont(font))
                .setPadding(8));
    }
}
