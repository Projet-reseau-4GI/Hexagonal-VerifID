package com.projects.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modèle domaine représentant une organisation VerifID.
 *
 * L'ID est identique à l'UUID retourné par le Kernel (organization-core).
 * C'est le schéma de BD répartie : une seule source de vérité côté Kernel,
 * enrichie ici par des attributs propres à VerifID (plan, quota, etc.).
 */
public class Organization {

    /**
     * UUID identique à organizationId dans le Kernel (pas de champ kernelOrgId
     * redondant).
     */
    private UUID id;

    /**
     * Email de contact de l'organisation (utilisé pour la recherche dans le
     * Kernel).
     */
    private String email;

    /** Nom du développeur. */
    private String developerName;

    /** Nom de l'organisation (optionnel). */
    private String organizationName;

    /**
     * Plan tarifaire VerifID.
     * Par défaut FREE — ne peut être modifié que via le webhook Stripe/upgrade.
     */
    private String plan;

    /** Nombre de vérifications effectuées dans la fenêtre courante (24h). */
    private Integer dailyVerificationCount;

    /** Date de la dernière réinitialisation du compteur quotidien. */
    private LocalDateTime dailyCountResetAt;

    /** Date de première synchronisation depuis le Kernel. */
    private LocalDateTime createdAt;

    /** Date de dernière synchronisation des attributs Kernel. */
    private LocalDateTime lastSyncedAt;

    /** Hash de la clé API de l'organisation. */
    private String apiKeyHash;

    /** Label de la clé API. */
    private String apiKeyLabel;

    /** Statut de la clé API. */
    private Boolean apiKeyActive;

    /** Date de création de la clé API. */
    private LocalDateTime apiKeyCreatedAt;

    /** Hash BCrypt du mot de passe (auth locale, sans Kernel). */
    private String passwordHash;

    /** Email vérifié via OTP. */
    private Boolean isEmailVerified;

    /** Statut de l'organisation : PENDING, ACTIVE, SUSPENDED. */
    private String status;

    /** Code OTP temporaire (inscription ou mot de passe oublié). */
    private String otpCode;

    /** Date/heure d'expiration de l'OTP. */
    private java.time.LocalDateTime otpExpiry;

    /** Identifiant client unique auto-généré lors de l'inscription. */
    private String clientId;

    public Organization() {
    }

    private Organization(Builder b) {
        this.id = b.id;
        this.email = b.email;
        this.developerName = b.developerName;
        this.organizationName = b.organizationName;
        this.plan = b.plan;
        this.dailyVerificationCount = b.dailyVerificationCount;
        this.dailyCountResetAt = b.dailyCountResetAt;
        this.createdAt = b.createdAt;
        this.lastSyncedAt = b.lastSyncedAt;
        this.apiKeyHash = b.apiKeyHash;
        this.apiKeyLabel = b.apiKeyLabel;
        this.apiKeyActive = b.apiKeyActive;
        this.apiKeyCreatedAt = b.apiKeyCreatedAt;
        this.passwordHash = b.passwordHash;
        this.isEmailVerified = b.isEmailVerified;
        this.status = b.status;
        this.otpCode = b.otpCode;
        this.otpExpiry = b.otpExpiry;
        this.clientId = b.clientId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String email;
        private String developerName;
        private String organizationName;
        private String plan = "FREE";
        private Integer dailyVerificationCount = 0;
        private LocalDateTime dailyCountResetAt;
        private LocalDateTime createdAt;
        private LocalDateTime lastSyncedAt;
        private String apiKeyHash;
        private String apiKeyLabel;
        private Boolean apiKeyActive = true;
        private LocalDateTime apiKeyCreatedAt;
        private String passwordHash;
        private Boolean isEmailVerified = false;
        private String status = "PENDING";
        private String otpCode;
        private LocalDateTime otpExpiry;
        private String clientId;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder developerName(String developerName) {
            this.developerName = developerName;
            return this;
        }

        public Builder organizationName(String organizationName) {
            this.organizationName = organizationName;
            return this;
        }

        public Builder plan(String plan) {
            this.plan = plan;
            return this;
        }

        public Builder dailyVerificationCount(Integer c) {
            this.dailyVerificationCount = c;
            return this;
        }

        public Builder dailyCountResetAt(LocalDateTime t) {
            this.dailyCountResetAt = t;
            return this;
        }

        public Builder createdAt(LocalDateTime t) {
            this.createdAt = t;
            return this;
        }

        public Builder lastSyncedAt(LocalDateTime t) {
            this.lastSyncedAt = t;
            return this;
        }

        public Builder apiKeyHash(String apiKeyHash) {
            this.apiKeyHash = apiKeyHash;
            return this;
        }

        public Builder apiKeyLabel(String apiKeyLabel) {
            this.apiKeyLabel = apiKeyLabel;
            return this;
        }

        public Builder apiKeyActive(Boolean apiKeyActive) {
            this.apiKeyActive = apiKeyActive;
            return this;
        }

        public Builder apiKeyCreatedAt(LocalDateTime t) {
            this.apiKeyCreatedAt = t;
            return this;
        }

        public Builder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder isEmailVerified(Boolean v) {
            this.isEmailVerified = v;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder otpCode(String otpCode) {
            this.otpCode = otpCode;
            return this;
        }

        public Builder otpExpiry(LocalDateTime t) {
            this.otpExpiry = t;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Organization build() {
            return new Organization(this);
        }
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDeveloperName() {
        return developerName;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public String getPlan() {
        return plan;
    }

    public Integer getDailyVerificationCount() {
        return dailyVerificationCount;
    }

    public LocalDateTime getDailyCountResetAt() {
        return dailyCountResetAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public String getApiKeyHash() {
        return apiKeyHash;
    }

    public String getApiKeyLabel() {
        return apiKeyLabel;
    }

    public Boolean getApiKeyActive() {
        return apiKeyActive;
    }

    public LocalDateTime getApiKeyCreatedAt() {
        return apiKeyCreatedAt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Boolean getIsEmailVerified() {
        return isEmailVerified;
    }

    public String getStatus() {
        return status;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public LocalDateTime getOtpExpiry() {
        return otpExpiry;
    }

    public String getClientId() {
        return clientId;
    }

    // Setters
    public void setId(UUID id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setDeveloperName(String developerName) {
        this.developerName = developerName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public void setDailyVerificationCount(Integer c) {
        this.dailyVerificationCount = c;
    }

    public void setDailyCountResetAt(LocalDateTime t) {
        this.dailyCountResetAt = t;
    }

    public void setCreatedAt(LocalDateTime t) {
        this.createdAt = t;
    }

    public void setLastSyncedAt(LocalDateTime t) {
        this.lastSyncedAt = t;
    }

    public void setApiKeyHash(String apiKeyHash) {
        this.apiKeyHash = apiKeyHash;
    }

    public void setApiKeyLabel(String apiKeyLabel) {
        this.apiKeyLabel = apiKeyLabel;
    }

    public void setApiKeyActive(Boolean apiKeyActive) {
        this.apiKeyActive = apiKeyActive;
    }

    public void setApiKeyCreatedAt(LocalDateTime t) {
        this.apiKeyCreatedAt = t;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setIsEmailVerified(Boolean v) {
        this.isEmailVerified = v;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public void setOtpExpiry(LocalDateTime t) {
        this.otpExpiry = t;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
