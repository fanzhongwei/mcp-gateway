package com.mmyf.commons.advice;

import com.mmyf.commons.util.SysUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * package com.mmyf.commons.advice <br/>
 * description: TODO <br/>
 * Copyright 2019 mmyf, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022/5/30
 */
@Data
@NoArgsConstructor
public class ExceptionBody {

    private String code;

    private String message;

    /**
     * 堆栈信息，调试时候使用
     */
    private String stackInfo;

    public ExceptionBody(HttpStatus status, String message) {
        this.code = String.valueOf(status.value());
        this.message = message;
    }

    public ExceptionBody(HttpStatus status, String message, String stackInfo) {
        this.code = String.valueOf(status.value());
        this.message = message;
        this.stackInfo = stackInfo;
    }

    public ExceptionBody(HttpStatus status, Exception e) {
        this.code = String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value());
        this.message = e.getMessage();
        this.stackInfo = SysUtils.getStackTrace(e);
    }

}
