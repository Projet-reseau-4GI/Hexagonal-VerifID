package com.projects.application.port.out;

import com.projects.adapter.out.kernel.dto.KernelOrgResponse;
import com.projects.adapter.out.kernel.dto.KernelTokenResponse;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Port for communication with the KSM Kernel Authentication Core.
 *
 * This port defines the operations required to authenticate users against the
 * centralized Kernel service and retrieve their associated organizations.
 */
public interface KernelAuthPort {

    /**
     * Authenticates a user with the Kernel using their email and password.
     *
     * @param email The user's email address.
     * @param password The user's password.
     * @return A Mono emitting the token response from the Kernel.
     */
    Mono<KernelTokenResponse> login(String email, String password);

    /**
     * Retrieves the list of organizations owned by the authenticated user.
     *
     * @param kernelToken The access token retrieved from the Kernel login.
     * @return A Mono emitting a list of organizations.
     */
    Mono<List<KernelOrgResponse>> getMyOrganizations(String kernelToken);
}
