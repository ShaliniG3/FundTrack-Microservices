package com.fundtrack.modules.application_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fundtrack.client.ProgramServiceClient;
import com.fundtrack.exceptions.*;
import com.fundtrack.modules.application_service.dto.*;
import com.fundtrack.modules.application_service.mappers.ApplicationMapper;
import com.fundtrack.modules.application_service.models.*;
import com.fundtrack.modules.application_service.models.enums.*;
import com.fundtrack.modules.application_service.repository.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepo;
    private final DocumentRepository documentRepo;
    private final ApplicationValidationRepository validationRepo;
    private final ApplicationMapper applicationMapper;
    private final ProgramServiceClient programServiceClient;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "jpg", "jpeg", "png");

    @Override
    @Transactional
    public ApplicationResponseDTO applyToProgram(UUID applicantId, ApplicationRequestDTO dto) {
        if (applicationRepo.existsByApplicantIdAndProgramId(applicantId, dto.getProgramId())) {
            throw new DuplicateApplicationException(applicantId, dto.getProgramId());
        }

        Application application = applicationMapper.toEntity(dto);
        application.setApplicantId(applicantId);
        application.setStatus(ApplicationStatus.SUBMITTED);

        // Ensure child lists are initialized
        application.setDocuments(new ArrayList<>());
        application.setValidations(new ArrayList<>());

        Application saved = applicationRepo.saveAndFlush(application);

        if (dto.getDocuments() != null && !dto.getDocuments().isEmpty()) {
            dto.getDocuments().forEach((docType, fileUrl) -> {
                validateAndSaveDocument(saved, docType, fileUrl);
            });
        }

        this.performValidation(saved.getApplicationId());

        // CRITICAL: Re-fetch the application so Hibernate pulls the new documents and
        // validations
        Application fullyPopulatedApp = applicationRepo.findById(saved.getApplicationId())
                .orElseThrow(() -> new ApplicationNotFoundException(saved.getApplicationId()));

        return applicationMapper.toResponseDTO(fullyPopulatedApp);
    }

    private void validateAndSaveDocument(Application app, String docType, String fileUrl) {
        String extension = fileUrl.substring(fileUrl.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new UnsupportedDocumentTypeException("Extension ." + extension + " is not supported.");
        }

        Document doc = Document.builder()
                .application(app)
                .docType(docType.toUpperCase())
                .fileUri(fileUrl)
                .verificationStatus(VerificationStatus.SUBMITTED)
                .build();

        documentRepo.save(doc);
        app.getDocuments().add(doc); // Link in memory
    }

    @Transactional
    public void performValidation(UUID applicationId) {
        try {
            Application app = applicationRepo.findById(applicationId)
                    .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

            // 1. Clear existing records from the database
            validationRepo.deleteByApplication_ApplicationId(applicationId);

            // 2. IMPORTANT: Re-initialize the internal list so it's ready to receive new
            // results
            if (app.getValidations() == null) {
                app.setValidations(new ArrayList<>());
            } else {
                app.getValidations().clear();
            }

            List<EligibilityRuleDTO> ruleDTOs = programServiceClient.getRulesByProgramId(app.getProgramId());

            boolean allPassed = true;
            for (EligibilityRuleDTO ruleDto : ruleDTOs) {
                boolean isEligible = evaluateRule(ruleDto.getRuleExpression(), app.getApplicationData());

                ApplicationValidation result = new ApplicationValidation();
                result.setApplication(app);
                result.setRuleName(ruleDto.getRuleExpression());
                result.setResult(isEligible ? "PASSED" : "FAILED");
                result.setMessage(isEligible ? "Criteria met." : "Does not meet: " + ruleDto.getRuleExpression());

                // 3. THE FIX: You MUST add the result to the 'app' list
                // This is what the Mapper looks at to build the JSON response!
                app.getValidations().add(result);

                if (!isEligible)
                    allPassed = false;
            }

            // 4. Save the results and the updated application status
            validationRepo.saveAll(app.getValidations());
            app.setStatus(allPassed ? ApplicationStatus.SUBMITTED : ApplicationStatus.REJECTED);

            applicationRepo.saveAndFlush(app);

        } catch (Exception e) {
            log.error("Validation Error: {}", e.getMessage());
        }
    }

    private boolean evaluateRule(String rule, String data) {
        if (data == null || rule == null)
            return false;
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
            return false;
        }
    }

    @Override
    @Transactional
    public ApplicationResponseDTO updateApplication(UUID applicationId, ApplicationUpdateDTO dto) {
        Application app = fetchApplication(applicationId);

        if (app.getStatus() == ApplicationStatus.APPROVED || app.getStatus() == ApplicationStatus.REJECTED) {
            throw new InvalidApplicationStateException(app.getStatus().name());
        }

        boolean dataChanged = false;
        if (dto.getApplicationData() != null && !dto.getApplicationData().equals(app.getApplicationData())) {
            app.setApplicationData(dto.getApplicationData());
            dataChanged = true;
        }

        if (dataChanged) {
            app.setStatus(ApplicationStatus.SUBMITTED);
            app = applicationRepo.saveAndFlush(app);
            this.performValidation(applicationId);
        }

        // Re-fetch here as well to ensure validation results are included in the update
        // response
        return applicationMapper.toResponseDTO(fetchApplication(applicationId));
    }

    @Override
    public ApplicantDetailsDTO getFullApplicationDetails(UUID applicationId) {
        return applicationMapper.toFullDetailsDTO(fetchApplication(applicationId));
    }

    @Override
    @Transactional
    public void deleteApplication(UUID applicationId) {
        if (!applicationRepo.existsById(applicationId)) {
            throw new ApplicationNotFoundException(applicationId);
        }
        applicationRepo.deleteById(applicationId);
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
}