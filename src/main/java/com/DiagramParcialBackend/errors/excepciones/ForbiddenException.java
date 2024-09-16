package com.DiagramParcialBackend.errors.excepciones;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
