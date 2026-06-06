package com.projects.application.port.out;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port sortant pour le stockage de fichiers.
 */
public interface FileStoragePort {
    Mono<StoredFileDTO> storeFile(String fileName, String contentType, Flux<DataBuffer> content, UUID tenantId, UUID orgId, String bearerToken);
    
    record StoredFileDTO(UUID id, UUID organizationId, UUID uploadedByUserId, String fileName, String contentType, long size) {}
}
