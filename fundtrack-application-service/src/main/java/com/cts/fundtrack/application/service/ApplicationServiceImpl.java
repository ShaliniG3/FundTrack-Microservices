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
import com.cts.fundtrack.common.dto.*;
import com.cts.fundtrack.common.exceptions.*;
import com.cts.fundtrack.common.models.enums.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    @Auditable(action = ActionType.CREATE, entityName = EntityType.APPLICATION)
    public ApplicationResponseDTO applyToProgram(UUID applicantId, ApplicationRequestDTO dto) {
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
            dto.getDocuments().forEach(docDto -> {
                validateAndSaveDocument(saved, docDto);
            });
        }

        this.performValidation(saved.getApplicationId());

        Application fullyPopulatedApp = applicationRepo.findById(saved.getApplicationId())
                .orElseThrow(() -> new ApplicationNotFoundException(saved.getApplicationId()));

        return applicationMapper.toResponseDTO(fullyPopulatedApp);
    }

    private void validateAndSaveDocument(Application app, DocumentDTO docDto) {
        String fileUrl = docDto.getFileUri();
        if (fileUrl == null || !fileUrl.contains(".")) {
             throw new UnsupportedDocumentTypeException("Invalid file URI provided.");
        }

        String extension = fileUrl.substring(fileUrl.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new UnsupportedDocumentTypeException("Extension ." + extension + " is not supported.");
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

    @Transactional
    @Auditable(action = ActionType.STATUS_CHANGE, entityName = EntityType.APPLICATION)
    public void performValidation(UUID applicationId) {
        try {
            Application app = fetchApplication(applicationId);

            validationRepo.deleteByApplication_ApplicationId(applicationId);

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

                app.getValidations().add(result);

                if (!isEligible) allPassed = false;
            }

            validationRepo.saveAll(app.getValidations());
            app.setStatus(allPassed ? ApplicationStatus.SUBMITTED : ApplicationStatus.REJECTED);

            applicationRepo.saveAndFlush(app);

        } catch (Exception e) {
            log.error("Validation Error: {}", e.getMessage());
        }
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
            log.error("Rule Evaluation Error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    @Auditable(action = ActionType.UPDATE, entityName = EntityType.APPLICATION)
    public ApplicationResponseDTO updateApplication(UUID applicationId, ApplicationUpdateDTO dto) {
        Application app = fetchApplication(applicationId);

        if (app.getStatus() == ApplicationStatus.APPROVED || app.getStatus() == ApplicationStatus.REJECTED) {
            throw new InvalidApplicationStateException(app.getStatus().name());
        }

        if (dto.getApplicationData() != null && !dto.getApplicationData().equals(app.getApplicationData())) {
            app.setApplicationData(dto.getApplicationData());
            app.setStatus(ApplicationStatus.SUBMITTED);
            app = applicationRepo.saveAndFlush(app);
            this.performValidation(applicationId);
        }

        return applicationMapper.toResponseDTO(fetchApplication(applicationId));
    }

    @Override
    @Transactional
    @Auditable(action = ActionType.DELETE, entityName = EntityType.APPLICATION)
    public void deleteApplication(UUID applicationId) {
        if (!applicationRepo.existsById(applicationId)) {
            throw new ApplicationNotFoundException(applicationId);
        }
        applicationRepo.deleteById(applicationId);
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
}