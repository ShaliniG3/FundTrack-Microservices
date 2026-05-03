package com.cts.fundtrack.common.models.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents the real-time transaction state of a specific fund transfer.
 */
@Schema(description = "The current transactional status of a disbursement payment")
public enum PaymentStatus {

    @Schema(description = "The transaction has been initiated or scheduled but not yet confirmed by the bank")
    PENDING,

    @Schema(description = "The funds have been successfully cleared and reached the recipient's account")
    SUCCESS,

    @Schema(description = "The transaction was rejected by the banking gateway or failed due to incorrect details")
    FAILED,

    @Schema(description = "The payment was successfully processed but subsequently reversed or returned")
    REFUNDED
}