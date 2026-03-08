package com.mmyf.commons.util.excel;

import cn.hutool.core.util.ClassUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.style.column.SimpleColumnWidthStyleStrategy;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.BeanUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mmyf.commons.exception.ResourceCreatedFailedException;
import com.mmyf.commons.model.entity.IEntity;
import com.mmyf.commons.model.vo.PageParam;
import com.mmyf.commons.model.vo.excel.ExcelColumn;
import com.mmyf.commons.model.vo.excel.ExportExcelParam;
import com.mmyf.commons.translate.service.DataTranslateService;
import com.mmyf.commons.util.excel.converter.EasyExcelLocalDateConverter;
import com.mmyf.commons.util.excel.converter.EasyExcelLocalDateTimeConverter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ExportExcelUtls
 *
 * @author fanzhongbo
 * @version 1.0.0
 * @description 公用导出工具类
 * @date 2024年2月7日
 */
@Slf4j
@Component
public class ExportExcelUtils {

    public static final String DEFAULT_FILE_NAME = "导出";

    public static final String EXPORT_FILE_SUFFIX = ".xlsx";

    public static final int DEFAULT_PAGE_SIZE = 1000;

    @Autowired
    private DataTranslateService dataTranslateService;
    private static DataTranslateService dataTranslate;

    @PostConstruct
    public void init() {
        if (dataTranslate == null) {
            setDateSource(dataTranslateService);
        }
    }

    private synchronized static void setDateSource(DataTranslateService dataTranslateService) {
        dataTranslate = dataTranslateService;
    }

    /**
     * description 导出
     * @param pageParam 查询参数
     * @param provider
     * @param request
     * @param response
     * @return void
     * @author fanzhongbo
     * @date 2024/2/15 17:06
     * @version 1.0
    **/
    public static <T, P> void doExport(PageParam<P> pageParam, ExportExcelDataProvider<T,P> provider,HttpServletRequest request, HttpServletResponse response) {
        try {
            exportExcelFile(pageParam.getExportExcelParam(),selectPage(pageParam,provider),request,response);
        } catch (Exception e) {
            throw new ResourceCreatedFailedException("创建导出excel文件失败", e);
        }
    }

    /**
     * description 数据查询
     * @param pageParam 查询参数
     * @param provider
     * @return java.util.List<T>
     * @author fanzhongbo
     * @date 2024/2/15 17:05
     * @version 1.0
    **/
    public static <T,P> List<T> selectPage(PageParam<P> pageParam, ExportExcelDataProvider<T, P> provider){
        List<T> dataList = new ArrayList<>();
        // 如果是导出全部则设置size为1000，将所有数据查询后导出，否则根据pageParam直接导出
        if(ObjectUtils.isNotEmpty(pageParam.getExportExcelParam()) && BooleanUtils.isTrue(pageParam.getExportExcelParam().getIsAll())){
            pageParam.getPage().setCurrent(1L);
            pageParam.getPage().setSize((long) DEFAULT_PAGE_SIZE);
            List<OrderItem> orders = pageParam.getPage().getOrders();
            Page<T> pageData = provider.getPageData(pageParam);
            dataList.addAll(pageData.getRecords());
            int totalPages = (int) Math.ceil((double)pageData.getTotal() / DEFAULT_PAGE_SIZE);
            for(int i = 1; i < totalPages; i++) {
                // 一次最多导出100000数据
                if(i > 100){
                    break;
                }
                pageParam.getPage().setCurrent((long) (i+1));
                pageParam.getPage().setOrders(orders);
                dataList.addAll(provider.getPageData(pageParam).getRecords());
            }
        }else {
            dataList.addAll(provider.getPageData(pageParam).getRecords());
        }
        return dataList;
    }
    /**
     * description 公用导出方法
     * @param exportExcelParam 导出参数
     * @param dataList 数据
     * @param request
     * @param response
     * @return void
     * @author fanzhongbo
     * @date 2024/2/7 23:14
     * @version 1.0
    **/
    public static void exportExcelFile(ExportExcelParam exportExcelParam, List dataList, HttpServletRequest request, HttpServletResponse response) throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        List<List<String>> allHeadList = new ArrayList<>();
        List<String> includeColumnFiledNameList = new ArrayList<>();
        // 如果传入的文件名称、sheet页名称、标题行内容，否则使用传入数据dataList对象ApiModel的value
        String defaultName = buildFileFileName(dataList);
        if(ObjectUtils.isEmpty(exportExcelParam)){
            exportExcelParam = new ExportExcelParam();
        }
        if(StringUtils.isBlank(exportExcelParam.getFileName())){
            exportExcelParam.setFileName(defaultName);
        }
        if(StringUtils.isBlank(exportExcelParam.getTitle())){
            exportExcelParam.setTitle(defaultName);
        }
        if(StringUtils.isBlank(exportExcelParam.getSheetName())){
            exportExcelParam.setSheetName(defaultName);
        }

