package com.projects.application.service.admin;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * Service JWT local VerifID (HS256).
 *
 * Génère et valide les tokens JWT internes sans dépendance au Kernel.
 * Utilisé pour l'authentification des organisations (rôle ORG)
 * et des administrateurs (rôle SUPER_ADMIN / ADMIN).
 *
 * Durée par défaut : 24h (configurable via jwt.expiration-hours).
 */
@Service
public class LocalJwtService {

    private final Key key;
    private final long expirationMs;

    public LocalJwtService(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration-hours:24}") long expirationHours) {

        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalArgumentException("JWT_SECRET doit contenir au moins 32 caractères");
        }
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationHours * 60 * 60 * 1000L;
    }

    /**
     * Génère un JWT signé avec le sujet (email) et le rôle.
     *
     * @param subject email de l'organisation ou de l'admin
     * @param role    "ORG", "SUPER_ADMIN", "ADMIN"
     * @return JWT signé
     */
    public String generateToken(String subject, String role) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extrait et valide les claims d'un token JWT.
     *
     * @param token JWT brut (sans le préfixe "Bearer ")
     * @return Claims Java JWT
     */
    public Claims validateAndExtract(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
