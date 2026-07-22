package com.projects.exception;

import com.projects.domain.model.QuotaStatus;

/**
 * Thrown when an organization has exhausted its daily verification quota.
 */
public class QuotaExceededException extends RuntimeException {

    private final QuotaStatus quotaStatus;

    public QuotaExceededException(QuotaStatus quotaStatus) {
        super("Quota journalier dépassé pour le plan " + quotaStatus.plan());
        this.quotaStatus = quotaStatus;
    }

    public QuotaExceededException(String message) {
        super(message);
        this.quotaStatus = null;
    }

    public QuotaStatus getQuotaStatus() {
        return quotaStatus;
    }
}
