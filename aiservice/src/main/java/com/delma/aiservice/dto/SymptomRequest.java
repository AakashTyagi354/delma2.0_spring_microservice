package com.delma.aiservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SymptomRequest (
        @NotBlank(message = "Symptoms cannot be empty")
        @Size(min = 10, max = 1000, message = "Please describe symptoms in 10-1000 characters")
        String symptoms
){}
