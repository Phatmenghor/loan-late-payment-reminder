package com.backend.shared.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * AOP Aspect responsible for measuring execution time of application REST Controller endpoints.
 * Scoped exclusively to com.backend package controllers. Emits slow API warnings when execution time exceeds threshold.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class ApiExecutionTimeAspect {

    private static final long SLOW_API_THRESHOLD_MS = 1000L;

    @Pointcut("within(com.backend..) && within(@org.springframework.web.bind.annotation.RestController *)")
    public void applicationControllerPointcut() {}

    @Around("applicationControllerPointcut()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String traceId = MDC.get("traceId");

        HttpServletRequest request = null;
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            request = attributes.getRequest();
        }

        String httpMethod = request != null ? request.getMethod() : "UNKNOWN";
        String uri = request != null ? request.getRequestURI() : (className + "." + methodName);

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - start;

            if (executionTime > SLOW_API_THRESHOLD_MS) {
                log.warn("SLOW API WARNING: {} {} [{}.{}()] completed in {}ms [traceId={}]",
                        httpMethod, uri, className, methodName, executionTime, traceId);
            }

            return result;
        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - start;
            log.error("API FAILED: {} {} [{}.{}()] failed after {}ms [traceId={}]: {}",
                    httpMethod, uri, className, methodName, executionTime, traceId, throwable.getMessage());
            throw throwable;
        }
    }
}
