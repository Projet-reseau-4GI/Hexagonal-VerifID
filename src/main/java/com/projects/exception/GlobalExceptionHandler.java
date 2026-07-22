package com.projects.exception;

import com.projects.adapter.in.web.dto.ErrorResponse;
import com.projects.exception.AccountSuspendedException;
import com.projects.exception.FileTooLargeException;
import com.projects.exception.OtpExpiredException;
import com.projects.exception.QuotaExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import com.projects.exception.ExternalServiceUnavailableException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        @ExceptionHandler(org.springframework.web.reactive.resource.NoResourceFoundException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleNoResourceFoundException(
                        org.springframework.web.reactive.resource.NoResourceFoundException ex,
                        ServerWebExchange exchange) {
                return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ErrorResponse.builder()
                                                .timestamp(LocalDateTime.now())
                                                .status(HttpStatus.NOT_FOUND.value())
                                                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                                                .message("Route or resource not found")
                                                .path(exchange.getRequest().getPath().value())
                                                .build()));
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleResourceNotFoundException(ResourceNotFoundException ex,
                        ServerWebExchange exchange) {
                return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ErrorResponse.builder()
                                                .timestamp(LocalDateTime.now())
                                                .status(HttpStatus.NOT_FOUND.value())
                                                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                                                .message(ex.getMessage())
                                                .path(exchange.getRequest().getPath().value())
                                                .build()));
        }

        @ExceptionHandler(EmailSendException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleEmailSendException(EmailSendException ex,
                        ServerWebExchange exchange) {
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ErrorResponse.builder()
                                                .timestamp(LocalDateTime.now())
                                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                .error("Email Service Error")
                                                .message(ex.getMessage())
                                                .path(exchange.getRequest().getPath().value())
                                                .build()));
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgumentException(IllegalArgumentException ex,
                        ServerWebExchange exchange) {
                HttpStatus status = ex.getMessage() != null && ex.getMessage().contains("déjà")
                                ? HttpStatus.CONFLICT
                                : HttpStatus.BAD_REQUEST;
                return Mono.just(ResponseEntity.status(status)
                                .body(ErrorResponse.builder()
                                                .timestamp(LocalDateTime.now())
                                                .status(status.value())
                                                .error(status.getReasonPhrase())
                                                .message(ex.getMessage() != null ? ex.getMessage()
                                                                : "Invalid request")
                                                .path(exchange.getRequest().getPath().value())
                                                .build()));
        }

        @ExceptionHandler(QuotaExceededException.class)
        public Mono<ResponseEntity<Map<String, Object>>> handleQuotaExceeded(
                        QuotaExceededException ex, ServerWebExchange exchange) {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("error", "QUOTA_EXCEEDED");
                body.put("message", ex.getMessage());
                if (ex.getQuotaStatus() != null) {
                        body.put("consumed", ex.getQuotaStatus().consumed());
                        body.put("limit", ex.getQuotaStatus().limit());
                        body.put("resetAt", ex.getQuotaStatus().resetAt() != null
                                ? ex.getQuotaStatus().resetAt().toString() : null);
                }
                return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body));
        }

        @ExceptionHandler(FileTooLargeException.class)
        public Mono<ResponseEntity<Map<String, Object>>> handleFileTooLarge(
                        FileTooLargeException ex, ServerWebExchange exchange) {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("error", "FILE_TOO_LARGE");
                body.put("maxSizeBytes", ex.getMaxSizeBytes());
                body.put("plan", ex.getPlan());
                body.put("message", ex.getMessage());
                return Mono.just(ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body));
        }

        @ExceptionHandler(AccountSuspendedException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleAccountSuspended(
                        AccountSuspendedException ex, ServerWebExchange exchange) {
                return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ErrorResponse.builder()
                                                .timestamp(LocalDateTime.now())
                                                .status(HttpStatus.FORBIDDEN.value())
                                                .error("ACCOUNT_SUSPENDED")
                                                .message(ex.getMessage())
                                                .path(exchange.getRequest().getPath().value())
                                                .build()));
        }

        @ExceptionHandler(OtpExpiredException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleOtpExpired(
                        OtpExpiredException ex, ServerWebExchange exchange) {
                return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ErrorResponse.builder()
                                                .timestamp(LocalDateTime.now())
                                                .status(HttpStatus.BAD_REQUEST.value())
                                                .error("OTP_EXPIRED")
                                                .message(ex.getMessage())
                                                .path(exchange.getRequest().getPath().value())
                                                .build()));
        }

        @ExceptionHandler(IllegalStateException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleIllegalStateException(
                        IllegalStateException ex, ServerWebExchange exchange) {
                HttpStatus status = ex.getMessage() != null && ex.getMessage().toLowerCase().contains("suspendu")
                                ? HttpStatus.FORBIDDEN
                                : HttpStatus.CONFLICT;
                return Mono.just(ResponseEntity.status(status)
                                .body(ErrorResponse.builder()
                                                .timestamp(LocalDateTime.now())
                                                .status(status.value())
                                                .error(status.getReasonPhrase())
                                                .message(ex.getMessage())
                                                .path(exchange.getRequest().getPath().value())
                                                .build()));
        }

        @ExceptionHandler(ExternalServiceUnavailableException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleExternalServiceUnavailable(ExternalServiceUnavailableException ex,
                        ServerWebExchange exchange) {
                return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                                .body(ErrorResponse.builder()
                                                .timestamp(LocalDateTime.now())
                                                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                                                .error(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase())
                                                .message(ex.getMessage())
                                                .path(exchange.getRequest().getPath().value())
                                                .build()));
        }

        @ExceptionHandler(RuntimeException.class)
        public Mono<ResponseEntity<ErrorResponse>> handleRuntimeException(RuntimeException ex,
                        ServerWebExchange exchange) {
                log.error("Unhandled runtime exception at {}: {}", exchange.getRequest().getPath(), ex.getMessage(),
                                ex);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ErrorResponse.builder()
                                                .timestamp(LocalDateTime.now())
                                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                                                .message(ex.getMessage() != null ? ex.getMessage()
                                                                : "Unknown internal server error")
                                                .path(exchange.getRequest().getPath().value())
                                                .build()));
        }

        @ExceptionHandler(Exception.class)
        public Mono<ResponseEntity<ErrorResponse>> handleGeneralException(Exception ex, ServerWebExchange exchange) {
                log.error("Unexpected error at {}: {}", exchange.getRequest().getPath(), ex.getMessage(), ex);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ErrorResponse.builder()
                                                .timestamp(LocalDateTime.now())
                                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                                .error("Unexpected Error")
                                                .message(ex.getMessage() != null ? ex.getMessage()
                                                                : "An unexpected error occurred")
                                                .path(exchange.getRequest().getPath().value())
                                                .build()));
        }
}
