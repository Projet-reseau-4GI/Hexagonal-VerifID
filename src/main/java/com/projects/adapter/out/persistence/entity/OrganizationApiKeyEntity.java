package com.projects.adapter.out.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("organization_api_keys")
public class OrganizationApiKeyEntity {

    @Id private Long id;
    @Column("organization_id") private String organizationId;
    @Column("api_key_hash") private String apiKeyHash;
    @Column("label") private String label;
    @Column("active") private Boolean active;
    @Column("created_at") private LocalDateTime createdAt;
}
