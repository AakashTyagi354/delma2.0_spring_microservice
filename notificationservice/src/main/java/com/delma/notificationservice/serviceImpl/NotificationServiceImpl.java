package com.delma.notificationservice.serviceImpl;

import com.delma.notificationservice.dto.NotificationCreateRequest;
import com.delma.notificationservice.dto.NotificationResponse;
import com.delma.notificationservice.entity.Notification;
import com.delma.notificationservice.kafka.NotificationEvent;
import com.delma.notificationservice.reposistory.NotificationRepository;
import com.delma.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repository;

    @Override
    public NotificationResponse create(NotificationCreateRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();

        log.info("Creating notification for user: {}", userId);

        Notification notification = new Notification();

        notification.setUserId(userId);
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        Notification saved = repository.save(notification);
        return mapToResponse(saved);
    }

    @Override
    public List<NotificationResponse> getUserNotifications(String userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    @Override
    public void markAsRead(UUID notificationId) {
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        notification.setRead(true);
        repository.save(notification);
    }

    private NotificationResponse mapToResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getTitle(),
                n.getMessage(),
                n.getRead(),
                n.getCreatedAt()
        );
    }
    @Override
    public void createFromEvent(NotificationEvent event) {

        Notification notification = new Notification();
        notification.setUserId(event.getUserId());
        notification.setTitle(event.getTitle());
        notification.setMessage(event.getMessage());
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        repository.save(notification);
    }
}
