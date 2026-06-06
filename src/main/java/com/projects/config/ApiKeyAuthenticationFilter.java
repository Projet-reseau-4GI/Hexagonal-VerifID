package com.projects.config;

import com.projects.adapter.out.security.SecurityUtils;
import com.projects.application.port.out.PlatformRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Filtre WebFlux qui valide la clé API pour les endpoints documents.
 * Utilise PlatformRepositoryPort (hexagonal) au lieu de l'ancien PlatformRepository.
 */
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter implements WebFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";

    private final PlatformRepositoryPort platformRepository;

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
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String hashedApiKey = SecurityUtils.hashApiKey(apiKey);

        return platformRepository.findByApiKey(hashedApiKey)
                .filter(platform -> Boolean.TRUE.equals(platform.getActive()))
                .flatMap(platform ->
                     chain.filter(exchange)
                          .contextWrite(ctx -> ReactiveTenantContext.putPlatform(ctx, platform))
                )
                .switchIfEmpty(Mono.defer(() -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }));
    }
}
