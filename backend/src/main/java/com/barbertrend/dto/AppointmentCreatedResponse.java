package com.barbertrend.dto;

import com.barbertrend.domain.Appointment;
import com.barbertrend.domain.AppointmentStatus;
import java.time.LocalDateTime;

/**
 * Confirmación de una reserva recién creada.
 *
 * <p>Devuelve el código público para que el cliente pueda consultar su turno
 * después sin iniciar sesión y sin exponer los datos de otros.
 */
public record AppointmentCreatedResponse(
        String reference,
        AppointmentStatus status,
        String serviceName,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String message
) {

    public static AppointmentCreatedResponse from(Appointment appointment) {
        return new AppointmentCreatedResponse(
                appointment.getReference(),
                appointment.getStatus(),
                appointment.getService().getName(),
                appointment.getStartsAt(),
                appointment.getEndsAt(),
                "Recibimos tu pedido. Te confirmamos el turno a la brevedad.");
    }
}
