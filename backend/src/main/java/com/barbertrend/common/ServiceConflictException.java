package com.barbertrend.common;

import org.springframework.http.HttpStatus;

/**
 * El servicio no se puede guardar porque choca con otro existente.
 *
 * <p>Se valida antes de llegar a la base para dar un mensaje claro. La
 * restricción única de la tabla queda igual como última defensa.
 */
public class ServiceConflictException extends ApiException {

    public static ServiceConflictException duplicateName() {
        return new ServiceConflictException();
    }

    private ServiceConflictException() {
        super("DUPLICATE_SERVICE_NAME", HttpStatus.CONFLICT,
                "Ya existe un servicio con ese nombre.", "name");
    }
}
