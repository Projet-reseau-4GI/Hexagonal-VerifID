package com.projects.config;

import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Injecte les headers de sécurité HTTP sur toutes les réponses.
 *
 * <ul>
 *   <li>{@code X-Content-Type-Options: nosniff}</li>
 *   <li>{@code X-Frame-Options: DENY}</li>
 *   <li>{@code Strict-Transport-Security: max-age=31536000; includeSubDomains}</li>
 *   <li>{@code X-XSS-Protection: 1; mode=block}</li>
 *   <li>{@code Cache-Control: no-store}</li>
 * </ul>
 *
 * Order -70 — runs after RateLimitWebFilter (-100) and ApiKeyAuthFilter (-90).
 */
@Component
@Order(-70)
public class SecurityHeadersWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();
        // Headers are set before the downstream handler writes the body
        response.getHeaders().set("X-Content-Type-Options", "nosniff");
        response.getHeaders().set("X-Frame-Options", "DENY");
        response.getHeaders().set("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        response.getHeaders().set("X-XSS-Protection", "1; mode=block");
        response.getHeaders().set("Cache-Control", "no-store");
        return chain.filter(exchange);
    }
}
