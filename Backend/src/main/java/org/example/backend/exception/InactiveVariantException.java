package org.example.backend.exception;

public class InactiveVariantException extends RuntimeException {
    public InactiveVariantException(String message) {
        super(message);
    }
}
