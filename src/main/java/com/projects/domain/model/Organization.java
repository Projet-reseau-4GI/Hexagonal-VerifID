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

    /** UUID identique à organizationId dans le Kernel (pas de champ kernelOrgId redondant). */
    private UUID id;

    /** Email de contact de l'organisation (utilisé pour la recherche dans le Kernel). */
    private String email;

    /** Nom court (shortName depuis organization-core). */
    private String name;

    /** Nom d'affichage (displayName depuis organization-core). */
    private String displayName;

    /** URL du logo (logoUri depuis organization-core). */
    private String logoUri;

    /**
     * Plan tarifaire VerifID.
     * Par défaut FREEMIUM — ne peut être modifié que via le webhook Stripe/upgrade.
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

    public Organization() {}

    private Organization(Builder b) {
        this.id = b.id;
        this.email = b.email;
        this.name = b.name;
        this.displayName = b.displayName;
        this.logoUri = b.logoUri;
        this.plan = b.plan;
        this.dailyVerificationCount = b.dailyVerificationCount;
        this.dailyCountResetAt = b.dailyCountResetAt;
        this.createdAt = b.createdAt;
        this.lastSyncedAt = b.lastSyncedAt;
        this.apiKeyHash = b.apiKeyHash;
        this.apiKeyLabel = b.apiKeyLabel;
        this.apiKeyActive = b.apiKeyActive;
        this.apiKeyCreatedAt = b.apiKeyCreatedAt;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private String email;
        private String name;
        private String displayName;
        private String logoUri;
        private String plan = "FREEMIUM";
        private Integer dailyVerificationCount = 0;
        private LocalDateTime dailyCountResetAt;
        private LocalDateTime createdAt;
        private LocalDateTime lastSyncedAt;
        private String apiKeyHash;
        private String apiKeyLabel;
        private Boolean apiKeyActive = true;
        private LocalDateTime apiKeyCreatedAt;

        public Builder id(UUID id)                                    { this.id = id; return this; }
        public Builder email(String email)                            { this.email = email; return this; }
        public Builder name(String name)                              { this.name = name; return this; }
        public Builder displayName(String displayName)                { this.displayName = displayName; return this; }
        public Builder logoUri(String logoUri)                        { this.logoUri = logoUri; return this; }
        public Builder plan(String plan)                              { this.plan = plan; return this; }
        public Builder dailyVerificationCount(Integer c)              { this.dailyVerificationCount = c; return this; }
        public Builder dailyCountResetAt(LocalDateTime t)             { this.dailyCountResetAt = t; return this; }
        public Builder createdAt(LocalDateTime t)                     { this.createdAt = t; return this; }
        public Builder lastSyncedAt(LocalDateTime t)                  { this.lastSyncedAt = t; return this; }
        public Builder apiKeyHash(String apiKeyHash)                  { this.apiKeyHash = apiKeyHash; return this; }
        public Builder apiKeyLabel(String apiKeyLabel)                { this.apiKeyLabel = apiKeyLabel; return this; }
        public Builder apiKeyActive(Boolean apiKeyActive)             { this.apiKeyActive = apiKeyActive; return this; }
        public Builder apiKeyCreatedAt(LocalDateTime t)               { this.apiKeyCreatedAt = t; return this; }
        public Organization build()                                   { return new Organization(this); }
    }

    // Getters
    public UUID           getId()                     { return id; }
    public String         getEmail()                  { return email; }
    public String         getName()                   { return name; }
    public String         getDisplayName()            { return displayName; }
    public String         getLogoUri()                { return logoUri; }
    public String         getPlan()                   { return plan; }
    public Integer        getDailyVerificationCount() { return dailyVerificationCount; }
    public LocalDateTime  getDailyCountResetAt()      { return dailyCountResetAt; }
    public LocalDateTime  getCreatedAt()              { return createdAt; }
    public LocalDateTime  getLastSyncedAt()           { return lastSyncedAt; }
    public String         getApiKeyHash()             { return apiKeyHash; }
    public String         getApiKeyLabel()            { return apiKeyLabel; }
    public Boolean        getApiKeyActive()           { return apiKeyActive; }
    public LocalDateTime  getApiKeyCreatedAt()        { return apiKeyCreatedAt; }

    // Setters
    public void setId(UUID id)                                    { this.id = id; }
    public void setEmail(String email)                            { this.email = email; }
    public void setName(String name)                              { this.name = name; }
    public void setDisplayName(String displayName)                { this.displayName = displayName; }
    public void setLogoUri(String logoUri)                        { this.logoUri = logoUri; }
    public void setPlan(String plan)                              { this.plan = plan; }
    public void setDailyVerificationCount(Integer c)              { this.dailyVerificationCount = c; }
    public void setDailyCountResetAt(LocalDateTime t)             { this.dailyCountResetAt = t; }
    public void setCreatedAt(LocalDateTime t)                     { this.createdAt = t; }
    public void setLastSyncedAt(LocalDateTime t)                  { this.lastSyncedAt = t; }
    public void setApiKeyHash(String apiKeyHash)                  { this.apiKeyHash = apiKeyHash; }
    public void setApiKeyLabel(String apiKeyLabel)                { this.apiKeyLabel = apiKeyLabel; }
    public void setApiKeyActive(Boolean apiKeyActive)             { this.apiKeyActive = apiKeyActive; }
    public void setApiKeyCreatedAt(LocalDateTime t)               { this.apiKeyCreatedAt = t; }
}
