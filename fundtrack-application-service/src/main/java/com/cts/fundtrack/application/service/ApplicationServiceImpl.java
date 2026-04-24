package com.cts.fundtrack.application.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.cts.fundtrack.common.dto.*;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cts.fundtrack.application.client.ProgramServiceClient;
import com.cts.fundtrack.application.mapper.ApplicationMapper;
import com.cts.fundtrack.application.model.Application;
import com.cts.fundtrack.application.model.ApplicationValidation;
import com.cts.fundtrack.application.model.Document;
import com.cts.fundtrack.application.repository.ApplicationRepository;
import com.cts.fundtrack.application.repository.ApplicationValidationRepository;
import com.cts.fundtrack.application.repository.DocumentRepository;
import com.cts.fundtrack.common.aspect.Auditable;
import com.cts.fundtrack.common.client.NotificationClient;
import com.cts.fundtrack.common.exceptions.ApplicationNotFoundException;
import com.cts.fundtrack.common.exceptions.DuplicateApplicationException;
import com.cts.fundtrack.common.exceptions.InvalidApplicationStateException;
import com.cts.fundtrack.common.exceptions.UnsupportedDocumentTypeException;
import com.cts.fundtrack.common.models.enums.ActionType;
import com.cts.fundtrack.common.models.enums.ApplicationStatus;
import com.cts.fundtrack.common.models.enums.EntityType;
import com.cts.fundtrack.common.models.enums.NotificationCategory;
import com.cts.fundtrack.common.models.enums.VerificationStatus;
import com.cts.fundtrack.application.client.IdentityServiceClient;
import com.cts.fundtrack.common.dto.UserMetadataDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Primary implementation of {@link ApplicationService}, providing the core business
 * logic for grant application submission, update, retrieval, validation, and deletion.
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Enforces duplicate-application prevention per applicant/program pair.</li>
 *   <li>Validates and persists uploaded supporting documents, enforcing an allowed
 *       extension whitelist ({@code pdf}, {@code jpg}, {@code jpeg}, {@code png}).</li>
 *   <li>Executes automated eligibility rule evaluation using Spring Expression Language
 *       (SpEL) expressions fetched from the Program Service. Each rule result is stored
 *       as an {@link com.cts.fundtrack.application.model.ApplicationValidation} record.</li>
 *   <li>Dispatches in-system notifications via the Notification Service at key workflow
 *       transitions (submission, update, auto-rejection, withdrawal).</li>
 *   <li>Records auditable actions through the {@link com.cts.fundtrack.common.aspect.Auditable}
 *       AOP annotation on mutating methods.</li>
 * </ul>
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepo;
    private final DocumentRepository documentRepo;
    private final ApplicationValidationRepository validationRepo;
    private final ApplicationMapper applicationMapper;
    private final ProgramServiceClient programServiceClient;
    private final NotificationClient notificationClient;
    private final HttpServletRequest request;
    private final IdentityServiceClient identityServiceClient;
    // Self-injection via @Lazy to call @Transactional/@Auditable methods through
    // the Spring proxy instead of bypassing it with 'this' (fixes S6809)
    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private ApplicationServiceImpl self;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "jpg", "jpeg", "png");

    /**
     * Extracts the authenticated user's UUID from the {@code X-User-Id} HTTP
     * header injected by the API Gateway.
     *
     * @return the current user's {@link UUID}, or {@code null} if the header
     *         is absent (e.g., for system-initiated operations)
     */
    private UUID getCurrentUserId() {
        String userIdStr = request.getHeader("X-User-Id");
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

    @Override
    @Transactional
    @Auditable(action = ActionType.CREATE, entityName = EntityType.APPLICATION)
    public ApplicationResponseDTO applyToProgram(UUID applicantId, ApplicationRequestDTO dto) {
        log.info("Processing application submission for Applicant: {} to Program: {}", applicantId, dto.getProgramId());

        if (applicationRepo.existsByApplicantIdAndProgramId(applicantId, dto.getProgramId())) {
            throw new DuplicateApplicationException();
        }

        Application application = applicationMapper.toEntity(dto);
        application.setApplicantId(applicantId);
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setDocuments(new ArrayList<>());
        application.setValidations(new ArrayList<>());

        Application saved = applicationRepo.saveAndFlush(application);

        if (dto.getDocuments() != null && !dto.getDocuments().isEmpty()) {
            dto.getDocuments().forEach(docDto -> validateAndSaveDocument(saved, docDto));
        }

        // Transactional Confirmation: Sent to the person who clicked 'Apply'
        sendInternalNotification(getCurrentUserId(), saved.getApplicationId(),
            "Success: Your application for Program ID " + dto.getProgramId() + " has been received.", NotificationCategory.SUBMITTED);

        self.performValidation(saved.getApplicationId());

        ApplicationResponseDTO response = applicationRepo.findById(saved.getApplicationId())
                .map(applicationMapper::toResponseDTO)
                .orElseThrow(() -> new ApplicationNotFoundException(saved.getApplicationId()));
        return enrichResponse(response);
    }

    /**
     * Executes the automated eligibility validation engine for the specified application.
     *
     * <p>Fetches the SpEL-based eligibility rules for the application's target program
     * from the Program Service, clears any previous validation records, evaluates each
     * rule against the application's submitted data, and persists the results as
     * {@link com.cts.fundtrack.application.model.ApplicationValidation} records.</p>
     *
     * <p>If all rules pass, the application status remains {@code SUBMITTED}. If any
     * rule fails, the status is set to {@code REJECTED} and the applicant is notified.
     * Exceptions during validation are caught and logged without propagating, so that a
     * Program Service outage does not break the submission flow.</p>
     *
     * @param applicationId the UUID of the application to validate
     */
    @Transactional
    @Auditable(action = ActionType.STATUS_CHANGE, entityName = EntityType.APPLICATION)
    public void performValidation(UUID applicationId) {
        log.info("Executing automated validation for Application ID: {}", applicationId);
        try {
            Application app = fetchApplication(applicationId);
            validationRepo.deleteByApplication_ApplicationId(applicationId);
            app.getValidations().clear();

            List<EligibilityRuleDTO> ruleDTOs = programServiceClient.getRulesByProgramId(app.getProgramId());

            boolean allPassed = true;
            for (EligibilityRuleDTO ruleDto : ruleDTOs) {
                boolean isEligible = evaluateRule(ruleDto.getRuleExpression(), app.getApplicationData());

                ApplicationValidation result = ApplicationValidation.builder()
                        .application(app)
                        .ruleName(ruleDto.getRuleExpression())
                        .result(isEligible ? "PASSED" : "FAILED")
                        .message(isEligible ? "Criteria met." : "Does not meet: " + ruleDto.getRuleExpression())
                        .build();

                app.getValidations().add(result);
                if (!isEligible) allPassed = false;
            }

            validationRepo.saveAll(app.getValidations());
            app.setStatus(allPassed ? ApplicationStatus.SUBMITTED : ApplicationStatus.REJECTED);
            applicationRepo.saveAndFlush(app);

            // Notify applicant if the system automatically rejects them
            if (!allPassed) {
                sendInternalNotification(app.getApplicantId(), app.getApplicationId(),
                    "Update: Your application does not currently meet our automated eligibility criteria.", NotificationCategory.REJECTED);
            }

        } catch (Exception e) {
            log.error("Automated Validation Engine Error: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    @Auditable(action = ActionType.UPDATE, entityName = EntityType.APPLICATION)
    public ApplicationResponseDTO updateApplication(UUID applicationId, ApplicationUpdateDTO dto) {
        log.info("Updating data for Application ID: {}", applicationId);
        Application app = fetchApplication(applicationId);

        if (app.getStatus() == ApplicationStatus.APPROVED || app.getStatus() == ApplicationStatus.ACCEPTED || app.getStatus() == ApplicationStatus.REJECTED) {
            throw new InvalidApplicationStateException("Updates are not permitted for applications in " + app.getStatus() + " state.");
        }

        if (dto.getApplicationData() != null && !dto.getApplicationData().equals(app.getApplicationData())) {
            app.setApplicationData(dto.getApplicationData());
            app.setStatus(ApplicationStatus.SUBMITTED);
            applicationRepo.saveAndFlush(app);

            sendInternalNotification(getCurrentUserId(), applicationId,
                "Confirmation: You have successfully updated your application details.", NotificationCategory.APPLICATION);

            self.performValidation(applicationId);
        }

        return enrichResponse(applicationMapper.toResponseDTO(fetchApplication(applicationId)));
    }

    @Override
    @Transactional
    @Auditable(action = ActionType.DELETE, entityName = EntityType.APPLICATION)
    public void deleteApplication(UUID applicationId) {
        log.warn("Deleting application record: {}", applicationId);
        fetchApplication(applicationId);
        applicationRepo.deleteById(applicationId);

        sendInternalNotification(getCurrentUserId(), null,
            "Success: Your application has been permanently removed.", NotificationCategory.GENERAL);
    }

    /**
     * Validates a document DTO and persists it as a {@link com.cts.fundtrack.application.model.Document}
     * entity linked to the given application.
     *
     * <p>Validation checks that the file URI is non-null, contains a file extension,
     * and that the extension is in the allowed whitelist
     * ({@code pdf}, {@code jpg}, {@code jpeg}, {@code png}).
     * The document type is normalised to upper-case; if absent it defaults to
     * {@code "UNKNOWN"}. Initial verification status is set to {@code SUBMITTED}.</p>
     *
     * @param app    the parent {@link com.cts.fundtrack.application.model.Application}
     *               to which the document will be associated
     * @param docDto the document data transfer object provided by the client
     * @throws com.cts.fundtrack.common.exceptions.UnsupportedDocumentTypeException if
     *         the file URI is missing an extension or the extension is not permitted
     */
    private void validateAndSaveDocument(Application app, DocumentDTO docDto) {
        String fileUrl = docDto.getFileUri();
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new UnsupportedDocumentTypeException("Invalid Document: File URI is missing.");
        }

        String extension;
        if (fileUrl.startsWith("data:")) {
            // Base64 data URI: data:<mimeType>;base64,<data>
            // Extract extension from MIME type, e.g. "data:image/jpeg;base64,..." -> "jpeg"
            try {
                String mimeType = fileUrl.substring(5, fileUrl.indexOf(';'));
                extension = mimeType.substring(mimeType.indexOf('/') + 1).toLowerCase();
            } catch (Exception e) {
                throw new UnsupportedDocumentTypeException("Invalid Document: Unrecognised data URI format.");
            }
        } else {
            // Regular file path or URL — extract extension after the last dot
            int dotIndex = fileUrl.lastIndexOf('.');
            if (dotIndex == -1 || dotIndex == fileUrl.length() - 1) {
                throw new UnsupportedDocumentTypeException("Invalid Document: Missing file extension.");
            }
            extension = fileUrl.substring(dotIndex + 1).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new UnsupportedDocumentTypeException("Unsupported file type '." + extension + "'. Allowed types: pdf, jpg, jpeg, png.");
        }

        Document doc = Document.builder()
                .application(app)
                .docType(docDto.getDocType() != null ? docDto.getDocType().toUpperCase() : "UNKNOWN")
                .fileUri(fileUrl)
                .verificationStatus(VerificationStatus.SUBMITTED)
                .build();

        documentRepo.save(doc);
        app.getDocuments().add(doc);
    }

    /**
     * Evaluates a single SpEL eligibility rule expression against an applicant's
     * submitted data string.
     *
     * <p>The {@code data} string is expected to be a comma-separated list of
     * {@code key=value} pairs (e.g., {@code "income=30000,age=25"}). Each pair is
     * parsed into a variable that is injected into the SpEL evaluation context.
     * The rule expression is then normalised to lower-case and variable references
     * are prefixed with {@code #} before being evaluated.</p>
     *
     * <p>Returns {@code false} — treating the rule as not met — if either argument
     * is {@code null} or if the SpEL expression throws any exception during parsing
     * or evaluation.</p>
     *
     * @param rule the SpEL expression string representing the eligibility criterion
     *             (e.g., {@code "income >= 10000"})
     * @param data the applicant's submitted data as a comma-separated key=value string
     * @return {@code true} if the expression evaluates to {@code Boolean.TRUE};
     *         {@code false} otherwise
     */
    private boolean evaluateRule(String rule, String data) {
        if (data == null || rule == null) return false;
        try {
            Map<String, Object> variables = new HashMap<>();
            String[] pairs = data.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().toLowerCase();
                    String raw = keyValue[1].trim();
                    // Parse numeric values so SpEL can compare them with integer/decimal literals.
                    // Without this, "50000" >= 10000 throws a type-mismatch and always returns false.
                    Object value;
                    try {
                        value = Long.parseLong(raw);
                        System.out.println(value);
                    } catch (NumberFormatException e1) {
                        try {
                            value = Double.parseDouble(raw);
                             System.out.println(value);
                        } catch (NumberFormatException e2) {
                            value = raw; // keep as String for non-numeric values
                        }
                    }
                    variables.put(key, value);
                }
            }

            ExpressionParser parser = new SpelExpressionParser();
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariables(variables);

            String processedRule = rule.toLowerCase();
            for (String key : variables.keySet()) {
                processedRule = processedRule.replace(key, "#" + key);
            }

            Expression exp = parser.parseExpression(processedRule);
            return Boolean.TRUE.equals(exp.getValue(context, Boolean.class));
        } catch (Exception e) {
            log.error("SpEL Evaluation Error for Rule [{}]: {}", rule, e.getMessage());
            return false;
        }
    }

    @Override
    public ApplicantDetailsDTO getFullApplicationDetails(UUID applicationId) {
        return applicationMapper.toFullDetailsDTO(fetchApplication(applicationId));
    }

    @Override
    public List<DocumentDTO> getDocumentsByApplicationId(UUID applicationId) {
        return fetchApplication(applicationId).getDocuments().stream()
                .map(applicationMapper::toDocumentDTO).toList();
    }

    @Override
    public List<ValidationResultDTO> getValidationResults(UUID applicationId) {
        return validationRepo.findByApplication_ApplicationId(applicationId).stream()
                .map(applicationMapper::toValidationDTO).toList();
    }

    @Override
    public ProgramRequirementsDTO getRequirementsByApplication(UUID applicationId) {
        return programServiceClient.getRequirements(fetchApplication(applicationId).getProgramId());
    }

    /**
     * Loads an {@link Application} entity by its UUID or throws a typed exception
     * if not found, providing a single consistent lookup point throughout the service.
     *
     * @param id the UUID of the application to load
     * @return the matching {@link Application} entity
     * @throws com.cts.fundtrack.common.exceptions.ApplicationNotFoundException if no
     *         application with the given ID exists in the repository
     */
    private Application fetchApplication(UUID id) {
        return applicationRepo.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    @Override
    public List<ApplicationResponseDTO> getMyApplications(UUID applicantId) {
        log.info("Fetching all applications for applicantId: {}", applicantId);
        return applicationRepo.findAllByApplicantId(applicantId)
                .stream()
                .map(applicationMapper::toResponseDTO)
                .map(this::enrichResponse)
                .toList();
    }

    @Override
    public List<ApplicationResponseDTO> getApplicationsByProgramId(UUID programId) {
        log.info("Fetching all applications for programId: {}", programId);
        return applicationRepo.findByProgramId(programId)
                .stream()
                .map(applicationMapper::toResponseDTO)
                .map(this::enrichProgramName)
                .toList();
    }

    @Override
    public List<UUID> getApprovedApplicationIds(UUID programId) {
        log.info("Fetching approved application IDs for programId: {}", programId);
        return applicationRepo.findByProgramId(programId)
                .stream()
                .filter(a -> a.getStatus() == ApplicationStatus.APPROVED)
                .map(Application::getApplicationId)
                .toList();
    }

    @Override
    public Boolean hasPendingReviews(UUID programId) {
        log.info("Checking for pending reviews for programId: {}", programId);
        return applicationRepo.findByProgramId(programId)
                .stream()
                .anyMatch(a -> a.getStatus() == ApplicationStatus.SUBMITTED
                            || a.getStatus() == ApplicationStatus.UNDER_REVIEW);
    }

    @Override
    @Transactional
    public void updateApplicationStatus(UUID applicationId, String newStatus) {
        log.info("Updating status of application {} to {}", applicationId, newStatus);
        Application app = fetchApplication(applicationId);
        app.setStatus(ApplicationStatus.valueOf(newStatus.toUpperCase()));
        applicationRepo.save(app);
    }

    @Override
    public ApplicationMetadataDTO getApplicationMetadata(UUID applicationId) {
        Application app = fetchApplication(applicationId);

        // Resolve applicant name from Identity Service
        String applicantName = "Unknown";
        try {
            UserMetadataDTO user = identityServiceClient.getUserById(app.getApplicantId());
            if (user != null && user.getName() != null) {
                applicantName = user.getName();
            }
        } catch (Exception e) {
            log.warn("Could not resolve applicant name for ID {}: {}", app.getApplicantId(), e.getMessage());
        }

        // Resolve program name from Program Service (already wired, reuse existing client)
        String programName = "";
        try {
            ProgramRequirementsDTO prog = programServiceClient.getRequirements(app.getProgramId());
            if (prog != null && prog.getProgramName() != null) {
                programName = prog.getProgramName();
            }
        } catch (Exception e) {
            log.warn("Could not resolve program name for ID {}: {}", app.getProgramId(), e.getMessage());
        }

        return ApplicationMetadataDTO.builder()
                .applicationId(app.getApplicationId())
                .applicantUserId(app.getApplicantId())
                .applicantName(applicantName)
                .programName(programName)
                .status(app.getStatus() != null ? app.getStatus().name() : null)
                .build();
    }
    /**
     * Enriches an {@link ApplicationResponseDTO} with the {@code programName} fetched
     * from the Program Service. Unlike {@link #enrichResponse}, this does NOT set
     * {@code userName} from the current request header — when fetching applications
     * for a whole program, each application belongs to a different applicant and the
     * {@code X-User-Email} header represents the Finance manager, not the individual applicant.
     */
    private ApplicationResponseDTO enrichProgramName(ApplicationResponseDTO response) {
        // existing program name logic
        try {
            ProgramRequirementsDTO requirements = programServiceClient.getRequirements(response.getProgramId());
            if (requirements != null) {
                response.setProgramName(requirements.getProgramName());
            }
        } catch (Exception e) {
            log.warn("Could not fetch program name for programId={}: {}", response.getProgramId(), e.getMessage());
        }

        // ADD THIS — resolve applicant name from Identity Service
        try {
            if (response.getUserId() != null) {
                UserMetadataDTO user = identityServiceClient.getUserById(response.getUserId());
                if (user != null && user.getName() != null) {
                    response.setUserName(user.getName());
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch applicant name for userId={}: {}", response.getUserId(), e.getMessage());
        }

        return response;
    }

    /**
     * Enriches an {@link ApplicationResponseDTO} with {@code programName} and {@code userName}
     * that the mapper cannot populate on its own (mapper has no service dependencies).
     *
     * <p>Program name is fetched from the Program Service via the existing
     * {@code getRequirements} Feign call (which already carries the field).
     * User name is read from the {@code X-User-Email} gateway header on the
     * current request — the identity service is not wired into this service.</p>
     *
     * <p>Both lookups are best-effort: failures are logged and the field is left
     * {@code null} rather than propagating an exception.</p>
     */
    private ApplicationResponseDTO enrichResponse(ApplicationResponseDTO response) {
        try {
            ProgramRequirementsDTO requirements = programServiceClient.getRequirements(response.getProgramId());
            if (requirements != null) {
                response.setProgramName(requirements.getProgramName());
            }
        } catch (Exception e) {
            log.warn("Could not fetch program name for programId={}: {}", response.getProgramId(), e.getMessage());
        }
        response.setUserName(request.getHeader("X-User-Email"));
        return response;
    }

    private void sendInternalNotification(UUID userId, UUID appId, String msg, NotificationCategory cat) {
        if (userId == null) {
            log.warn("Notification skipped: Null user context.");
            return;
        }
        try {
            NotificationRequestDTO notification = NotificationRequestDTO.builder()
                    .userId(userId)
                    .applicationId(appId)
                    .message(msg)
                    .category(cat)
                    .build();
            notificationClient.sendNotification(notification);
            log.debug("Notification successfully queued for user: {}", userId);
        } catch (Exception e) {
            log.error("Communication Failure with Notification Service: {}", e.getMessage());
        }
    }
}