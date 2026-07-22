package com.projects.adapter.out.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour GeminiAiAdapter.
 * Teste le parsing des réponses Gemini sans appel réseau réel.
 */
class GeminiAiAdapterTest {

    private GeminiAiAdapter adapter;
    private Method parseMethod;

    @BeforeEach
    void setUp() throws Exception {
        // Instancier l'adapter avec des valeurs fictives (pas d'appel réseau dans ces tests)
        adapter = new GeminiAiAdapter(
                org.springframework.web.reactive.function.client.WebClient.builder(),
                "fake-api-key",
                "gemini-flash-lite-latest",
                "https://fake.googleapis.com/{model}?key={key}"
        );

        // Accès à la méthode privée parseGeminiResponse via reflection
        parseMethod = GeminiAiAdapter.class.getDeclaredMethod("parseGeminiResponse", String.class);
        parseMethod.setAccessible(true);
    }

    private Map<String, String> parse(String json) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) parseMethod.invoke(adapter, json);
        return result;
    }

    private String wrapAsGeminiResponse(String innerJson) {
        return """
                {
                  "candidates": [{
                    "content": {
                      "parts": [{"text": "%s"}]
                    }
                  }]
                }
                """.formatted(innerJson.replace("\"", "\\\"").replace("\n", "\\n"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON valide → map correcte
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("JSON ID_CARD valide → tous les champs non-nuls présents dans la map")
    void validIdCardJsonParsedCorrectly() throws Exception {
        String innerJson = "{\"documentType\":\"ID_CARD\",\"surname\":\"DUPONT\",\"givenNames\":\"Jean\",\"documentNumber\":\"AB1234567\",\"issuingCountry\":\"Cameroun\",\"dateOfBirth\":\"1990-05-15\"}";
        String geminiResponse = wrapAsGeminiResponse(innerJson);

        Map<String, String> result = parse(geminiResponse);

        assertThat(result).containsEntry("documentType", "ID_CARD");
        assertThat(result).containsEntry("surname", "DUPONT");
        assertThat(result).containsEntry("givenNames", "Jean");
        assertThat(result).containsEntry("documentNumber", "AB1234567");
        assertThat(result).containsEntry("issuingCountry", "Cameroun");
        assertThat(result).containsEntry("dateOfBirth", "1990-05-15");
    }

    @Test
    @DisplayName("JSON VEHICLE_REGISTRATION valide → champs carte grise présents")
    void validVehicleRegistrationParsed() throws Exception {
        String innerJson = "{\"documentType\":\"VEHICLE_REGISTRATION\",\"registrationNumber\":\"LT-2024-AB\",\"brand\":\"Toyota\",\"model\":\"Corolla\",\"chassisNumber\":\"JTDBF12E900123456\",\"ownerName\":\"Paul MBARGA\"}";
        String geminiResponse = wrapAsGeminiResponse(innerJson);

        Map<String, String> result = parse(geminiResponse);

        assertThat(result).containsEntry("documentType", "VEHICLE_REGISTRATION");
        assertThat(result).containsEntry("registrationNumber", "LT-2024-AB");
        assertThat(result).containsEntry("brand", "Toyota");
        assertThat(result).containsEntry("chassisNumber", "JTDBF12E900123456");
        assertThat(result).containsEntry("ownerName", "Paul MBARGA");
    }

    @Test
    @DisplayName("JSON TAX_ID valide → champs NIU présents")
    void validTaxIdParsed() throws Exception {
        String innerJson = "{\"documentType\":\"TAX_ID\",\"taxIdNumber\":\"M012345678901P\",\"taxpayerName\":\"ENTERPRISE SARL\",\"issuingAuthority\":\"DGI Cameroun\"}";
        String geminiResponse = wrapAsGeminiResponse(innerJson);

        Map<String, String> result = parse(geminiResponse);

        assertThat(result).containsEntry("documentType", "TAX_ID");
        assertThat(result).containsEntry("taxIdNumber", "M012345678901P");
        assertThat(result).containsEntry("taxpayerName", "ENTERPRISE SARL");
    }

    @Test
    @DisplayName("JSON BUSINESS_REGISTRATION valide → champs RCCM présents")
    void validBusinessRegistrationParsed() throws Exception {
        String innerJson = "{\"documentType\":\"BUSINESS_REGISTRATION\",\"rccmNumber\":\"RC/DLA/2020/B/12345\",\"companyName\":\"ACME SARL\",\"legalForm\":\"SARL\",\"registeredOffice\":\"Douala\"}";
        String geminiResponse = wrapAsGeminiResponse(innerJson);

        Map<String, String> result = parse(geminiResponse);

        assertThat(result).containsEntry("documentType", "BUSINESS_REGISTRATION");
        assertThat(result).containsEntry("rccmNumber", "RC/DLA/2020/B/12345");
        assertThat(result).containsEntry("companyName", "ACME SARL");
        assertThat(result).containsEntry("legalForm", "SARL");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Champs null → exclus de la map
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Champs null dans le JSON Gemini → exclus de la map résultante")
    void nullFieldsExcludedFromMap() throws Exception {
        String innerJson = "{\"documentType\":\"PASSPORT\",\"surname\":\"MARTIN\",\"givenNames\":null,\"dateOfBirth\":null,\"documentNumber\":\"AB123456\"}";
        String geminiResponse = wrapAsGeminiResponse(innerJson);

        Map<String, String> result = parse(geminiResponse);

        assertThat(result).containsEntry("documentType", "PASSPORT");
        assertThat(result).containsEntry("surname", "MARTIN");
        assertThat(result).containsEntry("documentNumber", "AB123456");
        // Les champs null ne doivent pas apparaître
        assertThat(result).doesNotContainKey("givenNames");
        assertThat(result).doesNotContainKey("dateOfBirth");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON malformé → map vide (sans exception)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("JSON malformé → map vide retournée sans exception")
    void malformedJsonReturnsEmptyMap() throws Exception {
        String malformed = "this is not json at all {broken";
        String geminiResponse = wrapAsGeminiResponse(malformed);

        Map<String, String> result = parse(geminiResponse);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Réponse Gemini vide/null → Mono de map vide")
    void emptyOcrTextReturnsEmptyMapMono() {
        StepVerifier.create(adapter.extractDocumentData(null))
                .assertNext(map -> assertThat(map).isEmpty())
                .verifyComplete();

        StepVerifier.create(adapter.extractDocumentData("   "))
                .assertNext(map -> assertThat(map).isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("Structure Gemini incorrecte (pas de candidates) → map vide")
    void missingCandidatesReturnsEmptyMap() throws Exception {
        String noCandidate = "{\"error\":\"API_ERROR\"}";

        Map<String, String> result = parse(noCandidate);

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON avec markdown code block → extrait correctement
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("JSON enveloppé dans ```json ... ``` → parsé correctement")
    void jsonWithMarkdownFenceParsed() throws Exception {
        String innerJson = "```json\\n{\\\"documentType\\\":\\\"ID_CARD\\\",\\\"surname\\\":\\\"TEST\\\"}\\n```";
        // Simulate raw Gemini text with markdown
        String geminiResponse = """
                {
                  "candidates": [{
                    "content": {
                      "parts": [{"text": "```json\\n{\\"documentType\\":\\"ID_CARD\\",\\"surname\\":\\"TEST\\"}\\n```"}]
                    }
                  }]
                }
                """;

        Map<String, String> result = parse(geminiResponse);

        assertThat(result).containsEntry("documentType", "ID_CARD");
        assertThat(result).containsEntry("surname", "TEST");
    }
}
