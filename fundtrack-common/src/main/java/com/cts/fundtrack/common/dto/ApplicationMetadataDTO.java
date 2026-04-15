package com.cts.fundtrack.common.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationMetadataDTO {
    private UUID applicationId;
    private UUID applicantUserId;
    private String applicantName;
    private String programName;
    private String status;
}