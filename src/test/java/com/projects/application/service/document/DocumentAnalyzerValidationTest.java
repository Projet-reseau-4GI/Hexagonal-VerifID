package com.projects.application.service.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projects.application.port.out.AiAnalysisServicePort;
import com.projects.application.port.out.FileStoragePort;
import com.projects.application.port.out.OcrServicePort;
import com.projects.application.port.out.VerificationLogRepositoryPort;
import com.projects.application.port.in.billing.CheckQuotaUseCase;
import com.projects.adapter.in.web.dto.DocumentAnalysisResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour la validation des champs obligatoires
 * des nouveaux types documentaires dans AnalyzeDocumentUseCaseImpl.
 */
class DocumentAnalyzerValidationTest {

    private AnalyzeDocumentUseCaseImpl analyzer;
    private Method buildMethod;

    @BeforeEach
    void setUp() throws Exception {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        analyzer = new AnalyzeDocumentUseCaseImpl(
                mock(OcrServicePort.class),
                mock(AiAnalysisServicePort.class),
                mock(VerificationLogRepositoryPort.class),
                mock(FileStoragePort.class),
                validator,
                new ObjectMapper(),
                mock(CheckQuotaUseCase.class),
                new SimpleMeterRegistry()
        );

        buildMethod = AnalyzeDocumentUseCaseImpl.class
                .getDeclaredMethod("buildAnalysisResponse", String.class, String.class, Map.class, String.class);
        buildMethod.setAccessible(true);
    }

    private DocumentAnalysisResponse build(Map<String, String> fields) throws Exception {
        return (DocumentAnalysisResponse) buildMethod.invoke(analyzer, "", "", fields, "raw");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VEHICLE_REGISTRATION
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("VEHICLE_REGISTRATION valide avec registrationNumber + chassisNumber + ownerName")
    void vehicleRegistrationValidWithAllFields() throws Exception {
        Map<String, String> fields = Map.of(
                "documentType", "VEHICLE_REGISTRATION",
                "registrationNumber", "LT-2024-AB",
                "chassisNumber", "JT123456789",
                "ownerName", "Paul MBARGA",
                "brand", "Toyota",
                "model", "Corolla"
        );

        DocumentAnalysisResponse response = build(fields);

        assertThat(response.getIsValid()).isTrue();
        assertThat(response.getDocumentType()).isEqualTo("VEHICLE_REGISTRATION");
    }

    @Test
    @DisplayName("VEHICLE_REGISTRATION invalide sans registrationNumber")
    void vehicleRegistrationInvalidWithoutRegistrationNumber() throws Exception {
        Map<String, String> fields = Map.of(
                "documentType", "VEHICLE_REGISTRATION",
                "chassisNumber", "JT123456789",
                "ownerName", "Paul MBARGA"
        );

        DocumentAnalysisResponse response = build(fields);

        assertThat(response.getIsValid()).isFalse();
        assertThat(response.getValidationMessage()).contains("registrationNumber");
    }

    @Test
    @DisplayName("VEHICLE_REGISTRATION invalide sans ownerName")
    void vehicleRegistrationInvalidWithoutOwner() throws Exception {
        Map<String, String> fields = Map.of(
                "documentType", "VEHICLE_REGISTRATION",
                "registrationNumber", "LT-2024-AB",
                "chassisNumber", "JT123456789"
        );

        DocumentAnalysisResponse response = build(fields);

        assertThat(response.getIsValid()).isFalse();
        assertThat(response.getValidationMessage()).contains("ownerName");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAX_ID (NIU)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("TAX_ID valide avec taxIdNumber + taxpayerName")
    void taxIdValidWithRequiredFields() throws Exception {
        Map<String, String> fields = Map.of(
                "documentType", "TAX_ID",
                "taxIdNumber", "M012345678901P",
                "taxpayerName", "ACME SARL",
                "issuingAuthority", "DGI Cameroun"
        );

        DocumentAnalysisResponse response = build(fields);

        assertThat(response.getIsValid()).isTrue();
        assertThat(response.getDocumentType()).isEqualTo("TAX_ID");
    }

    @Test
    @DisplayName("TAX_ID invalide sans taxIdNumber")
    void taxIdInvalidWithoutTaxIdNumber() throws Exception {
        Map<String, String> fields = Map.of(
                "documentType", "TAX_ID",
                "taxpayerName", "ACME SARL"
        );

        DocumentAnalysisResponse response = build(fields);

        assertThat(response.getIsValid()).isFalse();
        assertThat(response.getValidationMessage()).contains("taxIdNumber");
    }

    @Test
    @DisplayName("TAX_ID invalide sans taxpayerName")
    void taxIdInvalidWithoutTaxpayerName() throws Exception {
        Map<String, String> fields = Map.of(
                "documentType", "TAX_ID",
                "taxIdNumber", "M012345678901P"
        );

        DocumentAnalysisResponse response = build(fields);

        assertThat(response.getIsValid()).isFalse();
        assertThat(response.getValidationMessage()).contains("taxpayerName");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUSINESS_REGISTRATION (RCCM)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BUSINESS_REGISTRATION valide avec rccmNumber + companyName")
    void businessRegistrationValidWithRequiredFields() throws Exception {
        Map<String, String> fields = Map.of(
                "documentType", "BUSINESS_REGISTRATION",
                "rccmNumber", "RC/DLA/2020/B/12345",
                "companyName", "ACME SARL",
                "legalForm", "SARL",
                "registeredOffice", "Douala"
        );

        DocumentAnalysisResponse response = build(fields);

        assertThat(response.getIsValid()).isTrue();
        assertThat(response.getDocumentType()).isEqualTo("BUSINESS_REGISTRATION");
    }

    @Test
    @DisplayName("BUSINESS_REGISTRATION invalide sans rccmNumber")
    void businessRegistrationInvalidWithoutRccmNumber() throws Exception {
        Map<String, String> fields = Map.of(
                "documentType", "BUSINESS_REGISTRATION",
                "companyName", "ACME SARL"
        );

        DocumentAnalysisResponse response = build(fields);

        assertThat(response.getIsValid()).isFalse();
        assertThat(response.getValidationMessage()).contains("rccmNumber");
    }

    @Test
    @DisplayName("BUSINESS_REGISTRATION invalide sans companyName")
    void businessRegistrationInvalidWithoutCompanyName() throws Exception {
        Map<String, String> fields = Map.of(
                "documentType", "BUSINESS_REGISTRATION",
                "rccmNumber", "RC/DLA/2020/B/12345"
        );

        DocumentAnalysisResponse response = build(fields);

        assertThat(response.getIsValid()).isFalse();
        assertThat(response.getValidationMessage()).contains("companyName");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Type UNKNOWN
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Type UNKNOWN → isValid=false avec message 'Type de document non reconnu'")
    void unknownTypeIsInvalid() throws Exception {
        Map<String, String> fields = Map.of(
                "documentType", "UNKNOWN",
                "documentNumber", "12345"
        );

        DocumentAnalysisResponse response = build(fields);

        assertThat(response.getIsValid()).isFalse();
        assertThat(response.getValidationMessage()).isEqualTo("Type de document non reconnu");
    }

    @Test
    @DisplayName("Type non reconnu → traité comme UNKNOWN")
    void unrecognizedTypeIsUnknown() throws Exception {
        Map<String, String> fields = Map.of(
                "documentType", "WHATEVER_RANDOM_TYPE",
                "documentNumber", "12345"
        );

        DocumentAnalysisResponse response = build(fields);

        assertThat(response.getIsValid()).isFalse();
        assertThat(response.getDocumentType()).isEqualTo("UNKNOWN");
    }
}
