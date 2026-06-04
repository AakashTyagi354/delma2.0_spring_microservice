package com.delma.paymentservice.serviceImpl;

import com.delma.paymentservice.entity.IdempotencyKey;
import com.delma.paymentservice.entity.IdempotencyStatus;
import com.delma.paymentservice.repository.IdempotencyKeyRepository;
import com.delma.paymentservice.service.IdempotencyService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Override
    public Optional<IdempotencyKey> findExistingKey(String key, String endpoint) {
        return idempotencyKeyRepository.findByIdempotencyKeyAndEndpoint(key, endpoint);
    }

    @Override
    public IdempotencyKey saveAsProcessing(String key, String endpoint, String userId) {
        IdempotencyKey record = IdempotencyKey.builder()
                .idempotencyKey(key)
                .endpoint(endpoint)
                .userId(userId)
                .status(IdempotencyStatus.PROCESSING)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        return idempotencyKeyRepository.save(record);
    }

    @Override
    @Transactional
    public void markCompleted(IdempotencyKey record, String responseBody) {
        record.setStatus(IdempotencyStatus.COMPLETED);
        record.setResponseBody(responseBody);
        idempotencyKeyRepository.save(record);
        log.info("Idempotency key [{}] marked COMPLETED", record.getIdempotencyKey());
    }

    @Override
    @Transactional
    public void markFailed(IdempotencyKey record) {
        record.setStatus(IdempotencyStatus.FAILED);
        idempotencyKeyRepository.save(record);
        log.warn("Idempotency key [{}] marked FAILED", record.getIdempotencyKey());
    }
}
