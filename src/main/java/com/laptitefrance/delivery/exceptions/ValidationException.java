package com.laptitefrance.delivery.exceptions;

/**
 * Errores por validaciones de entrada o reglas del dominio.
 */
public class ValidationException extends DomainException {

    public ValidationException() {
        super();
    }

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

