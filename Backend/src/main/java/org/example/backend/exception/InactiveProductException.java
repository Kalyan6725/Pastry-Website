package org.example.backend.exception;

public class InactiveProductException extends RuntimeException {
    public InactiveProductException(String message) {
        super(message);
    }
}
