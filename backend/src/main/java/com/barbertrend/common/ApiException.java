package com.barbertrend.common;

import org.springframework.http.HttpStatus;

/**
 * Error que se puede convertir directamente en una respuesta de la API.
 *
 * <p>Las clases hijas definen el código y el estado HTTP, de modo que
 * cada regla de negocio declara su próprio error sin repetir el texto ni el
 * número de estado en varias partes del código.
 */
public abstract class ApiException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final String field;

    protected ApiException(String code, HttpStatus status, String message, String field) {
        super(message);
        this.code = code;
        this.status = status;
        this.field = field;
    }

    /** Código estable para el frontend, por ejemplo {@code SLOT_NOT_AVAILABLE}. */
    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** Campo del formulario con el problema, o null. */
    public String getField() {
        return field;
    }
}
