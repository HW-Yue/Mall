package com.permissionsystem.aop;

import com.permissionsystem.annotation.LogOperation;
import com.permissionsystem.dto.LogDTO;
import com.permissionsystem.mapper.LogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;

@Aspect
@Component
public class OperationLogAspect {
    @Autowired
    private LogMapper operateLogMapper;

    // 环绕通知
    @Around("within(com.permissionsystem.controller..*)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        // 执行方法
        Object result = joinPoint.proceed();
        // 当前时间
        long endTime = System.currentTimeMillis();
        // 耗时
        long costTime = endTime - startTime;

        // 构建日志对象
        LogDTO operateLog = new LogDTO();
        operateLog.setUsername(getCurrentUsername()); // 需要实现 getCurrentUsername 方法
        operateLog.setCreateDate(LocalDateTime.now());
        operateLog.setOperationType(joinPoint.getTarget().getClass().getName());

        //把所有page方法的method里面的返回值result给去掉
        String method = "";
        if(joinPoint.getSignature().getName().contains("page")||result==null){
        method=  joinPoint.getSignature().getName()+','+Arrays.toString(joinPoint.getArgs());}
        else {
            method=  joinPoint.getSignature().getName()+','+Arrays.toString(joinPoint.getArgs())+','+
                    result.toString();
        }

        operateLog.setMethod(method);
        operateLog.setCostTime(costTime);
        operateLog.setIp(getCurrentIp());
        operateLog.setUserAgent(getCurrentUserAgent());
        // 插入日志
        operateLogMapper.insert(operateLog);
        return result;
    }

    // 示例方法，获取当前用户ID
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            return (String) principal;
        }

        // 对于未登录或者匿名用户，principal 可能不是我们期望的类型
        // 在这种情况下，我们返回 "anonymousUser" 或者 null，取决于你的业务需求
        // 这里我们简单处理，直接返回 principal 的 toString()
        // 但在生产环境中，你可能需要更精细的控制
        if (authentication.getName() != null) {
            return authentication.getName();
        }

        return null;
    }
    private String getCurrentIp() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }

        // 优先从 X-Forwarded-For 头获取，这在经过代理的情况下很有用
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 对于通过多个代理的情况，X-Forwarded-For 的值可能是由逗号分隔的 IP 列表
        // 第一个 IP 通常是原始客户端的 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0];
        }

        return ip;
    }
    private static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return attributes.getRequest();
        }
        return null;
    }
    private String getCurrentUserAgent() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            return request.getHeader("User-Agent");
        }
        return null;
    }
}
