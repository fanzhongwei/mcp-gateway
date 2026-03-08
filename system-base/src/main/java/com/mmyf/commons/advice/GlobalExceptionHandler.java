package com.mmyf.commons.advice;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import com.mmyf.commons.exception.OperationNotAllowedException;
import com.mmyf.commons.exception.ResourceNotFoundException;
import com.mmyf.commons.util.SysUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:prod}")
    private String profile;

    private static final String PROFILE_DEV = "dev";

    @InitBinder
    private void activateDirectFieldAccess(DataBinder dataBinder) {
        dataBinder.initDirectFieldAccess();
    }

    /**
     * 不允许操作，401
     */
    @ExceptionHandler({OperationNotAllowedException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ExceptionBody operatorForbiddenException(HttpServletRequest req, Exception e,
            HandlerMethod method) {
        log.error("---错误信息---method: {}, Host {} invokes url {} ERROR: {}", method.getMethod()
                        .getName(), req.getRemoteHost(), req.getRequestURL(),
                e.getMessage(), e);
        return new ExceptionBody(UNAUTHORIZED, e.getMessage(), getErrorMessage(e));
    }

    /**
     * 资源不存在，404
     */
    @ExceptionHandler({ResourceNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ExceptionBody resourceNotFoundException(HttpServletRequest req, Exception e,
                                                    HandlerMethod method) {
        log.error("---错误信息---method: {}, Host {} invokes url {} ERROR: {}", method.getMethod()
                        .getName(), req.getRemoteHost(), req.getRequestURL(),
                e.getMessage(), e);
        return new ExceptionBody(HttpStatus.NOT_FOUND, e.getMessage(), getErrorMessage(e));
    }

    private String getErrorMessage(Exception e) {
        if (StringUtils.equals(profile, PROFILE_DEV)) {
            return SysUtils.stringFormat("错误异常为：{}", SysUtils.getStackTrace(e));
        }
        return e.getMessage();
    }

    /**
     * 处理空指针的异常
     */
    @ExceptionHandler(value = NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ExceptionBody exceptionHandler(HttpServletRequest req, NullPointerException e, HandlerMethod method) {
        log.error("---发生空指针异常！---method: {}, Host {} invokes url {} ERROR: {}", method.getMethod()
                        .getName(), req.getRemoteHost(), req.getRequestURL(),
                e.getMessage(), e);
        return new ExceptionBody(INTERNAL_SERVER_ERROR, e.getMessage(), getErrorMessage(e));
    }

    /**
     * 处理自定义的业务异常
     */
    @ExceptionHandler({
            RuntimeException.class,
            Exception.class
    })
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public ExceptionBody exceptionHandler(HttpServletRequest req, Exception e, HandlerMethod method) {
        log.error("---发生业务异常！---method: {}, Host {} invokes url {} ERROR: {}", method.getMethod()
                        .getName(), req.getRemoteHost(), req.getRequestURL(),
                e.getMessage(), e);
        return new ExceptionBody(INTERNAL_SERVER_ERROR, e.getMessage(), getErrorMessage(e));
    }

    /**
     * 忽略参数异常处理器
     *
     * @param e 忽略参数异常
     * @return ResponseResult
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({AuthenticationException.class, IllegalArgumentException.class})
    public ExceptionBody badRequestExceptionHandler(HttpServletRequest req, Exception e, HandlerMethod method) {
        log.error("---发生请求参数异常！---method: {}, Host {} invokes url {} ERROR: {}", method.getMethod()
                                                                                               .getName(), req.getRemoteHost(), req.getRequestURL(),
                e.getMessage(), e);
        return new ExceptionBody(HttpStatus.BAD_REQUEST, e.getMessage(), getErrorMessage(e));
    }

    /**
     * 忽略参数异常处理器
     *
     * @param e 忽略参数异常
     * @return ResponseResult
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ExceptionBody parameterMissingExceptionHandler(HttpServletRequest req, MissingServletRequestParameterException e, HandlerMethod method) {
        log.error("---发生请求参数异常！---method: {}, Host {} invokes url {} ERROR: {}", method.getMethod()
                                                                                           .getName(), req.getRemoteHost(), req.getRequestURL(),
                e.getMessage(), e);
        return new ExceptionBody(HttpStatus.BAD_REQUEST, "请求参数 " + e.getParameterName() + " 不能为空", getErrorMessage(e));
    }

    /**
     * 缺少请求体异常处理器
     *
     * @param e 缺少请求体异常
     * @return ResponseResult
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ExceptionBody parameterBodyMissingExceptionHandler(HttpServletRequest req, HttpMessageNotReadableException e, HandlerMethod method) {
        log.error("---发生请求参数异常！---method: {}, Host {} invokes url {} ERROR: {}", method.getMethod()
                                                                                               .getName(), req.getRemoteHost(), req.getRequestURL(),
                e.getMessage(), e);
        return new ExceptionBody(HttpStatus.BAD_REQUEST, "请求体不能为空或者请求体格式错误", getErrorMessage(e));
    }

    /**
     * 参数效验异常处理器
     *
     * @param e 参数验证异常
     * @return ResponseInfo
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ExceptionBody parameterExceptionHandler(HttpServletRequest req, BindException e, HandlerMethod method) {
        log.error("---发生请求参数异常！---method: {}, Host {} invokes url {} ERROR: {}", method.getMethod()
                                                                                               .getName(), req.getRemoteHost(), req.getRequestURL(),
                e.getMessage(), e);
        // 获取异常信息
        BindingResult exceptions = e.getBindingResult();
        // 判断异常中是否有错误信息，如果存在就使用异常中的消息，否则使用默认消息

        if (exceptions.hasErrors()) {
            List<String> errorList = new ArrayList<>();
            List<ObjectError> errors = exceptions.getAllErrors();
            if (CollectionUtils.isNotEmpty(errors)) {
                // 这里列出了全部错误参数，按正常逻辑，只需要第一条错误即可
                errors.forEach(fieldError -> {
                    errorList.add(SysUtils.stringFormat("{} -> {}", ((FieldError) fieldError).getField(), fieldError.getDefaultMessage()));

                });
                return new ExceptionBody(HttpStatus.BAD_REQUEST, StringUtils.join(errorList, "\n "), getErrorMessage(e));
            }
        }
        return new ExceptionBody(HttpStatus.BAD_REQUEST, "参数校验错误", getErrorMessage(e));
    }
}
