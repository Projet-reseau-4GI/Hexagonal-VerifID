package com.projects.adapter.out.persistence.adapter;

import com.projects.application.port.out.FileStoragePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Implémentation locale pour stocker les fichiers de vérification.
 * Pour le moment, il simule le stockage ou effectue des requêtes basiques.
 * Remplace l'ancien KernelFileService.
 */
@Service
@Slf4j
public class LocalFileStorageAdapter implements FileStoragePort {

    @Override
    public Mono<StoredFileDTO> storeFile(String fileName, String contentType, Flux<DataBuffer> content, UUID tenantId,
            UUID orgId, String bearerToken) {
        log.info("[Local Storage] Mock upload for file {}. Not genuinely storing bytes to disk.", fileName);

        // Simuler qu'on a bien reçu le flux
        return content.count().map(chunks -> {
            log.info("Finished reading file chunks: {}", chunks);
            return new StoredFileDTO(
                    UUID.randomUUID(),
                    orgId,
                    null,
                    fileName,
                    contentType,
                    1024L);
        });
    }

    @Override
    public Mono<byte[]> downloadContent(UUID fileId, UUID tenantId, UUID orgId) {
        log.warn("[Local Storage] Download is not supported locally yet for file {}.", fileId);
        return Mono.error(new UnsupportedOperationException("Local file download not implemented"));
    }
}
