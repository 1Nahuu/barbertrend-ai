package com.barbertrend.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce las excepciones a respuestas HTTP.
 *
 * <p>Sin esta clase, un error de validación devolvería un 500 con una
 * traza técnica. Acá cada tipo de problema se convierte en un código
 * concreto y un mensaje que el cliente puede entender, con el mismo formato
 * siempre.
 *
 * <p>Importante: los detalles internos <b>nunca</b> se devuelven. Un
 * {@code 500} dice "algo falló" y en el log queda la traza completa; el
 * navegador no ve nombres de tablas ni de clases.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Errores de negocio: ya traen su código y su estado. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex) {
        log.info("Error de negocio {}: {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(ApiError.of(ex.getCode(), ex.getMessage(), ex.getField()));
    }

    /**
     * El cuerpo del pedido no coincide con lo esperado.
     *
     * <p>Incluye los errores de validación de los campos ({@code @Valid}).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        FieldError first = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .orElse(null);

        String field = first != null ? first.getField() : null;
        String message = first != null ? first.getDefaultMessage()
                : "Revisá los datos del formulario.";

        log.info("Validación fallida en '{}': {}", field, message);
        return ResponseEntity.badRequest()
                .body(ApiError.of("VALIDATION_ERROR", message, field));
    }

    /** JSON mal formado, o un tipo de dato que no se puede convertir. */
    @ExceptionHandler({HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception ex) {
        log.info("Petición mal formada: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiError.of("BAD_REQUEST", "Los datos enviados no son válidos."));
    }

    /**
     * Conflicto con una restricción de la base de datos.
     *
     * <p>Este es el que dispara la garantía de no superposición: cuando se
     * intenta reservar un bloque ya ocupado, la clave primaria compuesta de
     * {@code appointment_slot} viola la restricción y Hibernate lanza esto.
     *
     * <p>Se traduce a 409 con un mensaje entendible, en vez de dejar que
     * salte un 500.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        String lower = message == null ? "" : message.toLowerCase();

        // El texto exacto de la restricción depende del motor de base de
        // datos, así que se busca por palabras clave en vez de comparar
        // strings fijos.
        if (lower.contains("appointment_slot") || lower.contains("pk_appointment_slot")) {
            log.info("Choque de horario detectado por la base de datos.");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiError.of("SLOT_NOT_AVAILABLE",
                            "Ese horario acaba de ser reservado. Elegí otro."));
        }

        if (lower.contains("uq_service_name")) {
            log.info("Intento de crear un servicio con un nombre repetido.");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiError.of("DUPLICATE_SERVICE_NAME",
                            "Ya existe un servicio con ese nombre.", "name"));
        }

        log.warn("Violación de integridad no reconocida: {}", message);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of("DATA_CONFLICT",
                        "No se pudo guardar: choca con un registro existente."));
    }

    /** Cualquier otra cosa: se registra completa y se responde genérico. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Error inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("INTERNAL_ERROR",
                        "Algo falló de nuestro lado. Probá de nuevo en un momento."));
    }
}
