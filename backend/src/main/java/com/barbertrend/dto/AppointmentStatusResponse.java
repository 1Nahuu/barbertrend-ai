package com.barbertrend.dto;

import com.barbertrend.domain.Appointment;
import com.barbertrend.domain.AppointmentStatus;
import java.time.LocalDateTime;

/**
 * Estado de un turno consultado con su código público.
 *
 * <p>No devuelve el teléfono ni el nombre completo: solo lo mínimo para que
 * el cliente sepa que es suyo y cuándo tiene que venir. Devolver más
 * habilitaría a probar códigos ajenos y ver datos de otros clientes.
 */
public record AppointmentStatusResponse(
        String reference,
        AppointmentStatus status,
        String serviceName,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String customerFirstName
) {

    public static AppointmentStatusResponse from(Appointment appointment) {
        String fullName = appointment.getCustomer().getName();
        String firstName = fullName == null || fullName.isBlank()
                ? ""
                : fullName.split("\\s+")[0];

        return new AppointmentStatusResponse(
                appointment.getReference(),
                appointment.getStatus(),
                appointment.getService().getName(),
                appointment.getStartsAt(),
                appointment.getEndsAt(),
                firstName);
    }
}
