package com.projects.adapter.out.kernel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing an organization returned from the KSM Kernel Auth Core.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KernelOrgResponse {

    private UUID id;
    private String shortName;
    private String longName;
    private String displayName;
    private String email;
    private String logoUri;
    private String status;
    private String code;
    private String service;
    private Boolean isIndividualBusiness;
    private String websiteUrl;
    private String businessRegistrationNumber;
    private String taxNumber;
    private String ceoName;
    private String organizationType;
    private String legalForm;
    private Boolean isActive;

}
