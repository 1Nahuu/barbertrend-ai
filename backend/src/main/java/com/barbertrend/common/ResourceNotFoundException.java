package com.barbertrend.common;

import org.springframework.http.HttpStatus;

/** El recurso pedido no existe. */
public class ResourceNotFoundException extends ApiException {

    public static ResourceNotFoundException service(Long id) {
        return new ResourceNotFoundException("SERVICE_NOT_FOUND",
                "No existe el servicio " + id + ".", "serviceId");
    }

    public static ResourceNotFoundException appointment(Long id) {
        return new ResourceNotFoundException("APPOINTMENT_NOT_FOUND",
                "No existe el turno " + id + ".", null);
    }

    public static ResourceNotFoundException reference(String reference) {
        return new ResourceNotFoundException("APPOINTMENT_NOT_FOUND",
                "No encontramos ningún turno con ese código.", null);
    }

    public static ResourceNotFoundException block(Long id) {
        return new ResourceNotFoundException("BLOCK_NOT_FOUND",
                "No existe el cierre " + id + ".", null);
    }

    private ResourceNotFoundException(String code, String message, String field) {
        super(code, HttpStatus.NOT_FOUND, message, field);
    }
}
