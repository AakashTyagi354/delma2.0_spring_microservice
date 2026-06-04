package com.delma.paymentservice.repository;

import com.delma.paymentservice.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IdempotencyKeyRepository  extends JpaRepository<IdempotencyKey,Long> {
    Optional<IdempotencyKey> findByIdempotencyKeyAndEndpoint(
            String idempotencyKey,
            String endpoint
    );

    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
