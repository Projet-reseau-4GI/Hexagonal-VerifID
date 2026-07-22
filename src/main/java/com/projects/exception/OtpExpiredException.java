package com.projects.exception;

/**
 * Thrown when an OTP code has expired.
 */
public class OtpExpiredException extends RuntimeException {

    public OtpExpiredException() {
        super("Code OTP expiré. Veuillez recommencer l'inscription.");
    }

    public OtpExpiredException(String message) {
        super(message);
    }
}
