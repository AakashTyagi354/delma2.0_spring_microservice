package com.delma.userservice.aop;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ControllerLoggingAspect {
    private final HttpServletRequest request;

    public ControllerLoggingAspect(HttpServletRequest request){
        this.request = request;
    }

    @Before("execution(* com.delma.userservice.controller..*(..))")
    public void logRequest(JoinPoint jp) {
        log.info(
                "🌐 {} {} | Handler={}",
                request.getMethod(),
                request.getRequestURI(),
                jp.getSignature().toShortString()
        );
    }

    @AfterReturning(pointcut = "execution(* com.delma.userservice.controller..*(..))", returning = "response")
    public void logResponse(Object response) {
        log.info("📤 Response={}", response);
    }



}
