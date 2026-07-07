package com.projects.adapter.out.ocr;

import com.projects.application.port.out.OcrServicePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.projects.exception.ExternalServiceUnavailableException;

/**
 * Infrastructure adapter — implements OcrServicePort using the external parsing
 * API.
 */
@Component
@Slf4j
public class OcrAdapter implements OcrServicePort {

    private final WebClient webClient;

    @Value("${parsing.api.url:https://lcgfy76ct94esfka.aistudio-app.com/layout-parsing}")
    private String parsingApiUrl;

    @Value("${parsing.api.token:}")
    private String parsingApiToken;

    public OcrAdapter(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public Mono<String> extractText(byte[] content, boolean isPdf) {
        log.info("Performing in-memory OCR. Content size: {} bytes", content.length);
        String base64File = Base64.getEncoder().encodeToString(content);

        Map<String, Object> payload = new HashMap<>();
        payload.put("file", base64File);
        payload.put("fileType", isPdf ? 0 : 1);

        return webClient.post()
                .uri(parsingApiUrl)
                .header("Authorization", "token " + parsingApiToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .onStatus(status -> status.isError(), response -> response.bodyToMono(String.class).flatMap(body -> {
                    log.error("OCR API Error {}: {}", response.statusCode(), body);
                    return Mono.error(new RuntimeException("OCR API returned " + response.statusCode() + ": " + body));
                }))
                .bodyToMono(JsonNode.class)
                .timeout(java.time.Duration.ofSeconds(300))
                .map(resp -> {
                    try {
                        JsonNode result = resp.get("result");
                        if (result == null || result.isNull())
                            return "";
                        JsonNode layouts = result.get("layoutParsingResults");
                        if (layouts == null || layouts.isEmpty())
                            return "";
                        JsonNode first = layouts.get(0);
                        if (first == null || first.isNull())
                            return "";
                        JsonNode markdown = first.get("markdown");
                        if (markdown == null || markdown.isNull())
                            return "";
                        JsonNode textNode = markdown.get("text");
                        return textNode != null && !textNode.isNull() ? textNode.asText() : "";
                    } catch (Exception e) {
                        log.error("Failed to parse OCR response: {}", e.getMessage());
                        return "";
                    }
                })
                .onErrorMap(throwable -> {
                    Throwable cause = throwable;
                    if (throwable instanceof WebClientRequestException && throwable.getMessage() != null) {
                        return new ExternalServiceUnavailableException("Failed to reach parsing service: " + parsingApiUrl, throwable);
                    }
                    // unwrap nested causes (e.g., UnknownHostException)
                    while (cause.getCause() != null)
                        cause = cause.getCause();
                    if (cause instanceof java.net.UnknownHostException || cause instanceof java.net.ConnectException) {
                        return new ExternalServiceUnavailableException("Parsing service unreachable: " + parsingApiUrl + " (" + cause.getMessage() + ")", throwable);
                    }
                    return throwable;
                });
    }
}
