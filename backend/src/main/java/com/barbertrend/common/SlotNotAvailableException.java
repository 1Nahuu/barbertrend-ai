package com.barbertrend.common;

import org.springframework.http.HttpStatus;

/**
 * El horario ya está tomado.
 *
 * <p>Es el caso más normal del sistema, no una falla: dos personas quisieron
 * el mismo horario y la segunda llegó tarde. Por eso responde 409 y lleva un
 * mensaje que invita a elegir otro horario, no un error genérico.
 *
 * <p>Este error lo dispara la base de datos (violación de la clave primaria
 * de {@code appointment_slot}), no el código de aplicación. Ver
 * {@link com.barbertrend.domain.AppointmentSlot}.
 */
public class SlotNotAvailableException extends ApiException {

    public SlotNotAvailableException() {
        super("SLOT_NOT_AVAILABLE", HttpStatus.CONFLICT,
                "Ese horario acaba de ser reservado. Elegí otro.", null);
    }
}
