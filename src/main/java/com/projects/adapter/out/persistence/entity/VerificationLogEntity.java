package com.projects.adapter.out.persistence.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * R2DBC persistence entity for VerificationLog.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("verification_logs")
public class VerificationLogEntity {

    @Id private Long id;
    @Column("platform_id") private UUID platformId;
    @Column("date") private LocalDateTime date;
    @Column("doc_type") private String docType;
    @Column("status") private String status;
    @Column("reason") private String reason;
    @Column("confidence") private Double confidence;
    @Column("processing_time_ms") private Integer processingTimeMs;
    @Column("document_number") private String documentNumber;
    @Column("holder_name") private String holderName;
    @Column("date_of_birth") private String dateOfBirth;
    @Column("issue_date") private String issueDate;
    @Column("expiry_date") private String expiryDate;
    @Column("additional_fields") private String additionalFields;

    public String getDateOfBirth() { return dateOfBirth; }
    public String getIssueDate() { return issueDate; }
    public String getExpiryDate() { return expiryDate; }
    public String getAdditionalFields() { return additionalFields; }

    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public void setIssueDate(String issueDate) { this.issueDate = issueDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
    public void setAdditionalFields(String additionalFields) { this.additionalFields = additionalFields; }
}
