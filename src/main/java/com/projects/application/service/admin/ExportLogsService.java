package com.projects.application.service.admin;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.projects.application.port.out.VerificationLogRepositoryPort;
import com.projects.domain.model.VerificationLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Service pour l'export des logs de vérification en CSV ou PDF.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExportLogsService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CSV_HEADER = "id,date,docType,status,reason,confidence,processingTimeMs,documentNumber,holderName\n";

    private final VerificationLogRepositoryPort verificationLogRepository;

    // ─── CSV Export ───────────────────────────────────────────────────────────

    public Mono<byte[]> exportCsv(UUID organizationId, LocalDateTime startDate, LocalDateTime endDate) {
        return filterLogs(organizationId, startDate, endDate)
                .collectList()
                .map(logs -> {
                    StringBuilder sb = new StringBuilder(CSV_HEADER);
                    for (VerificationLog log : logs) {
                        sb.append(csvField(log.getId()))
                          .append(',').append(csvField(log.getDate() != null ? log.getDate().format(FMT) : ""))
                          .append(',').append(csvField(log.getDocType()))
                          .append(',').append(csvField(log.getStatus()))
                          .append(',').append(csvField(log.getReason()))
                          .append(',').append(log.getConfidence() != null ? log.getConfidence() : "")
                          .append(',').append(log.getProcessingTimeMs() != null ? log.getProcessingTimeMs() : "")
                          .append(',').append(csvField(log.getDocumentNumber()))
                          .append(',').append(csvField(log.getHolderName()))
                          .append('\n');
                    }
                    return sb.toString().getBytes(StandardCharsets.UTF_8);
                });
    }

    // ─── PDF Export ───────────────────────────────────────────────────────────

    public Mono<byte[]> exportPdf(UUID organizationId, LocalDateTime startDate, LocalDateTime endDate) {
        return filterLogs(organizationId, startDate, endDate)
                .collectList()
                .map(logs -> {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    try {
                        Document document = new Document(PageSize.A4.rotate());
                        PdfWriter.getInstance(document, out);
                        document.open();

                        // Title
                        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
                        document.add(new Paragraph("Rapport de Vérifications — " + organizationId, titleFont));
                        document.add(new Paragraph("Période : " + startDate.format(FMT) + " → " + endDate.format(FMT)));
                        document.add(new Paragraph("Nombre de logs : " + logs.size()));
                        document.add(Chunk.NEWLINE);

                        // Table
                        PdfPTable table = new PdfPTable(7);
                        table.setWidthPercentage(100f);
                        table.setWidths(new float[]{1.5f, 1.5f, 1.5f, 1f, 2f, 1f, 2f});

                        String[] headers = {"Date", "Type", "Statut", "Confiance", "Raison", "N° Document", "Titulaire"};
                        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
                        for (String h : headers) {
                            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                            cell.setBackgroundColor(new java.awt.Color(70, 130, 180));
                            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                            table.addCell(cell);
                        }

                        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
                        for (VerificationLog vl : logs) {
                            table.addCell(new Phrase(vl.getDate() != null ? vl.getDate().format(FMT) : "", cellFont));
                            table.addCell(new Phrase(safe(vl.getDocType()), cellFont));
                            table.addCell(new Phrase(safe(vl.getStatus()), cellFont));
                            table.addCell(new Phrase(vl.getConfidence() != null ? String.format("%.2f", vl.getConfidence()) : "", cellFont));
                            table.addCell(new Phrase(safe(vl.getReason()), cellFont));
                            table.addCell(new Phrase(safe(vl.getDocumentNumber()), cellFont));
                            table.addCell(new Phrase(safe(vl.getHolderName()), cellFont));
                        }
                        document.add(table);
                        document.close();
                    } catch (Exception e) {
                        log.error("[export] Erreur génération PDF : {}", e.getMessage());
                        throw new RuntimeException("Erreur lors de la génération du PDF", e);
                    }
                    return out.toByteArray();
                });
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Flux<VerificationLog> filterLogs(UUID orgId, LocalDateTime start, LocalDateTime end) {
        return verificationLogRepository.findByPlatformId(orgId)
                .filter(vl -> vl.getDate() != null
                        && !vl.getDate().isBefore(start)
                        && !vl.getDate().isAfter(end))
                .sort((a, b) -> b.getDate().compareTo(a.getDate()));
    }

    private String csvField(Object value) {
        if (value == null) return "";
        String s = value.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}
