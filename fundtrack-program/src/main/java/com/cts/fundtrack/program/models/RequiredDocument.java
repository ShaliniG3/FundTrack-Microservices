package com.cts.fundtrack.program.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * JPA entity representing a document that applicants must submit when applying
 * to a grant funding program.
 *
 * <p>Each {@code RequiredDocument} belongs to exactly one {@link Program} and specifies
 * a document type (by name) along with whether submission of that document is mandatory
 * or optional. The Application Service uses this information to validate that an
 * applicant's submission is complete before it is accepted for review.</p>
 *
 * <p>Documents are owned by their parent {@link Program} via a many-to-one relationship.
 * The {@code @JsonIgnore} annotation on the {@code program} field prevents infinite
 * recursion during JSON serialization when a document is serialized as part of a program
 * response.</p>
 *
 * <p>Maps to the {@code required_documents} database table.</p>
 */
@Entity
@Table(name = "required_documents")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RequiredDocument {

    /**
     * Unique identifier for this required document record, generated as a UUID by the database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID documentId;

    /**
     * The parent grant funding program that this document requirement belongs to.
     *
     * <p>Loaded lazily to avoid unnecessary joins when only the document's own fields
     * are needed. Annotated with {@code @JsonIgnore} to break the bidirectional
     * serialization cycle between {@link Program} and its documents collection.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    @JsonIgnore // Prevents infinite loop during JSON serialization
    private Program program;

    /**
     * The name or type of the required document.
     * For example: {@code "Proof of Non-Profit Status"} or {@code "Annual Budget Report"}.
     * Must not be null.
     */
    @Column(nullable = false)
    private String name;

    /**
     * Indicates whether this document is mandatory for a valid application submission.
     * <ul>
     *   <li>{@code true} — the applicant must provide this document; omission causes
     *       the application to be rejected during validation.</li>
     *   <li>{@code false} — the document is optional and its absence does not block
     *       submission.</li>
     * </ul>
     * Must not be null.
     */
    @Column(nullable = false)
    private Boolean mandatory;
}
