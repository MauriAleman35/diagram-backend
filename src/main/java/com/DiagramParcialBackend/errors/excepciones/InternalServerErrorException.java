package com.DiagramParcialBackend.errors.excepciones;

public class InternalServerErrorException extends RuntimeException {
    public InternalServerErrorException(String message) {
        super(message);
    }
}
