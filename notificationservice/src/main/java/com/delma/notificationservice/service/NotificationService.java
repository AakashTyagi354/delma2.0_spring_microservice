package com.delma.notificationservice.service;

import com.delma.notificationservice.dto.NotificationCreateRequest;
import com.delma.notificationservice.dto.NotificationResponse;
import com.delma.notificationservice.kafka.NotificationEvent;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    NotificationResponse create(NotificationCreateRequest request);

    List<NotificationResponse> getUserNotifications(String userId);

    void markAsRead(UUID notificationId);
    public void createFromEvent(NotificationEvent event);
}
