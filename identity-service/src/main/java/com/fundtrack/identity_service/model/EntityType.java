package com.fundtrack.identity_service.model;

/**
 * Categorizes the types of entities within the FundTrack system for auditing purposes.
 */
public enum EntityType {
    PROGRAM,
    ELIGIBILITY_RULE,
    REQUIRED_DOCUMENT,
    USER,
    PAYMENT,       // Added for financial tracking
    DISBURSEMENT,// Added for installment tracking
    APPLICATION,
    AUDIT_LOG
}


