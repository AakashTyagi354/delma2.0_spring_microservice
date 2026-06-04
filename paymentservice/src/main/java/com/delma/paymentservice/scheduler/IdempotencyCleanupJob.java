package com.delma.paymentservice.scheduler;


import com.delma.paymentservice.repository.IdempotencyKeyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotencyCleanupJob {
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanExpiredKeys() {
        log.info("Starting idempotency key cleanup...");
        idempotencyKeyRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Idempotency key cleanup complete");
    }
}
