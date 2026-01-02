package com.delma.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NotificationCreateRequest {



    @NotBlank
    private String title;

    @NotBlank
    private String message;

    @NotBlank
    private String type;
}