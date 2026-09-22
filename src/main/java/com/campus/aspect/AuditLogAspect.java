package com.campus.aspect;

import com.alibaba.fastjson.JSON;
import com.campus.annotation.AuditLog;
import com.campus.entity.AuditLogEntity;
import com.campus.mapper.AuditLogMapper;
import com.campus.utils.StudentHolder;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 操作审计日志 AOP 切面
 * 拦截 @AuditLog 注解的方法，自动记录操作日志到 tb_audit_log
 */
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    @Resource
    private AuditLogMapper auditLogMapper;

    @Around("@annotation(com.campus.annotation.AuditLog)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        AuditLogEntity logEntity = new AuditLogEntity();

        // 解析注解
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        AuditLog auditLog = method.getAnnotation(AuditLog.class);
        logEntity.setModule(auditLog.module());
        logEntity.setOperation(auditLog.operation());
        logEntity.setMethod(point.getTarget().getClass().getName() + "." + method.getName());

        // 获取请求信息
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            logEntity.setIp(getClientIp(request));
        }

        // 获取当前用户
        Long userId = StudentHolder.getStudentId();
        logEntity.setUserId(userId);

        // 获取参数（截断防止过长）
        try {
            Object[] args = point.getArgs();
            String params = JSON.toJSONString(args);
            logEntity.setParams(params.length() > 2000 ? params.substring(0, 2000) : params);
        } catch (Exception e) {
            logEntity.setParams("参数序列化失败");
        }

        // 执行目标方法
        Object result;
        try {
            result = point.proceed();
            logEntity.setStatus(1); // 成功
        } catch (Throwable ex) {
            logEntity.setStatus(0); // 失败
            String errMsg = ex.getMessage();
            logEntity.setErrorMsg(errMsg != null && errMsg.length() > 2000
                    ? errMsg.substring(0, 2000) : errMsg);
            throw ex;
        } finally {
            logEntity.setDuration(System.currentTimeMillis() - startTime);
            // 异步保存日志（不阻塞业务）
            try {
                auditLogMapper.insert(logEntity);
            } catch (Exception e) {
                log.error("审计日志保存失败", e);
            }
        }

        return result;
    }

    /** 获取客户端真实 IP */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
