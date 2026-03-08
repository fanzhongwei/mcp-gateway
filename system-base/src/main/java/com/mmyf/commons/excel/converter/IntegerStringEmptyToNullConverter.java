package com.mmyf.commons.excel.converter;

import com.alibaba.excel.converters.integer.IntegerStringConverter;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import com.alibaba.excel.util.NumberUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;

/**
 *
 * @author mmyf
 * @date 2024/01/12 15:03
 **/
public class IntegerStringEmptyToNullConverter extends IntegerStringConverter {

    @Override
    public Integer convertToJavaData(ReadCellData cellData, ExcelContentProperty contentProperty,
                                     GlobalConfiguration globalConfiguration) throws ParseException {
        if (StringUtils.isBlank(cellData.getStringValue())) {
            return null;
        }
        return NumberUtils.parseInteger(cellData.getStringValue(), contentProperty);
    }
}
