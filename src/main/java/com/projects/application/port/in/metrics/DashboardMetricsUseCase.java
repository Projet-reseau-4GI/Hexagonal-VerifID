package com.projects.application.port.in.metrics;

import com.projects.adapter.in.web.dto.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

/**
 * Inbound port — Dashboard metrics use cases.
 */
public interface DashboardMetricsUseCase {
    Mono<DashboardStatsDto> getDashboardStats(String platformId, LocalDateTime from, LocalDateTime to, String period);
    Flux<TimeSeriesPoint> getVerificationsOverTime(String platformId, LocalDateTime from, LocalDateTime to, String granularity);
    Flux<HourlyTrafficDto> getHourlyTraffic(String platformId, LocalDateTime from, LocalDateTime to);
    Flux<StatusDistributionDto> getStatusDistribution(String platformId, LocalDateTime from, LocalDateTime to);
    Flux<DocTypeBreakdownDto> getDocTypeBreakdown(String platformId, LocalDateTime from, LocalDateTime to);
    Flux<RecentVerificationDto> getRecentVerifications(String platformId);
    Flux<DetailedVerificationDto> getSuccessfulVerifications(String platformId, LocalDateTime from, LocalDateTime to);
    Flux<UsageStatisticDto> getOverallUsageStatistics();
    Flux<UsageStatisticDto> getUsageStatisticsByPlatform(String platformId);
}
