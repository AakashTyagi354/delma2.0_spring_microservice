package com.delma.paymentservice.service;

import com.delma.paymentservice.entity.IdempotencyKey;

import java.util.Optional;

public interface IdempotencyService {
    Optional<IdempotencyKey> findExistingKey(String key, String endpoint);
    IdempotencyKey saveAsProcessing(String key, String endpoint, String userId);
    void markCompleted(IdempotencyKey record, String responseBody);
    void markFailed(IdempotencyKey record);
}
