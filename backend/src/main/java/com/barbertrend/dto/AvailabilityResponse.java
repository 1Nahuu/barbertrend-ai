package com.barbertrend.dto;

import com.barbertrend.domain.ServiceOffering;
import com.barbertrend.service.AvailabilityService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta de la consulta de disponibilidad.
 *
 * @param closed  true si ese día la barbería está cerrada. No es un error:
 *                es un resultado válido que el frontend tiene que saber
 *                mostrar ("Ese día estamos cerrados").
 * @param slots   horarios de inicio libres
 */
public record AvailabilityResponse(
        LocalDate date,
        Long serviceId,
        String serviceName,
        boolean closed,
        String timezone,
        String currency,
        List<SlotResponse> slots
) {

    public static AvailabilityResponse from(ServiceOffering service,
                                            AvailabilityService.Availability availability,
                                            String timezone,
                                            String currency) {
        List<SlotResponse> slots = availability.availableStarts().stream()
                .map(SlotResponse::new)
                .toList();

        return new AvailabilityResponse(
                availability.date(),
                service.getId(),
                service.getName(),
                availability.closed(),
                timezone,
                currency,
                slots);
    }

    /** Un horario offered al cliente. */
    public record SlotResponse(LocalDateTime startsAt) {
    }
}
