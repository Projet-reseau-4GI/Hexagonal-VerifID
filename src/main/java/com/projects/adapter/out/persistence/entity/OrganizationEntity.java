package com.projects.adapter.out.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité R2DBC pour la table `organizations`.
 *
 * L'ID est l'UUID retourné par le Kernel (organization-core).
 * Schéma de BD répartie : même UUID que dans le Kernel, pas de clé étrangère
 * locale.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("organizations")
public class OrganizationEntity {

    /**
     * UUID identique à l'organizationId dans le Kernel (pas de champ redondant).
     */
    @Id
    @Column("id")
    private UUID id;

    @Column("email")
    private String email;

    /** Nom court (shortName du Kernel). */
    @Column("name")
    private String name;

    /** Nom d'affichage (displayName du Kernel). */
    @Column("display_name")
    private String displayName;

    /** URL du logo de l'organisation (logoUri du Kernel). */
    @Column("logo_uri")
    private String logoUri;

    /**
     * Plan tarifaire VerifID.
     * Initialisé à FREEMIUM lors de la première synchronisation.
     * Ne peut être mis à jour que par le webhook Stripe ou un admin VerifID.
     */
    @Column("plan")
    private String plan;

    /** Nombre de vérifications effectuées dans la fenêtre quotidienne en cours. */
    @Column("daily_verification_count")
    private Integer dailyVerificationCount;

    /** Horodatage de la dernière réinitialisation du compteur quotidien. */
    @Column("daily_count_reset_at")
    private LocalDateTime dailyCountResetAt;

    /** Date de première synchronisation depuis le Kernel. */
    @Column("created_at")
    private LocalDateTime createdAt;

    /** Date de dernière mise à jour des attributs Kernel. */
    @Column("last_synced_at")
    private LocalDateTime lastSyncedAt;

    /** Hash de la clé API. */
    @Column("api_key_hash")
    private String apiKeyHash;

    /** Label de la clé API. */
    @Column("api_key_label")
    private String apiKeyLabel;

    /** Statut d'activation de la clé API. */
    @Column("api_key_active")
    private Boolean apiKeyActive;

    /** Date de création de la clé API. */
    @Column("api_key_created_at")
    private LocalDateTime apiKeyCreatedAt;

    /** Hash BCrypt du mot de passe (auth locale). */
    @Column("password_hash")
    private String passwordHash;

    /** Indique si l'email a été vérifié via OTP. */
    @Column("is_email_verified")
    private Boolean isEmailVerified;

    /** Statut : PENDING, ACTIVE, SUSPENDED. */
    @Column("status")
    private String status;

    /** Code OTP temporaire. */
    @Column("otp_code")
    private String otpCode;

    /** Expiration de l'OTP. */
    @Column("otp_expiry")
    private LocalDateTime otpExpiry;

    /** Identifiant client unique généré à l'inscription. */
    @Column("client_id")
    private String clientId;
}
