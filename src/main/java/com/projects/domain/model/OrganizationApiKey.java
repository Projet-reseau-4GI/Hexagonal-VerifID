package com.projects.domain.model;

import java.time.LocalDateTime;

public class OrganizationApiKey {
    private Long id;
    private String organizationId;
    private String apiKeyHash;
    private String label;
    private Boolean active;
    private LocalDateTime createdAt;

    public OrganizationApiKey() {}

    private OrganizationApiKey(Builder b) {
        this.id = b.id;
        this.organizationId = b.organizationId;
        this.apiKeyHash = b.apiKeyHash;
        this.label = b.label;
        this.active = b.active;
        this.createdAt = b.createdAt;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String organizationId;
        private String apiKeyHash;
        private String label;
        private Boolean active;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder organizationId(String organizationId) { this.organizationId = organizationId; return this; }
        public Builder apiKeyHash(String apiKeyHash) { this.apiKeyHash = apiKeyHash; return this; }
        public Builder label(String label) { this.label = label; return this; }
        public Builder active(Boolean active) { this.active = active; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public OrganizationApiKey build() { return new OrganizationApiKey(this); }
    }

    public Long getId() { return id; }
    public String getOrganizationId() { return organizationId; }
    public String getApiKeyHash() { return apiKeyHash; }
    public String getLabel() { return label; }
    public Boolean getActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
    public void setApiKeyHash(String apiKeyHash) { this.apiKeyHash = apiKeyHash; }
    public void setLabel(String label) { this.label = label; }
    public void setActive(Boolean active) { this.active = active; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
