package com.projects.config;

import com.projects.application.service.admin.LocalJwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;

/**
 * Filtre JWT LOCAL VerifID (mode autonome — sans Kernel).
 *
 * Valide le token HS256 émis par {@link LocalJwtService} présent dans le header
 * <code>Authorization: Bearer &lt;token&gt;</code> sur les routes protégées.
 *
 * S'il est valide, injecte l'email et le rôle dans le SecurityContext de
 * Spring.
 */
@Component
@Order(2)
@Slf4j
public class LocalJwtAuthFilter implements WebFilter {

    private final Key signingKey;

    public LocalJwtAuthFilter(@Value("${jwt.secret}") String jwtSecret) {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Routes publiques : on laisse passer sans JWT
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Pas de token → on continue (d'autres filtres comme ApiKeyFilter gèrent)
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7).trim();

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            if (email != null && role != null) {
                var auth = new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                log.debug("[jwt-local] Token valide pour email={}, rôle={}", email, role);
                return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
            }

        } catch (JwtException e) {
            log.warn("[jwt-local] Token JWT invalide : {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/api-docs")
                || path.startsWith("/webjars")
                || path.startsWith("/actuator")
                || path.startsWith("/api/sdk")
                || path.startsWith("/api/payments/webhook")
                || path.equals("/api/org/auth/register")
                || path.equals("/api/org/auth/verify-email")
                || path.equals("/api/org/auth/login")
                || path.equals("/api/org/auth/initiate")
                || path.equals("/api/org/auth/verify-otp")
                || path.startsWith("/api/admin/auth")
                || path.startsWith("/api/documents")
                || path.startsWith("/api/verify")
                || path.startsWith("/api/dashboard");
    }
}
