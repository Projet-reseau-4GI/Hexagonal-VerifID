package com.projects.config;

import com.projects.adapter.out.security.SecurityUtils;
import com.projects.application.port.out.OrganizationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Filtre WebFlux qui valide la clé API pour les endpoints documents.
 * Retourne HTTP 401 avec un corps JSON si la clé est absente, invalide ou inactive.
 */
@Component
@Order(-90)
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter implements WebFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final byte[] INVALID_KEY_BODY = "{\"error\":\"INVALID_API_KEY\",\"message\":\"Missing or invalid API key\"}".getBytes();

    private final OrganizationRepositoryPort apiKeyRepository;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (!path.startsWith("/api/kernel/") &&
            !path.startsWith("/api/documents/")) {
            return chain.filter(exchange);
        }

        String apiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);
        if (apiKey != null) {
            apiKey = apiKey.trim();
        }

        if (apiKey == null || apiKey.isEmpty()) {
            return writeUnauthorized(exchange);
        }

        String hashedApiKey = SecurityUtils.hashApiKey(apiKey);

        return apiKeyRepository.findByApiKeyHash(hashedApiKey)
                .filter(org -> Boolean.TRUE.equals(org.getApiKeyActive()))
                .flatMap(org ->
                     chain.filter(exchange)
                          .contextWrite(ctx -> ReactiveTenantContext
                              .putOrganizationId(ctx, org.getId().toString()))
                )
                .switchIfEmpty(Mono.defer(() -> writeUnauthorized(exchange)));
    }

    private Mono<Void> writeUnauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(INVALID_KEY_BODY);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
