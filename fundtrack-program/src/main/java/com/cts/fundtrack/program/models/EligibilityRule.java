package com.cts.fundtrack.program.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "eligibility_rules")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class EligibilityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "rule_id", updatable = false, nullable = false)
    private UUID ruleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    @JsonIgnore // Prevents infinite loop during JSON serialization
    private Program program; 

    @Column(nullable = false)
    private String ruleDescription;

    @Column(nullable = false)
    private String ruleExpression;
}