package com.barbertrend.dto;

import com.barbertrend.domain.Appointment;
import com.barbertrend.domain.AppointmentStatus;
import java.time.LocalDateTime;

/**
 * Turno en el listado del panel del dueño.
 *
 * <p>El teléfono va ENMASCARADO en los listados y completo solo en el
 * detalle. En un panel no hace falta ver el teléfono de 50 clientes de un
 * vistazo: se abre el turno que interesa y ahí sí está el número.
 */
public record AdminAppointmentResponse(
        Long id,
        String reference,
        AppointmentStatus status,
        String serviceName,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String customerName,
        String customerPhoneMasked,
        String notes,
        String cancellationReason
) {

    public static AdminAppointmentResponse from(Appointment appointment) {
        return new AdminAppointmentResponse(
                appointment.getId(),
                appointment.getReference(),
                appointment.getStatus(),
                appointment.getService().getName(),
                appointment.getStartsAt(),
                appointment.getEndsAt(),
                appointment.getCustomer().getName(),
                maskPhone(appointment.getCustomer().getPhone()),
                appointment.getNotes(),
                appointment.getCancellationReason());
    }

    /**
     * Enmascara un teléfono dejando ver los últimos dígitos.
     * 5491122334455 -> 54911*****55
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "";
        }
        if (phone.length() <= 4) {
            return "*".repeat(phone.length());
        }

        int visibleTail = 2;
        int visibleHead = 5;

        if (phone.length() <= visibleHead + visibleTail) {
            return phone.substring(0, 1) + "*".repeat(phone.length() - 2)
                    + phone.substring(phone.length() - 1);
        }

        return phone.substring(0, visibleHead)
                + "*".repeat(phone.length() - visibleHead - visibleTail)
                + phone.substring(phone.length() - visibleTail);
    }
}
