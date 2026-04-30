package com.delma.notificationservice.reposistory;

import com.delma.notificationservice.entity.Notification;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
        List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    @Modifying
    @Transactional
        void deleteAllByUserId(String userId);
        List<Notification> findByUserId(String userId);
}
