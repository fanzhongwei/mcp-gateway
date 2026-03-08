package com.mmyf.commons.util.excel.converter;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.CellData;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * EasyExcelLocalTimeConverter
 *
 * @author fanzhongbo
 * @version 1.0.0
 * @description EasyExcel中涉及LocalDate时需要单独处理
 * @date 2024年2月16日
 */
public class EasyExcelLocalDateConverter implements Converter<LocalDate>{


    @Override
    public Class supportJavaTypeKey() {
        return LocalDate.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    @Override
    public LocalDate convertToJavaData(ReadCellData cellData, ExcelContentProperty excelContentProperty, GlobalConfiguration globalConfiguration) throws Exception {
        if (cellData.getType().equals(CellDataTypeEnum.NUMBER)) {
            LocalDate localDate = LocalDate.of(1900, 1, 1);
            //excel 有些奇怪的bug, 导致日期数差2
            localDate = localDate.plusDays(cellData.getNumberValue().longValue() - 2);
            return localDate;
        }
        return LocalDate.parse(cellData.getStringValue(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    @Override
    public WriteCellData<String> convertToExcelData(LocalDate LocalDate, ExcelContentProperty excelContentProperty, GlobalConfiguration globalConfiguration) throws Exception {
        return new WriteCellData<>(LocalDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }
}
