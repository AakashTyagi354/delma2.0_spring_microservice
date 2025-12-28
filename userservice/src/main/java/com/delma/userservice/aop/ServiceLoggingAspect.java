package com.delma.userservice.aop;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ServiceLoggingAspect {

    @Around("execution(* com.delma.userservice.service..*(..))")
    public Object logService(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        String method = pjp.getSignature().toShortString();
        log.info("Started execution of " + method);

        try{
            Object result = pjp.proceed();
            long time = System.currentTimeMillis() - start;

            log.info("Completed execution of " + method + " in " + time + " ms");
            return result;

        }catch(Exception ex){
            log.error("Exception in " + method + ": " + ex.getMessage());
            throw ex;
        }
    }
}