        // 设置响应头
        setResponseHeader(request, response, exportExcelParam.getFileName() + EXPORT_FILE_SUFFIX);
        // 如果导出数据为空则导出空Excel
        if(CollectionUtils.isEmpty(dataList)){
            EasyExcel.write(response.getOutputStream(),Object.class)
                    .sheet(exportExcelParam.getSheetName())
                    .doWrite(new ArrayList<>());
            return;
        }
        // 如果设置了导出列则只导出指定的列，否则导出全部
        ExportExcelParam finalExportExcelParam = exportExcelParam;
        // 设置序号列
        List<String> indexHead = new ArrayList<>();
        // 设置表头，每列都设置filename，相同名字的表头EasyExcel会自动合并
        indexHead.add(finalExportExcelParam.getTitle());
        indexHead.add("序号");
        allHeadList.add(indexHead);
        if(CollectionUtils.isNotEmpty(exportExcelParam.getColumns())){
            exportExcelParam.getColumns().stream().map(ExcelColumn::getTitle).forEach(s -> {
                List<String> headList = new ArrayList<>();
                // 设置表头，每列都设置filename，相同名字的表头EasyExcel会自动合并
                headList.add(finalExportExcelParam.getTitle());
                headList.add(s);
                allHeadList.add(headList);
            });
            // 获取需要导出的列
            includeColumnFiledNameList = exportExcelParam.getColumns().stream().map(ExcelColumn::getName).collect(Collectors.toList());
            EasyExcel.write(response.getOutputStream(),dataList.get(0).getClass())
                    //插入数据
                    .sheet(exportExcelParam.getSheetName())
                    .head(allHeadList)
                    .includeColumnFiledNames(includeColumnFiledNameList)
                    .registerConverter(new EasyExcelLocalDateTimeConverter())
                    .registerConverter(new EasyExcelLocalDateConverter())
                    .registerWriteHandler(new SimpleColumnWidthStyleStrategy(20)).registerWriteHandler(setHorizontalCellStyleStrategy())
                    .doWrite(getExportListDatas(dataList,includeColumnFiledNameList));
        }else {
            // 根据注解获取表头名称
            LinkedHashMap<String, String> headMap = resolveExcelTableName(dataList.get(0));
            headMap.values().stream().forEach(s -> {
                List<String> rowHeadList = new ArrayList<>();
                rowHeadList.add(finalExportExcelParam.getTitle());
                rowHeadList.add(s);
                allHeadList.add(rowHeadList);
            });
            includeColumnFiledNameList = new ArrayList<>(headMap.keySet());
            EasyExcel.write(response.getOutputStream(),dataList.get(0).getClass())
                    //插入数据
                    .sheet(exportExcelParam.getSheetName())
                    .head(allHeadList)
                    .registerConverter(new EasyExcelLocalDateTimeConverter())
                    .registerConverter(new EasyExcelLocalDateConverter())
                    .registerWriteHandler(new SimpleColumnWidthStyleStrategy(20)).registerWriteHandler(setHorizontalCellStyleStrategy())
                    .doWrite(getExportListDatas(dataList,includeColumnFiledNameList));
        }


    }

    /**
     * description 将数据按照传入的includeColumns调整数据顺序
     * @param list 导出数据
     * @param includeColumns 需要导出的列
     * @return java.util.List<java.util.List<java.lang.Object>>
     * @author fanzhongbo
     * @date 2024/2/8 18:11
     * @version 1.0
    **/
    public static List<List<Object>> getExportListDatas(List list, List<String> includeColumns) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        List<List<Object>> listData = new ArrayList<>();
        if (CollectionUtils.isEmpty(list)) {
            return listData;
        }
        int index = 1;
        for (Object t : list) {
            JSONObject translateJson = JSON.parseObject(dataTranslate.translateValue(t).toString());
            Map<String, Object> beanFieldMap = BeanUtils.beanToMap(t);
            List<Object> rowLine = new ArrayList<>();
            // 设置序号
            rowLine.add(index);
            index++;
            for(String s : includeColumns) {
                // 获取原javabean对象字段，如果有翻译值优先取翻译字段值
                String translateValue = translateJson.getString(s + DataTranslateService.TranslatePropSuffix);
                Object value = beanFieldMap.get(s);
                if (null != value && !ClassUtil.isSimpleTypeOrArray(value.getClass())) {
                    value = translateJson.get(s).toString();
                }
                rowLine.add(ObjectUtils.defaultIfNull(translateValue, value));
            }
            listData.add(rowLine);
        }
        return listData;
    }

    /**
     * description 设置表头和内容行的样式
     * @param
     * @return com.alibaba.excel.write.style.HorizontalCellStyleStrategy
     * @author fanzhongbo
     * @date 2024/2/7 18:36
     * @version 1.0
    **/
    public static HorizontalCellStyleStrategy setHorizontalCellStyleStrategy(){
        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        contentWriteCellStyle.setBorderLeft(BorderStyle.THIN);
        contentWriteCellStyle.setBorderTop(BorderStyle.THIN);
        contentWriteCellStyle.setBorderRight(BorderStyle.THIN);
        contentWriteCellStyle.setBorderBottom(BorderStyle.THIN);
        contentWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        headWriteCellStyle.setBorderLeft(BorderStyle.THIN);
        headWriteCellStyle.setBorderTop(BorderStyle.THIN);
        headWriteCellStyle.setBorderRight(BorderStyle.THIN);
        headWriteCellStyle.setBorderBottom(BorderStyle.THIN);
        //设置头部标题居中
        headWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        // 设置表头样式和内容样式互不影响
        return new HorizontalCellStyleStrategy(headWriteCellStyle, contentWriteCellStyle);
    }
    /**
     * description 构建文件名称，如果需导出的dataList是空，则文件名称为默认：导出，否则取ApiModel的value为文件名称
     * @param dataList 导出数据
     * @return java.lang.String
     * @author fanzhongbo
     * @date 2024/2/7 17:30
     * @version 1.0
    **/
    public static String buildFileFileName(List dataList) {
        if(CollectionUtils.isEmpty(dataList)){
            return DEFAULT_FILE_NAME;
        }
        Schema apiModel = dataList.get(0).getClass().getAnnotation(Schema.class);
        if(ObjectUtils.isNotEmpty(apiModel) && StringUtils.isNotBlank(apiModel.name())){
            return apiModel.name();
        }
        return DEFAULT_FILE_NAME;
    }

    /**
     * description 根据注解获取列名
     * @param entity 文件内容
     * @return java.util.List<java.lang.String>
     * @author fanzhongbo
     * @date 2024/2/7 15:13
     * @version 1.0
     **/
    public static <T> LinkedHashMap<String, String> resolveExcelTableName(T entity) {
        LinkedHashMap<String, String> tableNamesMap = new LinkedHashMap<>();
        Class<? extends Object> entityClass = entity.getClass();
        List<Field> fields = new ArrayList<>();
        // 获取所有属性，包括父类
        ReflectionUtils.doWithFields(entityClass, fields::add);
        for (Field field : fields) {
            try {
                if (!"serialVersionUID".equals(field.getName())) {
                    String fieldName = field.getName();
                    String tableTitleName = field.getName();
                    Schema apiModelProperty = field.getAnnotation(Schema.class);
                    if (null == apiModelProperty) {
                        continue;
                    }
                    String annName = apiModelProperty.name();
                    if (StringUtils.isNotBlank(annName)) {
                        tableTitleName = annName;
                    }
                    tableNamesMap.put(fieldName, tableTitleName);
                }
            } catch (Exception e) {
                log.warn("解析导出excel默认表头名称失败", e);
            }
        }
        return tableNamesMap;
    }

    /**
     * description 设置响应头
     * @param response 响应
     * @param request 请求
     * @param filename 文件名
     * @return void
     * @author fanzhongbo
     * @date 2024/2/7 17:24
     * @version 1.0
    **/
    public static void setResponseHeader(HttpServletRequest request, HttpServletResponse response, String filename)
            throws UnsupportedEncodingException {
        response.setContentType("application/octet-stream");
        String name = new String(filename.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
        // 设置文件头
        response.setHeader("Content-Disposition", "attachment;fileName=" + name);
    }
}
