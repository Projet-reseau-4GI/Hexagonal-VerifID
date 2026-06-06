package com.projects.application.service.admin;

import com.projects.adapter.in.web.dto.PlatformTokenStatsDto;
import com.projects.application.port.in.admin.ManagePlatformAdminUseCase;
import com.projects.application.port.out.PlatformRepositoryPort;
import com.projects.domain.model.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Application use case — Administration des plateformes.
 * Remplace l'ancien com.projects.service.PlatformService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ManagePlatformAdminUseCaseImpl implements ManagePlatformAdminUseCase {

    private final PlatformRepositoryPort platformRepository;
    private final DatabaseClient db;

    @Override
    public Flux<Platform> getAllPlatforms() {
        return platformRepository.findAll();
    }

    @Override
    public Mono<Platform> toggleStatus(Long platformId) {
        return platformRepository.findById(platformId)
                .flatMap(platform -> {
                    boolean newState = !Boolean.TRUE.equals(platform.getActive());
                    platform.setActive(newState);
                    platform.setUpdatedAt(LocalDateTime.now());
                    log.info("Platform id={} status toggled to active={}", platformId, newState);
                    return platformRepository.save(platform);
                })
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Plateforme introuvable.")));
    }

    @Override
    public Flux<PlatformTokenStatsDto> getPlatformTokenStats(String search) {
        String searchFilter = "";
        if (search != null && !search.trim().isEmpty()) {
            String safeSearch = search.replace("'", "''");
            searchFilter = " WHERE p.name ILIKE '%" + safeSearch + "%' OR p.email ILIKE '%" + safeSearch
                    + "%' OR p.api_key ILIKE '%" + safeSearch + "%'";
        }

        String sql = "SELECT p.id, p.name, p.email, p.api_key, p.active, " +
                "       COUNT(vl.id) AS total_calls, " +
                "       MAX(vl.date) AS last_call_date " +
                "FROM platforms p " +
                "LEFT JOIN verification_logs vl ON p.id = vl.platform_id " +
                searchFilter +
                " GROUP BY p.id, p.name, p.email, p.api_key, p.active " +
                " ORDER BY p.name ASC";

        return db.sql(sql)
                .map((row, md) -> PlatformTokenStatsDto.builder()
                        .platformId(row.get("id", Long.class))
                        .platformName(row.get("name", String.class))
                        .email(row.get("email", String.class))
                        .apiKey(row.get("api_key", String.class))
                        .active(row.get("active", Boolean.class))
                        .totalCalls(row.get("total_calls", Long.class))
                        .lastCallDate(row.get("last_call_date", LocalDateTime.class))
                        .build())
                .all();
    }
}
