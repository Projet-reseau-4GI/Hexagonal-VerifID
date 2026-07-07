package com.projects.config;

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
                        ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
                        AdminJwtAuthenticationFilter adminJwtAuthenticationFilter,
                        LocalJwtAuthFilter localJwtAuthFilter) {

                return http
                                .csrf(csrf -> csrf.disable())
                                .authorizeExchange(exchange -> exchange
                                                // Swagger / docs
                                                .pathMatchers(
                                                                "/swagger-ui.html", "/swagger-ui/**",
                                                                "/v3/api-docs/**", "/api-docs/**", "/webjars/**")
                                                .permitAll()
                                                // Santé & métriques
                                                .pathMatchers("/actuator/**").permitAll()
                                                // SDK
                                                .pathMatchers("/api/sdk/**").permitAll()
                                                // Webhook paiement
                                                .pathMatchers("/api/payments/webhook").permitAll()
                                                // Auth organisation (inscription, connexion, OTP)
                                                .pathMatchers(
                                                                "/api/org/auth/register",
                                                                "/api/org/auth/verify-email",
                                                                "/api/org/auth/login",
                                                                "/api/org/auth/initiate",
                                                                "/api/org/auth/verify-otp")
                                                .permitAll()
                                                // Auth admin
                                                .pathMatchers("/api/admin/auth/**").permitAll()
                                                // Vérification de documents (protégée par X-API-KEY via
                                                // ApiKeyAuthenticationFilter)
                                                .pathMatchers("/api/documents/**", "/api/verify/**").permitAll()
                                                // Dashboard
                                                .pathMatchers("/api/dashboard/**").permitAll()
                                                // Super Admin traceability → rôle requis
                                                .pathMatchers("/api/admin/traceability/**").hasRole("SUPER_ADMIN")
                                                // Tout le reste
                                                .anyExchange().permitAll())
                                /*
                                 */
                                .addFilterAt(localJwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                                .addFilterAt(adminJwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                                .addFilterAt(apiKeyAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                                .build();
        }
}