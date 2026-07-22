package com.projects.adapter.out.persistence.mapper;

import com.projects.domain.model.VerificationLog;
import com.projects.adapter.out.persistence.entity.VerificationLogEntity;
import org.springframework.stereotype.Component;
import java.util.UUID;

/**
 * Mapper: VerificationLog domain model ↔ VerificationLogEntity (R2DBC).
 */
@Component
public class VerificationLogMapper {

    public VerificationLog toDomain(VerificationLogEntity entity) {
        if (entity == null) return null;
        return VerificationLog.builder()
            .id(entity.getId())
            .platformId(entity.getPlatformId() != null ? entity.getPlatformId().toString() : null)
            .date(entity.getDate())
            .docType(entity.getDocType())
            .status(entity.getStatus())
            .reason(entity.getReason())
            .confidence(entity.getConfidence())
            .processingTimeMs(entity.getProcessingTimeMs())
            .documentNumber(entity.getDocumentNumber())
            .holderName(entity.getHolderName())
            .dateOfBirth(entity.getDateOfBirth())
            .issueDate(entity.getIssueDate())
            .expiryDate(entity.getExpiryDate())
            .additionalFields(entity.getAdditionalFields())
            .build();
    }

    public VerificationLogEntity toEntity(VerificationLog domain) {
        if (domain == null) return null;
        VerificationLogEntity entity = new VerificationLogEntity();
        entity.setId(domain.getId());
        entity.setPlatformId(domain.getPlatformId() != null ? UUID.fromString(domain.getPlatformId()) : null);
        entity.setDate(domain.getDate());
        entity.setDocType(domain.getDocType());
        entity.setStatus(domain.getStatus());
        entity.setReason(domain.getReason());
        entity.setConfidence(domain.getConfidence());
        entity.setProcessingTimeMs(domain.getProcessingTimeMs());
        entity.setDocumentNumber(domain.getDocumentNumber());
        entity.setHolderName(domain.getHolderName());
        entity.setDateOfBirth(domain.getDateOfBirth());
        entity.setIssueDate(domain.getIssueDate());
        entity.setExpiryDate(domain.getExpiryDate());
        entity.setAdditionalFields(domain.getAdditionalFields());
        return entity;
    }
}
