package com.cts.fundtrack.application.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
import com.cts.fundtrack.common.dto.*;
import com.cts.fundtrack.common.exceptions.*;
import com.cts.fundtrack.common.models.enums.*;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for managing the lifecycle of funding applications.
 * <p>
 * This service handles the initial submission, automated eligibility validation 
 * via SpEL (Spring Expression Language), and document management. It ensures 
 * real-time feedback to applicants through integrated notifications.
 * </p>
 *
 * @author FundTrack Development Team
 * @version 1.4
 * @since 2026-04-16
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

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "jpg", "jpeg", "png");

    /**
     * Extracts the Unique Identifier of the currently authenticated user from request headers.
     * @return The UUID of the logged-in user.
     */
    private UUID getCurrentUserId() {
        String userIdStr = request.getHeader("X-User-Id");
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

    /**
     * Processes a new application submission for a funding program.
     * <p>
     * Performs duplicate checks, persists the application and documents, and triggers 
     * an automated validation workflow. Sends a confirmation notification to the applicant.
     * </p>
     *
     * @param applicantId The ID of the applicant (owner).
     * @param dto The application data and document references.
     * @return The persisted application details.
     * @throws DuplicateApplicationException if the applicant has already applied to this program.
     */
    @Override
    @Transactional
    @Auditable(action = ActionType.CREATE, entityName = EntityType.APPLICATION)
    public ApplicationResponseDTO applyToProgram(UUID applicantId, ApplicationRequestDTO dto) {
        log.info("Processing application submission for Applicant: {} to Program: {}", applicantId, dto.getProgramId());
        
        if (applicationRepo.existsByApplicantIdAndProgramId(applicantId, dto.getProgramId())) {
            throw new DuplicateApplicationException(applicantId, dto.getProgramId());
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

        // 🚀 Transactional Confirmation: Sent to the person who clicked 'Apply'
        sendInternalNotification(getCurrentUserId(), saved.getApplicationId(), 
            "Success: Your application for Program ID " + dto.getProgramId() + " has been received.", NotificationCategory.SUBMITTED);

        this.performValidation(saved.getApplicationId());

        return applicationRepo.findById(saved.getApplicationId())
                .map(applicationMapper::toResponseDTO)
                .orElseThrow(() -> new ApplicationNotFoundException(saved.getApplicationId()));
    }

    /**
     * Executes automated eligibility rules against the application data using SpEL.
     * <p>
     * If validation fails, the application status is set to REJECTED and a 
     * notification is dispatched to the applicant.
     * </p>
     *
     * @param applicationId The ID of the application to validate.
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

            // 🚀 Workflow Update: Notify applicant if the system automatically rejects them
            if (!allPassed) {
                sendInternalNotification(app.getApplicantId(), app.getApplicationId(), 
                    "Update: Your application does not currently meet our automated eligibility criteria.", NotificationCategory.REJECTED);
            }

        } catch (Exception e) {
            log.error("Automated Validation Engine Error: {}", e.getMessage());
        }
    }

    /**
     * Updates application data and re-triggers validation if the business data changed.
     */
    @Override
    @Transactional
    @Auditable(action = ActionType.UPDATE, entityName = EntityType.APPLICATION)
    public ApplicationResponseDTO updateApplication(UUID applicationId, ApplicationUpdateDTO dto) {
        log.info("Updating data for Application ID: {}", applicationId);
        Application app = fetchApplication(applicationId);

        if (app.getStatus() == ApplicationStatus.APPROVED || app.getStatus() == ApplicationStatus.REJECTED) {
            throw new InvalidApplicationStateException("Updates are not permitted for applications in " + app.getStatus() + " state.");
        }

        if (dto.getApplicationData() != null && !dto.getApplicationData().equals(app.getApplicationData())) {
            app.setApplicationData(dto.getApplicationData());
            app.setStatus(ApplicationStatus.SUBMITTED);
            app = applicationRepo.saveAndFlush(app);
            
            sendInternalNotification(getCurrentUserId(), applicationId, 
                "Confirmation: You have successfully updated your application details.", NotificationCategory.APPLICATION);

            this.performValidation(applicationId);
        }

        return applicationMapper.toResponseDTO(fetchApplication(applicationId));
    }

    @Override
    @Transactional
    @Auditable(action = ActionType.DELETE, entityName = EntityType.APPLICATION)
    public void deleteApplication(UUID applicationId) {
        log.warn("Deleting application record: {}", applicationId);
        Application app = fetchApplication(applicationId);
        applicationRepo.deleteById(applicationId);

        sendInternalNotification(getCurrentUserId(), null, 
            "Success: Your application has been permanently removed.", NotificationCategory.GENERAL);
    }

    private void validateAndSaveDocument(Application app, DocumentDTO docDto) {
        String fileUrl = docDto.getFileUri();
        if (fileUrl == null || !fileUrl.contains(".")) {
             throw new UnsupportedDocumentTypeException("Invalid Document: Missing file extension.");
        }

        String extension = fileUrl.substring(fileUrl.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new UnsupportedDocumentTypeException("Extension ." + extension + " is strictly prohibited.");
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

    private boolean evaluateRule(String rule, String data) {
        if (data == null || rule == null) return false;
        try {
            Map<String, Object> variables = new HashMap<>();
            String[] pairs = data.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    variables.put(keyValue[0].trim().toLowerCase(), keyValue[1].trim());
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
                .map(applicationMapper::toDocumentDTO).collect(Collectors.toList());
    }

    @Override
    public List<ValidationResultDTO> getValidationResults(UUID applicationId) {
        return validationRepo.findByApplication_ApplicationId(applicationId).stream()
                .map(applicationMapper::toValidationDTO).collect(Collectors.toList());
    }

    @Override
    public ProgramRequirementsDTO getRequirementsByApplication(UUID applicationId) {
        return programServiceClient.getRequirements(fetchApplication(applicationId).getProgramId());
    }

    private Application fetchApplication(UUID id) {
        return applicationRepo.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    /**
     * Dispatches notifications to the internal Notification Microservice.
     */
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