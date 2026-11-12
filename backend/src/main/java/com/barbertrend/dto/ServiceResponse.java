package com.barbertrend.dto;

import com.barbertrend.domain.ServiceOffering;
import java.math.BigDecimal;

/**
 * Servicio que se devuelve al público.
 *
 * <p>Es un {@code record} y no la entidad directamente, por dos razones:
 * la entidad tiene campos internos que no van a la web, y si mañana cambia
 * el modelo no se rompe lo que ya consume el frontend.
 *
 * <p>Incluye la duración porque el formulario de reserva la necesita para
 * calcular cuántos bloques ocupa el turno. No se muestra como dato comercial:
 * un corte no dura siempre lo mismo.
 */
public record ServiceResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String currency,
        int durationMinutes
) {

    public static ServiceResponse from(ServiceOffering service, String currency) {
        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getPrice(),
                currency,
                service.getDurationMinutes());
    }
}
