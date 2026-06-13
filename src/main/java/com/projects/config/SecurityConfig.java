package com.projects.config;

import com.projects.config.kernel.KernelTenantContextFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            KernelTenantContextFilter kernelTenantContextFilter,
            ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
            AdminJwtAuthenticationFilter adminJwtAuthenticationFilter) {

        return http
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(exchange -> exchange
                // Public: Swagger
                .pathMatchers(
                    "/swagger-ui.html", "/swagger-ui/**",
                    "/v3/api-docs/**", "/api-docs/**", "/webjars/**"
                ).permitAll()
                // Public: Health & métriques
                .pathMatchers("/api/metrics/**").permitAll()
                .pathMatchers("/actuator/**").permitAll()
                // Webhook Stripe
                .pathMatchers("/api/payments/webhook").permitAll()
                // SDK Snippets
                .pathMatchers("/api/sdk/**").permitAll()
                // Admin Traceability (protected by AdminJwtAuthenticationFilter)
                .pathMatchers("/api/admin/traceability/**").hasRole("SUPER_ADMIN")
                // Public: Vérification de documents (auth par X-API-KEY ou JWT kernel)
                .pathMatchers("/api/verify/**", "/api/documents/**").permitAll()
                // Public: Dashboard
                .pathMatchers("/api/dashboard/**").permitAll()
                // Any other exchange is permitted since we rely on Gateway/Kernel for external auth, 
                // and API Key filter for documents.
                .anyExchange().permitAll()
            )
            /*
             * Ordre des filtres :
             *  1. KernelTenantContextFilter  — lit X-Tenant-Id, X-Org-Id et valide le JWT RS256
             *  2. ApiKeyAuthenticationFilter — valide X-API-KEY pour l'analyse de documents
             */
            .addFilterBefore(kernelTenantContextFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterAt(adminJwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterAt(apiKeyAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }
}