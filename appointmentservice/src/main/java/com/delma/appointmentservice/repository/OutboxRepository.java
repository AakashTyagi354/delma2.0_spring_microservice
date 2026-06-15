package com.delma.appointmentservice.repository;

import com.delma.appointmentservice.entity.OutboxEvent;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent,Long> {
    @Query("SELECT o FROM OutboxEvent o WHERE o.publishedAt IS NULL " +
            "ORDER BY o.createdAt ASC")
    List<OutboxEvent> findUnpublished(PageRequest pageable);
}
