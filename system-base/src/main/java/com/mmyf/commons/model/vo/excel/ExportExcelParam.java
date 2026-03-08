package com.mmyf.commons.model.vo.excel;

import com.mmyf.commons.translate.annotation.DeepTranslate;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ExportExcelParam
 *
 * @author fanzhongbo
 * @version 1.0.0
 * @description
 * @date
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Excel导出参数")
@DeepTranslate
public class ExportExcelParam {

    /**
     * 导出文件名称
     */
    @Schema(description = "导出文件名称，不传时使用系统默认值")
    private String fileName;

    /**
     * 导出文件标题
     */
    @Schema(description = "导出文件标题，不传时使用系统默认值")
    private String title;

    /**
     * 导出sheet名称
     */
    @Schema(description = "导出sheet名称，不传时使用系统默认值")
    private String sheetName;

    /**
     * 是否导出全部
     */
    @Schema(description = "是否导出全部，不传时默认导出当前页")
    private Boolean isAll;

    /**
     * 导出列参数
     */
    @Schema(description = "导出列参数，不传时默认所有列")
    private List<ExcelColumn> columns;
}
