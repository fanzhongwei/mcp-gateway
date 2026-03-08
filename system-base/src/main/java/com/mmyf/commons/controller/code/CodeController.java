package com.mmyf.commons.controller.code;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mmyf.commons.constant.SystemCodeEnum;
import com.mmyf.commons.model.entity.code.Code;
import com.mmyf.commons.model.entity.code.CodeType;
import com.mmyf.commons.model.vo.PageParam;
import com.mmyf.commons.model.vo.ValidList;
import com.mmyf.commons.service.DbOperateService;
import com.mmyf.commons.service.code.CodeCache;
import com.mmyf.commons.service.code.CodeService;
import com.mmyf.commons.translate.annotation.DataTranslate;
import com.mmyf.commons.util.PinYinUtils;
import com.mmyf.commons.util.excel.ExportExcelUtils;
import com.mmyf.commons.util.mybatis.QueryWrapperUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 代码值 前端控制器
 * </p>
 *
 * @author Teddy
 * @since 2022-05-16
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "system_base-码值接口")
public class CodeController {

    @Autowired
    private CodeCache codeCache;

    @Autowired
    private CodeService codeService;

    @Autowired
    private DbOperateService dbOperateService;

    @Operation(summary = "代码值--根据代码类型获取代码集合")
    @GetMapping(value = "/code/list/{codeType}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Collection<Code>> getCodeList(@PathVariable("codeType") @Parameter(description = "代码类型", name = "codeType", required = true) String codeType) {
        return ResponseEntity.ok(codeCache.getCodeList(codeType));
    }

    @Operation(summary = "代码值--更新代码类型对应码值列表，代码值顺序同接口传入顺序")
    @PutMapping(value = "/code/list/{codeType}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> updateCodeList(@PathVariable("codeType") @Parameter(description = "代码类型", name = "codeType", required = true) String codeType,
                                                  @RequestBody @Validated ValidList<Code> codeList) {
        CodeType type = codeCache.getCodeType(codeType);
        Assert.notNull(type, "该代码类型不存在");
        Assert.isTrue(SystemCodeEnum.Sfxz.S.getCode().equals(type.getEditable()), "该代码类型暂不支持维护");
        Assert.isTrue(CollectionUtils.isNotEmpty(codeList), "代码列表不能为空");
        codeList.forEach(code -> Assert.isTrue(StringUtils.equals(codeType, code.getPid()), "代码值类型必须统一，请检查传入的代码类型"));
        for (int i = 0; i < codeList.size(); i++) {
            Code code = codeList.get(i);
            code.setNOrder(i + 1);
            if (StringUtils.isBlank(code.getValid())) {
                code.setValid(SystemCodeEnum.Sfxz.S.getCode());
            }
            if (StringUtils.isBlank(code.getDmjp())) {
                code.setDmjp(PinYinUtils.getFirstSpell(code.getName()));
            }
        }
        dbOperateService.executeInTransaction(() -> {
            codeService.remove(Wrappers.<Code>lambdaQuery().eq(Code::getPid, codeType));
            codeService.saveBatch(codeList);
        });
        codeCache.reload();
        return ResponseEntity.ok(true);
    }

    /**
     * 代码值--分页查询接口
     *
     * @param pageParam 分页查询参数
     * @return org.springframework.http.ResponseEntity<java.lang.Boolean>
     * @author mmyf
     * @date 2024-02-19
     **/
    @Operation(summary = "代码值--分页查询接口")
    @PostMapping(value = "/code/page", produces = MediaType.APPLICATION_JSON_VALUE)
    @DataTranslate
    public ResponseEntity<Page<Code>> selectPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<Code> pageParam) {
        QueryWrapper<Code> wrapper = Wrappers.query(pageParam.getEntityParam(Code.class));
        QueryWrapperUtils.appendOrderItem(pageParam, wrapper, Code.class);
        return new ResponseEntity<>(codeService.page(pageParam.getPageParam(), wrapper), HttpStatus.OK);
    }

    @Operation(summary = "代码值--分页查询支持排序的字段，理论上支持，但不建议对所有字段进行排序，性能差")
    @PostMapping(value = "/code/page/allowOrderColumns", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<String>> pageAllowOrderColumns() {
        return ResponseEntity.ok(QueryWrapperUtils.entityAllowOrderColumns(Code.class));
    }

    /**
     * 代码值--导出Excel接口
     *
     * @param pageParam 分页查询参数
     * @author mmyf
     * @date 2024-02-19
     **/
    @Operation(summary = "代码值--导出Excel")
    @PostMapping(value = "/code/page/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public void exportPage(@RequestBody @Validated(PageParam.ValidateGroup.class) PageParam<Code> pageParam, HttpServletRequest request, HttpServletResponse response) {
        ExportExcelUtils.doExport(pageParam, (currentPageParam) -> selectPage(currentPageParam).getBody(),request,response);
    }
}
