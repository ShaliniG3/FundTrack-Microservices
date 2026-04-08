package com.cts.fundtrack.dgcs.client.dto;

import lombok.*;
import java.util.UUID;

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