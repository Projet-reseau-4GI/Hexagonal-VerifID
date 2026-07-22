package com.projects.exception;

/**
 * Thrown when an uploaded file exceeds the size limit for the organization's plan.
 */
public class FileTooLargeException extends RuntimeException {

    private final long maxSizeBytes;
    private final String plan;

    public FileTooLargeException(long maxSizeBytes, String plan) {
        super("Fichier trop volumineux pour le plan " + plan
                + ". Taille maximale : " + (maxSizeBytes / (1024 * 1024)) + " MB");
        this.maxSizeBytes = maxSizeBytes;
        this.plan = plan;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public String getPlan() {
        return plan;
    }
}
