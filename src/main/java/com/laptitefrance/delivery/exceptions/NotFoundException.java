package com.laptitefrance.delivery.exceptions;

/**
 * Recurso/entidad no encontrada.
 */
public class NotFoundException extends DomainException {

    public NotFoundException() {
        super();
    }

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

