package com.mmyf.commons.model.vo;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.mmyf.commons.exception.ResourceCreatedFailedException;
import com.mmyf.commons.model.vo.excel.ExportExcelParam;
import com.mmyf.commons.translate.annotation.DeepTranslate;
import com.mmyf.commons.util.SysUtils;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 通用分页查询参数
 *
 * @author teddy
 * @date 2024/02/01 17:12
 **/
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "通用分页查询参数")
@DeepTranslate
public class PageParam<T> {

    /** 分页参数 */
    @Schema(description = "分页参数")
    @Valid
    @NotNull(message = "分页参数不能为空")
    private Page page;

    /** 实体查询参数，正常所有查询参数都允许为空，如有特殊要求，请添加自己的校验分组 */
    @Schema(description = "实体查询参数")
    @Valid
    private T entityParam;

    /**
     * 导出参数
     */
    @Schema(description = "导出参数")
    private ExportExcelParam exportExcelParam;

    public void setPage(Page page) {
        this.page = page;
    }

    public Page getPage() {
        return this.page;
    }

    public void setEntityParam(T entityParam) {
        this.entityParam = entityParam;
    }

    public T getEntityParam(Class<T> clazz) {
        if (null == this.entityParam) {
            try {
                this.entityParam = clazz.newInstance();
            } catch (Exception e) {
                throw new ResourceCreatedFailedException(SysUtils.stringFormat("创建【{}】实例失败", clazz), e);
            }
        }
        return this.entityParam;
    }

    public ExportExcelParam getExportExcelParam() {
        return exportExcelParam;
    }

    public void setExportExcelParam(ExportExcelParam exportExcelParam) {
        this.exportExcelParam = exportExcelParam;
    }

    @Parameter(hidden = true)
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page getPageParam() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>();
        page.setCurrent(this.page.getCurrent());
        page.setSize(this.page.getSize());
        page.setOrders(this.page.getOrders());
        return page;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "分页查询参数")
    public static class Page {

        @Schema(description = "当前页", required = true)
        @NotNull(message = "当前页不能为空")
        private Long current;

        @Schema(description = "每页显示条数", required = true)
        @NotNull(message = "每页显示条数不能为空")
        private Long size;

        @Schema(description = "排序参数：column->排序字段，asc->是否升序")
        List<OrderItem> orders;
    }

    /**
     * 分页参数校验
     */
    public interface ValidateGroup {

    }
}
