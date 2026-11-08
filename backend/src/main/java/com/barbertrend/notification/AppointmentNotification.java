package com.barbertrend.notification;

import com.barbertrend.domain.Appointment;
import java.time.LocalDateTime;

/**
 * Datos de un turno nuevo que se le avisan al barbero.
 *
 * <p>Es un {@code record} y no la entidad {@link Appointment} a propósito:
 * el aviso se arma dentro de la transacción que crea el turno, pero se
 * envía <b>después</b> de que esa transacción terminó. Pasar la entidad
 * obligaría a seguir accediendo a relaciones perezosas con la sesión ya
 * cerrada. Copiar los datos en el momento en que están cargados evita ese
 * problema.
 *
 * <p><b>Privacidad:</b> este objeto lleva el teléfono completo porque va
 * al email privado del barbero. Nunca se registra completo en el log: para
 * eso está {@link #maskedPhone()}.
 */
public record AppointmentNotification(
        String reference,
        String customerName,
        String customerPhone,
        String serviceName,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String notes
) {

    public static AppointmentNotification from(Appointment appointment) {
        return new AppointmentNotification(
                appointment.getReference(),
                appointment.getCustomer().getName(),
                appointment.getCustomer().getPhone(),
                appointment.getService().getName(),
                appointment.getStartsAt(),
                appointment.getEndsAt(),
                appointment.getNotes());
    }

    /** Teléfono con los dígitos del medio ocultos, para el log. */
    public String maskedPhone() {
        if (customerPhone == null || customerPhone.isBlank()) {
            return "";
        }
        if (customerPhone.length() <= 4) {
            return "*".repeat(customerPhone.length());
        }
        int tail = Math.min(2, customerPhone.length() - 1);
        return customerPhone.substring(0, 2)
                + "*".repeat(customerPhone.length() - 2 - tail)
                + customerPhone.substring(customerPhone.length() - tail);
    }

    /** Primer nombre, para mensajes más cálidos. */
    public String firstName() {
        if (customerName == null || customerName.isBlank()) {
            return "";
        }
        return customerName.trim().split("\\s+")[0];
    }
}
