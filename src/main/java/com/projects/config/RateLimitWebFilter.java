package com.projects.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Rate limiting filter using Redis sliding window counter per API key.
 *
 * <p>Algorithm: for each incoming request bearing an X-API-KEY header on
 * protected paths, we INCR a Redis key scoped to the (hashed key, current epoch
 * second). A 2-second TTL ensures the window slides naturally. Requests beyond
 * the configured limit receive HTTP 429.</p>
 *
 * <p>Order -100 ensures this runs before ApiKeyAuthenticationFilter (-90).</p>
 */
@Component
@Order(-100)
@Slf4j
public class RateLimitWebFilter implements WebFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ReactiveStringRedisTemplate redisTemplate;
    private final int rateLimitPerSecond;

    public RateLimitWebFilter(
            ReactiveStringRedisTemplate redisTemplate,
            @Value("${app.security.rate-limit-per-second:10}") int rateLimitPerSecond) {
        this.redisTemplate = redisTemplate;
        this.rateLimitPerSecond = rateLimitPerSecond;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Only rate-limit document verification endpoints
        if (!path.startsWith("/api/documents/") && !path.startsWith("/api/kernel/")) {
            return chain.filter(exchange);
        }

        String apiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            // Let ApiKeyAuthenticationFilter handle the missing key (will return 401)
            return chain.filter(exchange);
        }

        String hashedKey = com.projects.adapter.out.security.SecurityUtils.hashApiKey(apiKey.trim());
        long epochSecond = Instant.now().getEpochSecond();
        String redisKey = "rate:" + hashedKey + ":" + epochSecond;

        return redisTemplate.opsForValue()
                .increment(redisKey)
                .flatMap(count -> {
                    if (count == 1L) {
                        // First request in this second — set TTL of 2 s (sliding window)
                        return redisTemplate.expire(redisKey, Duration.ofSeconds(2))
                                .thenReturn(count);
                    }
                    return Mono.just(count);
                })
                .flatMap(count -> {
                    if (count > rateLimitPerSecond) {
                        log.warn("[rate-limit] API key {} exceeded rate limit ({}/s) — 429",
                                hashedKey.substring(0, 8) + "...", rateLimitPerSecond);
                        return writeTooManyRequests(exchange, rateLimitPerSecond);
                    }
                    return chain.filter(exchange);
                })
                .onErrorResume(e -> {
                    // Redis unavailable — fail open (let request through)
                    log.error("[rate-limit] Redis error, failing open: {}", e.getMessage());
                    return chain.filter(exchange);
                });
    }

    private Mono<Void> writeTooManyRequests(ServerWebExchange exchange, int limit) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body;
        try {
            body = MAPPER.writeValueAsBytes(Map.of(
                    "error", "RATE_LIMIT_EXCEEDED",
                    "message", "Too many requests. Limit: " + limit + " req/s",
                    "retryAfterSeconds", 1));
        } catch (JsonProcessingException e) {
            body = "{\"error\":\"RATE_LIMIT_EXCEEDED\"}".getBytes();
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
