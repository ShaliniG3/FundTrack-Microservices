package com.cts.fundtrack.common.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight DTO carrying essential user identity information resolved from
 * the Identity Service.
 *
 * <p>Used by the Application Service when populating
 * {@link ApplicationMetadataDTO#getApplicantName()} via a Feign call to the
 * Identity Service. Since all microservices (Disbursement, Compliance, Payment)
 * call {@code applicationClient.getApplicationMetadata()} to get applicant names,
 * the fix is centralised here — one Feign call in Application Service resolves
 * the name for all downstream consumers.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMetadataDTO {

    /** Unique identifier of the user in the Identity Service. */
    private UUID userId;

    /** Full display name of the user. */
    private String name;

    /** Email address of the user. */
    private String email;
}
