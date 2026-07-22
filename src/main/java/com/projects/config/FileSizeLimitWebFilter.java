package com.projects.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projects.adapter.out.security.SecurityUtils;
import com.projects.application.port.out.OrganizationRepositoryPort;
import com.projects.domain.model.Plan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Rejects multipart/file upload requests whose Content-Length exceeds the plan limit.
 *
 * <ul>
 *   <li>FREE    : 0 bytes — all uploads blocked</li>
 *   <li>PREMIUM : 10 MB</li>
 *   <li>MAX     : 50 MB</li>
 * </ul>
 *
 * Order -80 — runs after RateLimitWebFilter (-100) and before SecurityHeadersWebFilter (-70).
 */
@Component
@Order(-80)
@RequiredArgsConstructor
@Slf4j
public class FileSizeLimitWebFilter implements WebFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OrganizationRepositoryPort organizationRepository;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        // Only check uploads on document endpoints
        if (!path.startsWith("/api/documents/") || !HttpMethod.POST.equals(method)) {
            return chain.filter(exchange);
        }

        // Content-Length may not be set for chunked uploads — skip check if absent
        long contentLength = exchange.getRequest().getHeaders().getContentLength();
        if (contentLength <= 0) {
            return chain.filter(exchange);
        }

        String apiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            // ApiKeyAuthFilter will reject this with 401 — let it pass through
            return chain.filter(exchange);
        }

        String hashedKey = SecurityUtils.hashApiKey(apiKey.trim());

        return organizationRepository.findByApiKeyHash(hashedKey)
                .flatMap(org -> {
                    Plan plan = Plan.fromString(org.getPlan());
                    long maxBytes = plan.getMaxFileSizeBytes();

                    // FREE plan: no uploads allowed
                    if (plan == Plan.FREE || contentLength > maxBytes) {
                        log.warn("[file-size] Upload rejected for org={} plan={}: {}B > {}B",
                                org.getId(), plan.name(), contentLength, maxBytes);
                        return writePayloadTooLarge(exchange, maxBytes, plan.name());
                    }
                    return chain.filter(exchange);
                })
                .switchIfEmpty(chain.filter(exchange)) // unknown key → let ApiKeyFilter handle it
                .onErrorResume(e -> {
                    log.error("[file-size] Error checking plan: {}", e.getMessage());
                    return chain.filter(exchange); // fail open
                });
    }

    private Mono<Void> writePayloadTooLarge(ServerWebExchange exchange, long maxBytes, String plan) {
        exchange.getResponse().setStatusCode(HttpStatus.PAYLOAD_TOO_LARGE);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body;
        try {
            body = MAPPER.writeValueAsBytes(Map.of(
                    "error", "FILE_TOO_LARGE",
                    "maxSizeBytes", maxBytes,
                    "plan", plan,
                    "message", "Fichier trop volumineux pour le plan " + plan
                            + ". Taille maximale : " + (maxBytes / (1024 * 1024)) + " MB"));
        } catch (JsonProcessingException e) {
            body = "{\"error\":\"FILE_TOO_LARGE\"}".getBytes();
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
