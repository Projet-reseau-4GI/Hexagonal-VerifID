package com.projects.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgAuthResponse {

    private String token;            // Le JWT retourné par le Kernel
    private UUID organizationId;     // UUID de l'organisation dans le Kernel
    private String organizationName; // Nom d'affichage
    private String email;            // Email de l'utilisateur
    private String plan;             // Plan (ex: FREEMIUM)
    private String logoUri;          // Photo de profil / logo
}
