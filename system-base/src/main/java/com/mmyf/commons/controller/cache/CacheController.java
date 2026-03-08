package com.mmyf.commons.controller.cache;

import com.mmyf.commons.service.access.DwjkAccessCache;
import com.mmyf.commons.service.code.CodeCache;
import com.mmyf.commons.service.organ.OrganCache;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * 缓存刷新接口
 *
 * @author fanzhongwei
 * @date 2024/02/26 17:55
 **/
@RestController
@RequestMapping("/api/v1")
@Tag(name = "缓存", description = "system_base-系统缓存")
public class CacheController {

    @Resource
    private OrganCache organCache;

    @Resource
    private CodeCache codeCache;

    @Resource
    private DwjkAccessCache dwjkAccessCache;

    @Operation(summary = "缓存--刷新租户缓存")
    @GetMapping(value = "/organ/cache/reload")
    public ResponseEntity<Boolean> organCacheReload() {
        organCache.reload();
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @Operation(summary = "缓存--刷新码值缓存")
    @GetMapping(value = "/code/cache/reload")
    public ResponseEntity<Boolean> codeCacheReload() {
        codeCache.reload();
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    @Operation(summary = "缓存--刷新对外接口授权缓存")
    @GetMapping(value = "/dwjkAccess/cache/reload")
    public ResponseEntity<Boolean> dwjkAccessCacheReload() {
        dwjkAccessCache.reload();
        return new ResponseEntity<>(true, HttpStatus.OK);
    }
}
