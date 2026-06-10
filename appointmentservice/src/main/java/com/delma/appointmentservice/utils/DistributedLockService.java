package com.delma.appointmentservice.utils;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {
    private final StringRedisTemplate redisTemplate;
    private static final String LOCK_PREFIX = "lock:slot:";
    private static final Duration LOCK_EXPIRY = Duration.ofSeconds(10);

    public String tryAcquire(Long slotId){
        String lockKey = LOCK_PREFIX + slotId;
        String lockValue = UUID.randomUUID().toString();

        // Redis executes setIfAbsent atomically — no race condition possible
        Boolean aquired = redisTemplate.opsForValue().
                setIfAbsent(lockKey,lockValue,LOCK_EXPIRY);

        if(Boolean.TRUE.equals(aquired)){
            log.debug("Lock acquired for slotId: {} with value: {}",
                    slotId, lockValue);
            return lockValue;
        }
        log.debug("Lock NOT acquired for slotId: {} — already held", slotId);
        return null;
    }
    public void release(Long slotId, String lockValue){
        if(lockValue == null) return;
        String lockKey = LOCK_PREFIX + slotId;
        // No other operation can run between the GET check and the DEL
        String luaScript =
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "    return redis.call('del', KEYS[1]) " +
                        "else " +
                        "    return 0 " +
                        "end";
        redisTemplate.execute(
                new org.springframework.data.redis.core.script
                        .DefaultRedisScript<>(luaScript, Long.class),
                java.util.List.of(lockKey),
                lockValue
        );

        log.debug("Lock released for slotId: {}", slotId);

    }
}
