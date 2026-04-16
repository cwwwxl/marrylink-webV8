package com.marrylink.listener;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.marrylink.dto.HostImportDTO;
import com.marrylink.entity.Host;
import com.marrylink.service.IHostService;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 主持人Excel导入监听器
 */
@Slf4j
public class HostImportListener extends AnalysisEventListener<HostImportDTO> {

    /**
     * 每隔100条存储数据库，然后清理list，方便内存回收
     */
    private static final int BATCH_COUNT = 100;

    /**
     * 缓存的数据
     */
    private List<Host> cachedDataList = new ArrayList<>(BATCH_COUNT);

    /**
     * 主持人服务
     */
    private final IHostService hostService;

    /**
     * 错误信息
     */
    private final List<String> errorMessages = new ArrayList<>();

    /**
     * 成功导入数量
     */
    private int successCount = 0;

    /**
     * 手机号正则
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /**
     * 日期格式化
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public HostImportListener(IHostService hostService) {
        this.hostService = hostService;
    }

    @Override
    public void invoke(HostImportDTO data, AnalysisContext context) {
        int rowIndex = context.readRowHolder().getRowIndex() + 1;
        log.info("解析第{}行数据: name={}, phone={}, stageName={}", rowIndex, data.getName(), data.getPhone(), data.getStageName());

        // 数据校验
//        String error = validateData(data, rowIndex);
//        if (StrUtil.isNotBlank(error)) {
//            log.warn("第{}行数据校验失败: {}", rowIndex, error);
//            errorMessages.add(error);
//            return;
//        }

        // 转换为实体
        Host host = convertToEntity(data);
        cachedDataList.add(host);

        // 达到BATCH_COUNT了，需要去存储一次数据库，防止数据几万条数据在内存，容易OOM
        if (cachedDataList.size() >= BATCH_COUNT) {
            saveData();
            // 存储完成清理 list
            cachedDataList.clear();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 这里也要保存数据，确保最后遗留的数据也存储到数据库
        if (!cachedDataList.isEmpty()) {
            saveData();
        }
        log.info("所有数据解析完成！成功导入: {} 条", successCount);
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) {
        log.error("解析失败，但是继续解析下一行: {}", exception.getMessage());
        if (exception instanceof ExcelDataConvertException) {
            ExcelDataConvertException excelDataConvertException = (ExcelDataConvertException) exception;
            int rowIndex = excelDataConvertException.getRowIndex() + 1;
            int columnIndex = excelDataConvertException.getColumnIndex() + 1;
            errorMessages.add(String.format("第%d行，第%d列数据格式错误", rowIndex, columnIndex));
        }
    }


    /**
     * 转换为实体
     */
    private Host convertToEntity(HostImportDTO data) {
        Host host = new Host();
        host.setName(data.getName());
        host.setStageName(data.getStageName());
        host.setPhone(data.getPhone());
        host.setEmail(data.getEmail());
        host.setPrice(data.getPrice());
        host.setDescription(data.getDescription());
        host.setOrderCount(0);

        // 解析入驻时间
        if (data.getJoinTime() != null) {
            host.setJoinTime(data.getJoinTime());
//            host.setJoinTime(LocalDate.parse(data.getJoinTime(), DATE_FORMATTER));
        } else {
            host.setJoinTime(LocalDate.now());
        }

        return host;
    }

    /**
     * 保存数据
     */
    private void saveData() {
        log.info("开始存储数据，数量: {}", cachedDataList.size());
        for (Host host : cachedDataList) {
            try {
                hostService.saveHost(host);
                successCount++;
            } catch (Exception e) {
                log.error("保存主持人失败: {}", e.getMessage());
                errorMessages.add("保存主持人[" + host.getName() + "]失败: " + e.getMessage());
            }
        }
        log.info("存储数据完成！");
    }

    /**
     * 获取错误信息
     */
    public List<String> getErrorMessages() {
        return errorMessages;
    }

    /**
     * 获取成功导入数量
     */
    public int getSuccessCount() {
        return successCount;
    }

    /**
     * 是否有错误
     */
    public boolean hasErrors() {
        return !errorMessages.isEmpty();
    }
}
