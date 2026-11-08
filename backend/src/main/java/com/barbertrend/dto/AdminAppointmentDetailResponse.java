package com.barbertrend.dto;

import com.barbertrend.domain.Appointment;
import com.barbertrend.domain.AppointmentStatus;
import com.barbertrend.notification.WhatsAppLinkBuilder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Detalle de un turno en el panel del barbero.
 *
 * <p>A diferencia del listado ({@link AdminAppointmentResponse}), acá el
 * teléfono va <b>completo</b> y se incluye el enlace de WhatsApp ya armado.
 * Es la pantalla donde el barbero necesita el número para avisarle al
 * cliente, así que no se enmascara.
 *
 * <p>{@code clientWhatsAppUrl} puede ser {@code null} si el turno está
 * cancelado o si el teléfono no se pudo interpretar. El panel muestra el
 * botón solo cuando hay enlace.
 */
public record AdminAppointmentDetailResponse(
        Long id,
        String reference,
        AppointmentStatus status,
        String serviceName,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String customerName,
        String customerPhone,
        String notes,
        String cancellationReason,
        String clientWhatsAppUrl
) {

    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm", new Locale("es", "AR"));

    public static AdminAppointmentDetailResponse from(Appointment appointment,
                                                      String businessName,
                                                      String address) {
        return new AdminAppointmentDetailResponse(
                appointment.getId(),
                appointment.getReference(),
                appointment.getStatus(),
                appointment.getService().getName(),
                appointment.getStartsAt(),
                appointment.getEndsAt(),
                appointment.getCustomer().getName(),
                appointment.getCustomer().getPhone(),
                appointment.getNotes(),
                appointment.getCancellationReason(),
                buildWhatsAppUrl(appointment, businessName, address));
    }

    private static String buildWhatsAppUrl(Appointment appointment,
                                           String businessName,
                                           String address) {
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            return null;
        }

        String message = buildMessage(appointment, businessName, address);
        return WhatsAppLinkBuilder.build(appointment.getCustomer().getPhone(), message);
    }

    private static String buildMessage(Appointment appointment,
                                       String businessName,
                                       String address) {
        String firstName = firstName(appointment.getCustomer().getName());
        String when = appointment.getStartsAt().format(WHEN);
        String service = appointment.getService().getName();
        String reference = appointment.getReference();

        if (appointment.getStatus() == AppointmentStatus.CONFIRMED) {
            return "Hola " + firstName + ", te confirmamos el turno de "
                    + service + " para el " + when + " en " + businessName
                    + " (" + address + "). Código: " + reference
                    + ". ¡Te esperamos!";
        }

        // PENDING: se usa para responderle al cliente antes o después de
        // confirmar. El mensaje no promete que ya está confirmado.
        return "Hola " + firstName + ", recibimos tu solicitud para "
                + service + " el " + when + ". Te confirmamos a la brevedad. "
                + "Código: " + reference + ".";
    }

    private static String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        return fullName.trim().split("\\s+")[0];
    }
}
