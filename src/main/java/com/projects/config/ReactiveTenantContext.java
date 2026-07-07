package com.projects.config;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * Context helper for the reactive Spring WebFlux environment.
 *
 * Stocke et récupère :
 * - L'Organization ID locale (via X-API-KEY ou local JWT)
 */
public class ReactiveTenantContext {

    public static final String TENANT_KEY = "CURRENT_PLATFORM_TENANT";

    // ----------------------------------------------------------------
    // Platform locale
    // ----------------------------------------------------------------

    public static Context putOrganizationId(Context context, String organizationId) {
        return context.put(TENANT_KEY, organizationId);
    }

    public static Mono<String> getOrganizationId() {
        return Mono.deferContextual(ctx -> ctx.hasKey(TENANT_KEY) ? Mono.just(ctx.get(TENANT_KEY)) : Mono.empty());
    }
}
