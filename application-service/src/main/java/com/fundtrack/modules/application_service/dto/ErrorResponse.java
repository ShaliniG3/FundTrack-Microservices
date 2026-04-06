package com.fundtrack.modules.application_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor // Added for flexibility in serialization
public class ErrorResponse {
    private LocalDateTime timestamp;
    private String message;
    private String details;
}