package com.mmyf.commons.log;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.mmyf.commons.model.entity.access.DwjkAccess;
import com.mmyf.commons.model.entity.log.ApiLog;
import com.mmyf.commons.model.entity.organ.User;
import com.mmyf.commons.service.log.ApiLogService;
import com.mmyf.commons.shiro.security.SecurityService;
import com.mmyf.commons.util.http.RequestUtils;
import com.mmyf.commons.util.SysUtils;
import com.mmyf.commons.util.uuid.UUIDHelper;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * package com.mmyf.commons.log <br/>
 * description: 切面处理类，操作日志异常日志记录处理 <br/>
 * Copyright 2019 mmyf, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022/5/30
 */
@Aspect
@Component
@Slf4j
public class ApiLogAspect {

    /**
     * 统计请求的处理时间
     */
    ThreadLocal<LocalDateTime> startTime = new ThreadLocal<>();
    ThreadLocal<Long> start = new ThreadLocal<>();

    @Autowired
    private ApiLogService apiLogService;

    /**
     * 设置操作日志切入点 记录操作日志 在注解的位置切入代码
     */
    @Pointcut("@annotation(com.mmyf.commons.log.ApiLog)")
    public void logPointCut() {
    }

    @Before("logPointCut()")
    public void doBefore() {
        // 接收到请求，记录请求开始时间
        startTime.set(LocalDateTime.now());
        start.set(System.currentTimeMillis());
    }

    /**
     *
     * @param joinPoint
     * @param keys
     */
    @AfterReturning(value = "logPointCut()", returning = "keys")
    public void doAfterReturning(JoinPoint joinPoint, Object keys) {
        // 获取RequestAttributes
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        // 从获取RequestAttributes中获取HttpServletRequest的信息
        HttpServletRequest request = (HttpServletRequest) requestAttributes.resolveReference(RequestAttributes.REFERENCE_REQUEST);

        ApiLog logInfo = new ApiLog();
        try {
            // 从切面织入点处通过反射机制获取织入点处的方法
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();

            // 获取切入点所在的方法
            Method method = signature.getMethod();

            // 获取操作
            com.mmyf.commons.log.ApiLog log = method.getAnnotation(com.mmyf.commons.log.ApiLog.class);
            if (Objects.nonNull(log)) {
                logInfo.setModule(log.module());
                logInfo.setApiName(log.name());
            }

            logInfo.setId(UUIDHelper.getUuid());

            // 请求的方法名
            // 请求参数
            logInfo.setRequest(method.getName() + ": " + JSON.toJSONString(getParams(signature, joinPoint)));
            // 返回结果
            logInfo.setResponse(JSON.toJSONString(keys));

            setOperatorInfo(logInfo);

            // 请求IP
            logInfo.setIp(RequestUtils.getRemoteIp(request));
            // 请求URI
            logInfo.setApi(request.getRequestURI());
            // 请求开始时间
            logInfo.setRequestStartTime(startTime.get());
            // 请求结束时间
            logInfo.setRequestEndTime(LocalDateTime.now());
            // 请求耗时
            logInfo.setRequestTimes(System.currentTimeMillis() - start.get());

            logInfo.setStatus("success");
            apiLogService.save(logInfo);
        } catch (Exception e) {
            log.error("记录请求日志失败，请检查：", e);
        } finally {
            start.remove();
            startTime.remove();
        }
    }

    private void setOperatorInfo(ApiLog logInfo) {
        Subject subject = SecurityUtils.getSubject();
        if (null != subject) {
            Object principal = subject.getPrincipal();
            if (principal instanceof User) {
                User user = (User) principal;
                // 请求用户ID
                logInfo.setUser(user.getId());
                logInfo.setTenant(user.getTenant());
                // 请求用户名称
                logInfo.setUserName(user.getName());
            } else if (principal instanceof DwjkAccess) {
                DwjkAccess dwjkAccess = (DwjkAccess) principal;
                // 请求用户ID
                logInfo.setUser(dwjkAccess.getId());
                logInfo.setTenant(dwjkAccess.getTenant());
                // 请求用户名称
                logInfo.setUserName(dwjkAccess.getSystemName());
            }
        }
    }

    /**
     * 异常返回通知，用于拦截异常日志信息 连接点抛出异常后执行
     *
     * @param joinPoint JoinPoint
     * @param e Throwable
     */
    @AfterThrowing(pointcut = "logPointCut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Throwable e) {
        // 获取RequestAttributes
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        // 从获取RequestAttributes中获取HttpServletRequest的信息
        HttpServletRequest request = (HttpServletRequest) requestAttributes.resolveReference(RequestAttributes.REFERENCE_REQUEST);

        try {
            // 从切面织入点处通过反射机制获取织入点处的方法
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();

            // 获取切入点所在的方法
            Method method = signature.getMethod();

            ApiLog logInfo = new ApiLog();

            // 获取操作
            com.mmyf.commons.log.ApiLog log = method.getAnnotation(com.mmyf.commons.log.ApiLog.class);
            if (Objects.nonNull(log)) {
                logInfo.setModule(log.module());
                logInfo.setApiName(log.name());
            }

            logInfo.setId(UUIDHelper.getUuid());
            // 请求的方法名
            // 请求参数
            logInfo.setRequest(method.getName() + ": " + JSON.toJSONString(getParams(signature, joinPoint)));
            // 返回结果
            logInfo.setResponse(SysUtils.getStackTrace(e));

            setOperatorInfo(logInfo);

            // 请求IP
            logInfo.setIp(RequestUtils.getRemoteIp(request));
            // 请求URI
            logInfo.setApi(request.getRequestURI());
            // 请求开始时间
            logInfo.setRequestStartTime(startTime.get());
            // 请求结束时间
            logInfo.setRequestEndTime(LocalDateTime.now());
            // 请求耗时
            logInfo.setRequestTimes(System.currentTimeMillis() - start.get());

            logInfo.setStatus("error");

            apiLogService.save(logInfo);
        } catch (Exception e2) {
            log.error("记录请求异常日志失败, 请检查：", e);
        } finally {
            start.remove();
            startTime.remove();
        }
    }

    private Map<String, Object> getParams(MethodSignature signature, JoinPoint joinPoint) {
        // 构造参数组集合
        Map<String, Object> result = Maps.newHashMap();
        // 参数名数组
        String[] parameterNames = signature.getParameterNames();
        if (parameterNames.length <= 0) {
            return result;
        }

        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < args.length; i++) {
            // request/response无法使用toJSON
            if (args[i] instanceof HttpServletRequest || args[i] instanceof HttpServletResponse) {
                continue;
            } else {
                result.put(parameterNames[i], args[i]);
            }
        }
        return result;
    }

}
