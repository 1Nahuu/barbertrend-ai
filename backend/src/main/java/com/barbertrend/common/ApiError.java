package com.barbertrend.common;

/**
 * Error de la API, con la información que necesita el frontend.
 *
 * @param code      identificador corto y estable, por ejemplo {@code SLOT_NOT_AVAILABLE}
 * @param message   texto que se puede mostrar al cliente, ya en español
 * @param field     campo del formulario que tiene el problema, o null
 * @param timestamp momento en que ocurrió
 */
public record ApiError(
        String code,
        String message,
        String field,
        java.time.Instant timestamp
) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null, java.time.Instant.now());
    }

    public static ApiError of(String code, String message, String field) {
        return new ApiError(code, message, field, java.time.Instant.now());
    }
}
