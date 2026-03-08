package com.mmyf.commons.model.vo.excel;

import com.mmyf.commons.translate.annotation.DeepTranslate;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ExcelColumn
 *
 * @author fanzhongbo
 * @version 1.0.0
 * @description
 * @date
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Excel列参数")
@DeepTranslate
public class ExcelColumn {

    /**
     * 列名称
     */
    @Schema(description = "列名称")
    private String name;

    /**
     * 列标题
     */
    @Schema(description = "列标题")
    private String title;
}
