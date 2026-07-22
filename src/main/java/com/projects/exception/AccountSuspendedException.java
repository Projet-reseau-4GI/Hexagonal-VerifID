package com.projects.exception;

/**
 * Thrown when a suspended account attempts to authenticate.
 */
public class AccountSuspendedException extends RuntimeException {

    public AccountSuspendedException() {
        super("Compte suspendu ou désactivé.");
    }

    public AccountSuspendedException(String message) {
        super(message);
    }
}
