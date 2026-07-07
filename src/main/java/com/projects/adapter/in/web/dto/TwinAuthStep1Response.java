package com.projects.adapter.in.web.dto;

import com.projects.adapter.out.kernel.dto.KernelOrgResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for the first step of the twin authentication process.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Réponse contenant le token Kernel temporaire et la liste des organisations du propriétaire")
public class TwinAuthStep1Response {

    @Schema(description = "Jeton d'accès Kernel (temporaire pour cette session)", example = "eyJhbG...")
    private String kernelToken;

    @Schema(description = "Liste des organisations appartenant à l'utilisateur")
    private List<KernelOrgResponse> availableOrganizations;
}
