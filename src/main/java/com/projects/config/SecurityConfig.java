package com.projects.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

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
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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
                                                // Tarification des forfaits (Public)
                                                .pathMatchers("/api/plans/pricing").permitAll()
                                                // Sécurité Globale pour toutes les routes Admin (Sauf Auth défini plus haut)
                                                .pathMatchers("/api/admin/**").hasRole("SUPER_ADMIN")
                                                // Tout le reste
                                                .anyExchange().permitAll())
                                /*
                                 */
                                .addFilterAt(localJwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                                .addFilterAt(adminJwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                                .addFilterAt(apiKeyAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                                .build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                // En production, il est recommandé de remplacer "*" par le domaine spécifique (ex: https://verifid.example.com)
                configuration.addAllowedOriginPattern("*");
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.addAllowedHeader("*");
                configuration.setAllowCredentials(true);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}