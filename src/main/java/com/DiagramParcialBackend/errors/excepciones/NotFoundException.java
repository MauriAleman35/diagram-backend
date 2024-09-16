package com.DiagramParcialBackend.errors.excepciones;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
