package com.delma.appointmentservice.entity;



import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events",
        indexes = {
                @Index(name = "idx_outbox_unpublished",
                        columnList = "published_at, created_at")
        })
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String aggregateType;     // e.g., "ConsultationNotes"

    @Column(nullable = false)
    private Long aggregateId;          // e.g., the notes id

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime publishedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OutboxEvent that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}