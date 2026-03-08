package com.mmyf.commons.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.mmyf.commons.constant.AbstractEnum;
import com.mmyf.commons.exception.ResourceReadFailedException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;

/**
 * package com.mmyf.commons.util <br/>
 * description: TODO <br/>
 * Copyright 2019 mmyf, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022/5/30
 */
@Slf4j
public class SysUtils {

    /**
     * 字符串格式化，参考slf4j: log.error("aa{}bb{}cc", 111,"222)
     *
     * @param s    格式化模板
     * @param args 参数
     * @return 格式化后的字符串
     */
    public static String stringFormat(String s, Object... args) {
        return MessageFormatter.arrayFormat(s, args)
                .getMessage();
    }

    public static String getStackTrace(Throwable e) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (PrintStream printStream = new PrintStream(outputStream, true, StandardCharsets.UTF_8.name())) {
            e.printStackTrace(printStream);
            return outputStream.toString(StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException unsupportedEncodingException) {
            log.error("提取堆栈失败！", e);
        }
        return StringUtils.EMPTY;
    }

}
