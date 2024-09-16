package com.DiagramParcialBackend.errors.excepciones;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
