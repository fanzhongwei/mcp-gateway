package com.mmyf.commons.util.excel;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mmyf.commons.model.vo.PageParam;

/**
 * ExportExcelDataProvider
 *
 * @author fanzhongbo
 * @version 1.0.0
 * @description 导出Provider
 * @date 2024年2月16日
 */
public interface ExportExcelDataProvider<T,P> {

    Page<T> getPageData(PageParam<P> currentPageParam);
}
