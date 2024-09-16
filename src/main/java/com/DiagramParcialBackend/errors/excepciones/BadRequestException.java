package com.DiagramParcialBackend.errors.excepciones;

public class BadRequestException extends RuntimeException{
    public BadRequestException(String message) {
        super(message);
    }
}