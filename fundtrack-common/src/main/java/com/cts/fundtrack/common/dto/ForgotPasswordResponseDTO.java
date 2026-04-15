package com.cts.fundtrack.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordResponseDTO {
    private String message;
    private String resetLink;
    private UUID userId; // Required for the Aspect to extract the ID
}