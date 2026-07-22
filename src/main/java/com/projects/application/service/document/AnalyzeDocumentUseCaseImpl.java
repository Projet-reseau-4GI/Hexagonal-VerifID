package com.projects.application.service.document;

import com.projects.application.port.in.document.AnalyzeDocumentUseCase;
import com.projects.application.port.out.AiAnalysisServicePort;
import com.projects.application.port.out.FileStoragePort;
import com.projects.application.port.out.OcrServicePort;
import com.projects.application.port.out.VerificationLogRepositoryPort;
import com.projects.domain.model.VerificationLog;
import com.projects.adapter.in.web.dto.DocumentAnalysisResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Application use case — Document analysis orchestration.
 * Orchestrates OCR → AI extraction → validation → logging.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyzeDocumentUseCaseImpl implements AnalyzeDocumentUseCase {

    private final OcrServicePort ocrService;
    private final AiAnalysisServicePort aiService;
    private final VerificationLogRepositoryPort verificationLogRepository;
    private final FileStoragePort fileStoragePort;
    private final Validator validator;
    private final ObjectMapper objectMapper;
    private final com.projects.application.port.in.billing.CheckQuotaUseCase checkQuotaUseCase;
    private final MeterRegistry meterRegistry;

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("dd.MM.yyyy"), DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"), DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("d.MM.yyyy"), DateTimeFormatter.ofPattern("d/MM/yyyy")
    };

    @Override
    public Mono<DocumentAnalysisResponse> analyzeDocument(byte[] frontBytes, byte[] backBytes,
            String frontFilename, String organizationId) {
        boolean isPdf = frontFilename != null && frontFilename.toLowerCase().endsWith(".pdf");

        return checkQuotaUseCase.isQuotaAvailable(organizationId)
                .flatMap(isAvailable -> {
                    if (!isAvailable) {
                        return Mono.error(
                                new RuntimeException("QUOTA_EXCEEDED: Daily limit reached for this organization."));
                    }
                    return analyzeAndLog(frontBytes, backBytes, isPdf, organizationId)
                            .flatMap(response -> {
                                Mono<DocumentAnalysisResponse> resultMono = Mono.just(response);
                                if (Boolean.TRUE.equals(response.getIsValid())) {
                                    // Upload file to file_core if valid (chemin synchrone REST historique)
                                    Flux<org.springframework.core.io.buffer.DataBuffer> contentFlux = Flux
                                            .just(new DefaultDataBufferFactory().wrap(frontBytes));
                                    return fileStoragePort
                                            .storeFile(frontFilename, isPdf ? "application/pdf" : "image/jpeg",
                                                    contentFlux, null, null, null)
                                            .then(resultMono)
                                            .onErrorResume(e -> {
                                                log.error("Failed to upload document to file_core: {}", e.getMessage());
                                                return resultMono;
                                            });
                                }
                                return resultMono;
                            });
                });
    }

    @Override
    public Mono<DocumentAnalysisResponse> analyzeStoredDocument(byte[] frontBytes, String frontFilename,
            String organizationId) {
        boolean isPdf = frontFilename != null && frontFilename.toLowerCase().endsWith(".pdf");
        // Le fichier est déjà stocké dans le Kernel (mode asynchrone) → pas de
        // ré-upload.
        return checkQuotaUseCase.isQuotaAvailable(organizationId)
                .flatMap(isAvailable -> {
                    if (!isAvailable) {
                        return Mono.error(
                                new RuntimeException("QUOTA_EXCEEDED: Daily limit reached for this organization."));
                    }
                    return analyzeAndLog(frontBytes, null, isPdf, organizationId);
                });
    }

    /**
     * OCR → IA → validation → enregistrement du log. Commun aux modes synchrone et
     * asynchrone.
     */
    private Mono<DocumentAnalysisResponse> analyzeAndLog(byte[] frontBytes, byte[] backBytes, boolean isPdf,
            String organizationId) {
        Mono<String> frontOcr = ocrService.extractText(frontBytes, isPdf);
        Mono<String> backOcr = backBytes != null && backBytes.length > 0
                ? ocrService.extractText(backBytes, isPdf)
                : Mono.just("");

        return Mono.zip(frontOcr, backOcr)
                .flatMap(tuple -> {
                    String combined = tuple.getT1() + "\n" + tuple.getT2();
                    return aiService.extractDocumentData(combined)
                            .map(geminiFields -> buildAnalysisResponse(tuple.getT1(), tuple.getT2(), geminiFields,
                                    combined));
                })
                .flatMap(response -> logVerification(response, organizationId).thenReturn(response));
    }

    private static final Set<String> KNOWN_DOC_TYPES = Set.of(
            "ID_CARD", "PASSPORT", "DRIVER_LICENSE",
            "VEHICLE_REGISTRATION", "TAX_ID", "BUSINESS_REGISTRATION");

    private DocumentAnalysisResponse buildAnalysisResponse(String front, String back,
            Map<String, String> geminiFields,
            String rawCombined) {
        log.info("=== Starting Gemini Document Analysis ===");
        Map<String, String> fields = new HashMap<>(geminiFields);
        String docType = fields.getOrDefault("documentType", "UNKNOWN");
        String issuingCountry = fields.getOrDefault("issuingCountry", "UNKNOWN");

        // Normalise UNKNOWN for types not in the known set
        if (!KNOWN_DOC_TYPES.contains(docType)) {
            docType = "UNKNOWN";
        }

        fields.entrySet().removeIf(e -> e.getValue() == null ||
                e.getValue().equalsIgnoreCase("null") || e.getValue().isBlank());

        // ── Validation logic per document type ────────────────────────────────
        boolean valid;
        String msg;

        if ("UNKNOWN".equals(docType)) {
            valid = false;
            msg = "Type de document non reconnu";
        } else if ("VEHICLE_REGISTRATION".equals(docType)) {
            boolean hasRegNum = fields.get("registrationNumber") != null;
            boolean hasChassis = fields.get("chassisNumber") != null;
            boolean hasOwner = fields.get("ownerName") != null;
            valid = hasRegNum && hasChassis && hasOwner;
            msg = valid ? "Carte grise valide (Analyse Gemini)"
                    : buildMissingFieldsMsg("Carte grise",
                            hasRegNum ? null : "registrationNumber",
                            hasChassis ? null : "chassisNumber",
                            hasOwner ? null : "ownerName");
        } else if ("TAX_ID".equals(docType)) {
            boolean hasTaxId = fields.get("taxIdNumber") != null;
            boolean hasTaxpayer = fields.get("taxpayerName") != null;
            valid = hasTaxId && hasTaxpayer;
            msg = valid ? "NIU valide (Analyse Gemini)"
                    : buildMissingFieldsMsg("NIU",
                            hasTaxId ? null : "taxIdNumber",
                            hasTaxpayer ? null : "taxpayerName");
        } else if ("BUSINESS_REGISTRATION".equals(docType)) {
            boolean hasRccm = fields.get("rccmNumber") != null;
            boolean hasCompany = fields.get("companyName") != null;
            valid = hasRccm && hasCompany;
            msg = valid ? "RCCM valide (Analyse Gemini)"
                    : buildMissingFieldsMsg("RCCM",
                            hasRccm ? null : "rccmNumber",
                            hasCompany ? null : "companyName");
        } else {
            // ID_CARD, PASSPORT, DRIVER_LICENSE — classic identity logic
            LocalDate birthDate = parseDate(fields.get("dateOfBirth"));
            LocalDate issueDate = parseDate(fields.get("issueDate"));
            LocalDate expiryDate = parseDate(fields.get("expiryDate"));

            boolean namesValid = fields.get("surname") != null && fields.get("givenNames") != null;
            boolean isExpired = expiryDate != null && expiryDate.isBefore(LocalDate.now());
            boolean hasDocNumber = fields.get("documentNumber") != null;
            boolean nomencValid = validateNomenclature(issuingCountry, docType, fields.get("documentNumber"));

            valid = !isExpired && namesValid && hasDocNumber && nomencValid;
            StringBuilder sb = new StringBuilder();
            if (valid) {
                sb.append("Document valide (Analyse Gemini)");
            } else {
                if (!namesValid) sb.append("Noms manquants. ");
                if (!hasDocNumber) sb.append("Numéro manquant. ");
                else if (!nomencValid) sb.append("Format du numéro invalide pour ").append(issuingCountry).append(". ");
                if (isExpired) sb.append("Document expiré. ");
                if (sb.length() == 0) sb.append("Document non conforme.");
            }
            msg = sb.toString().trim();
        }

        double confidence = valid ? 0.9 : 0.5;

        // Build holderName for display — works for all types
        String holderName = buildHolderName(fields);

        // Parse dates for identity docs (null for non-identity docs)
        LocalDate birthDate = parseDate(fields.get("dateOfBirth"));
        LocalDate issueDate = parseDate(fields.getOrDefault("issueDate", fields.get("circulationDate")));
        LocalDate expiryDate = parseDate(fields.get("expiryDate"));

        DocumentAnalysisResponse response = DocumentAnalysisResponse.builder()
                .documentType(docType).issuingCountry(issuingCountry)
                .documentNumber(fields.getOrDefault("documentNumber",
                        fields.getOrDefault("registrationNumber",
                                fields.getOrDefault("taxIdNumber", fields.get("rccmNumber")))))
                .holderName(holderName)
                .dateOfBirth(birthDate).issueDate(issueDate).expirationDate(expiryDate)
                .isValid(valid).validationMessage(msg)
                .confidenceScore(confidence).hasUncertainty(confidence < 0.6)
                .additionalFields(buildAdditionalFields(fields)).rawExtractedText(rawCombined)
                .build();

        Set<ConstraintViolation<DocumentAnalysisResponse>> violations = validator.validate(response);
        if (!violations.isEmpty()) {
            String summary = violations.stream().map(v -> v.getMessage())
                    .distinct().collect(Collectors.joining(", "));
            response.setValidationMessage(response.getValidationMessage() + " (Format: " + summary + ")");
        }
        return response;
    }

    /** Build a missing-fields error message for non-identity document types. */
    private String buildMissingFieldsMsg(String docLabel, String... missingFields) {
        String missing = Arrays.stream(missingFields)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
        return docLabel + " invalide — champs manquants : " + missing;
    }

    private Mono<Void> logVerification(DocumentAnalysisResponse response, String organizationId) {
        String additionalFieldsJson = null;
        if (response.getAdditionalFields() != null && !response.getAdditionalFields().isEmpty()) {
            try {
                additionalFieldsJson = objectMapper.writeValueAsString(response.getAdditionalFields());
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize additional fields", e);
            }
        }
        String status = Boolean.TRUE.equals(response.getIsValid()) ? "ACCEPTED" : "REJECTED";
        String reason = Boolean.TRUE.equals(response.getIsValid()) ? null : response.getValidationMessage();

        VerificationLog logEntry = VerificationLog.builder()
                .platformId(organizationId).date(LocalDateTime.now())
                .docType(response.getDocumentType()).status(status).reason(reason)
                .confidence(response.getConfidenceScore()).processingTimeMs(1500)
                .documentNumber(response.getDocumentNumber()).holderName(response.getHolderName())
                .dateOfBirth(response.getDateOfBirth() != null ? response.getDateOfBirth().toString() : null)
                .issueDate(response.getIssueDate() != null ? response.getIssueDate().toString() : null)
                .expiryDate(response.getExpirationDate() != null ? response.getExpirationDate().toString() : null)
                .additionalFields(additionalFieldsJson)
                .build();

        return verificationLogRepository.save(logEntry)
                .doOnSuccess(saved -> {
                    meterRegistry.counter("verifid.documents.analyzed",
                            "status", status,
                            "organizationId", organizationId != null ? organizationId : "unknown")
                            .increment();
                })
                .then();
    }

    private String buildHolderName(Map<String, String> fields) {
        String s = fields.get("surname"), g = fields.get("givenNames");
        if (s != null && g != null)
            return s.trim() + " " + g.trim();
        return s != null ? s.trim() : (g != null ? g.trim() : "INCONNU");
    }

    private Map<String, String> buildAdditionalFields(Map<String, String> fields) {
        Set<String> topLevel = Set.of("surname", "givenNames", "documentNumber", "dateOfBirth", "issueDate",
                "expiryDate", "expirationDate", "documentType", "issuingCountry");
        Map<String, String> add = new LinkedHashMap<>();
        fields.forEach((k, v) -> {
            if (v != null && !v.isEmpty() && !topLevel.contains(k))
                add.put(k, v);
        });
        return add;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null)
            return null;
        String clean = dateStr.replaceAll("[^\\d./-]", "").trim();
        for (DateTimeFormatter fmt : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(clean, fmt);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private boolean validateNomenclature(String country, String docType, String documentNumber) {
        if (documentNumber == null || documentNumber.isBlank())
            return false;
        if (country == null || country.equalsIgnoreCase("UNKNOWN"))
            return documentNumber.length() >= 5;
        String nc = country.toLowerCase().trim();
        String nn = documentNumber.replaceAll("[^a-zA-Z0-9]", "");
        if (nc.contains("gabon"))
            return nn.length() == 14;
        if (nc.contains("cameroun") || nc.contains("cameroon")) {
            if ("ID_CARD".equals(docType))
                return nn.length() >= 9 && nn.length() <= 17;
            if ("PASSPORT".equals(docType))
                return nn.length() >= 7;
            return nn.length() >= 5;
        }
        if (nc.contains("tchad") || nc.contains("chad"))
            return nn.length() >= 5 && nn.length() <= 20;
        if (nc.contains("congo"))
            return nn.length() >= 5 && nn.length() <= 25;
        if (nc.contains("centrafrique") || nc.contains("central african"))
            return nn.length() >= 5 && nn.length() <= 20;
        return documentNumber.length() >= 5 && documentNumber.length() <= 30;
    }
}
