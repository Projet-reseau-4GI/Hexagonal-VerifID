package com.projects.application.port.in.admin;

import com.projects.adapter.in.web.dto.PlatformTokenStatsDto;
import com.projects.domain.model.Platform;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Port entrant — Administration des plateformes (super-admin).
 */
public interface ManagePlatformAdminUseCase {
    Flux<Platform> getAllPlatforms();
    Mono<Platform> toggleStatus(Long platformId);
    Flux<PlatformTokenStatsDto> getPlatformTokenStats(String search);
}
