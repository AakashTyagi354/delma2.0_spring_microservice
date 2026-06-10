package com.delma.appointmentservice.datasource;



import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
@Order(1)
public class DataSourceRoutingAspect {

    // The pointcut expression:
    // "@annotation(transactional)" means:
    // "intercept any method annotated with @Transactional"
    // "transactional" is the parameter name — Spring binds the annotation instance to it
    @Around("@annotation(transactional)")
    public Object routeDataSource(
            ProceedingJoinPoint joinPoint,
            Transactional transactional
    ) throws  Throwable{
        // @Transactional(readOnly = true) → route to REPLICA
        // @Transactional                  → route to PRIMARY (readOnly defaults to false)
        // This is the single decision point — all routing logic lives here
        DataSourceType type = transactional.readOnly()
                ? DataSourceType.REPLICA
                : DataSourceType.PRIMARY;

        log.debug("Routing to {} for method: {}",
                type, joinPoint.getSignature().getName());

        // Set the ThreadLocal BEFORE proceeding
        DataSourceContextHolder.set(type);

        try{
            // proceed() calls the actual method
            return joinPoint.proceed();
        }finally {
            // If method throws exception or success → finally still runs → ThreadLocal cleared
            DataSourceContextHolder.clear();
            log.debug("Cleared datasource context after: {}",
                    joinPoint.getSignature().getName());
        }
    }
}
