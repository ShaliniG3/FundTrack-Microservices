package com.cts.fundtrack.common.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

/**
 * Data Transfer Object for creating or updating a Program.
 * <p>
 * This class encapsulates all necessary information for a funding program, 
 * including financial constraints, timelines, and the associated 
 * requirements for eligibility and documentation.
 * </p>
 */
@Data
public class ProgramRequestDTO {
	
    /**
     * The unique or identifying name of the program.
     * Must not be null or blank.
     */
	private String name;

    /**
     * A detailed explanation of the program's goals and purpose.
     */
	private String description;
	
    /**
     * The total financial allocation for this program.
     * Must be a positive numerical value.
     */
	private Double budget;
	
    /**
     * The date the program becomes active or starts accepting applications.
     */
	private LocalDate startDate;
	
    /**
     * The date the program expires or stops accepting applications.
     */
	private LocalDate endDate;
	
    /**
     * A collection of {@link EligibilityRuleDTO} objects that define 
     * the criteria a candidate must meet to qualify for the program.
     */
	private List<EligibilityRuleDTO> eligibilityRules;

    /**
     * A list of {@link RequiredDocumentDTO} objects specifying the 
     * paperwork or uploads needed for a complete application.
     */
	private List<RequiredDocumentDTO> requiredDocuments;
}

