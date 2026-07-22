package com.projects.adapter.out.ai;

import com.projects.application.port.out.AiAnalysisServicePort;
import com.projects.exception.ExternalServiceUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Infrastructure adapter — implements AiAnalysisServicePort using Google Gemini API.
 * Protected by Resilience4j circuit breaker "gemini-api".
 */
@Component
@Slf4j
public class GeminiAiAdapter implements AiAnalysisServicePort {

    private final WebClient webClient;
    private final String apiUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiAiAdapter(WebClient.Builder webClientBuilder,
                           @Value("${gemini.api.key:}") String apiKey,
                           @Value("${gemini.model:gemini-flash-lite-latest}") String model,
                           @Value("${gemini.api.url:}") String apiUrlTemplate) {
        this.webClient = webClientBuilder.build();
        this.apiUrl = apiUrlTemplate.replace("{model}", model).replace("{key}", apiKey);
    }

    @Override
    @CircuitBreaker(name = "gemini-api", fallbackMethod = "fallbackExtract")
    public Mono<Map<String, String>> extractDocumentData(String rawOcrText) {
        if (rawOcrText == null || rawOcrText.isBlank()) return Mono.just(new HashMap<>());

        String prompt = """
                You are a strict OCR document extraction engine for CEMAC zone documents (Cameroon, Gabon, Chad, Congo, CAR).

                RULES:
                - Return ONLY valid flat JSON. No markdown, no explanations.
                - Use null for missing or unreadable fields.
                - Dates format: yyyy-MM-dd.
                - documentType MUST be exactly one of:
                    ID_CARD | PASSPORT | DRIVER_LICENSE | VEHICLE_REGISTRATION | TAX_ID | BUSINESS_REGISTRATION
                - If the document does not match any type above, use UNKNOWN.

                FIELD MAPPING BY TYPE:

                For ID_CARD, PASSPORT, DRIVER_LICENSE:
                {
                  "documentType": "...", "issuingCountry": "...", "surname": "...",
                  "givenNames": "...", "dateOfBirth": "...", "issueDate": "...",
                  "expiryDate": "...", "documentNumber": "...", "sex": "...",
                  "height": "...", "placeOfBirth": "...", "occupation": "..."
                }

                For VEHICLE_REGISTRATION (Carte grise):
                {
                  "documentType": "VEHICLE_REGISTRATION", "issuingCountry": "...",
                  "registrationNumber": "...", "brand": "...", "model": "...",
                  "chassisNumber": "...", "ownerName": "...", "circulationDate": "..."
                }

                For TAX_ID (NIU / Numéro Identifiant Unique):
                {
                  "documentType": "TAX_ID", "issuingCountry": "...",
                  "taxIdNumber": "...", "taxpayerName": "...",
                  "issuingAuthority": "...", "issueDate": "..."
                }

                For BUSINESS_REGISTRATION (RCCM / Registre de Commerce):
                {
                  "documentType": "BUSINESS_REGISTRATION", "issuingCountry": "...",
                  "rccmNumber": "...", "companyName": "...", "legalForm": "...",
                  "registeredOffice": "...", "registrationDate": "..."
                }

                Return a single flat JSON object matching the detected document type.
                Include only fields relevant to the detected type. Set null for any missing field.

                Raw OCR text:
                """ + rawOcrText;

        Map<String, Object> requestBody = Map.of(
            "generationConfig", Map.of("responseMimeType", "application/json"),
            "contents", new Object[]{Map.of("role", "user",
                "parts", new Object[]{Map.of("text", prompt)})});

        return webClient.post().uri(apiUrl)
            .header("Content-Type", "application/json")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .map(this::parseGeminiResponse)
            .doOnError(e -> log.error("Gemini API error: {}", e.getMessage()))
            .onErrorReturn(new HashMap<>());
    }

    private Map<String, String> parseGeminiResponse(String response) {
        Map<String, String> result = new HashMap<>();
        try {
            JsonNode root = objectMapper.readTree(response);
            String text = root.path("candidates").get(0)
                .path("content").path("parts").get(0).path("text").asText();
            text = text.replaceAll("```json", "").replaceAll("```", "").trim();
            objectMapper.readTree(text).fields()
                .forEachRemaining(e -> { if (!e.getValue().isNull()) result.put(e.getKey(), e.getValue().asText()); });
        } catch (Exception e) {
            log.warn("Failed to parse Gemini response: {}", e.getMessage());
        }
        return result;
    }

    /**
     * Fallback method invoked when the "gemini-api" circuit breaker is open
     * or when the Gemini call throws an exception.
     */
    public Mono<Map<String, String>> fallbackExtract(String rawOcrText, Throwable t) {
        log.error("[gemini] Circuit breaker ouvert ou erreur Gemini : {}", t.getMessage());
        return Mono.error(new ExternalServiceUnavailableException(
                "Service d'analyse temporairement indisponible"));
    }
}
