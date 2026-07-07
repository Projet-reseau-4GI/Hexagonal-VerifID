package com.projects.adapter.out.kernel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper DTO for API responses returning a list of organizations from the Kernel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KernelListOrgApiResponse {

    private Boolean success;
    private List<KernelOrgResponse> data;
    private String message;
    private String errorCode;
    private String timestamp;

}
