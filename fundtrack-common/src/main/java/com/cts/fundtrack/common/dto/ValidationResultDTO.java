package com.cts.fundtrack.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResultDTO {
    private String ruleName;   // e.g., "Income Threshold Check"
    private String result;     // e.g., "PASS" or "FAIL"
    private String message;    // e.g., "Income is within the allowed range."
    private LocalDateTime validatedAt;
}