package com.delma.notificationservice.controller;

import com.delma.common.dto.ApiResponse;
import com.delma.notificationservice.dto.NotificationCreateRequest;
import com.delma.notificationservice.dto.NotificationResponse;

import com.delma.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<NotificationResponse>> create(
            @RequestBody @Valid NotificationCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.create(request),"Notification created"));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUserNotifications(
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUserNotifications(userId),"Getting all the notification for user"));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("marked as read"));
    }
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<String>> deleteNotification(@PathVariable UUID id) {
        notificationService.deleteNotificationById(id);

        return ResponseEntity.ok(ApiResponse.success("Notification deleted successfully"));
    }
}
