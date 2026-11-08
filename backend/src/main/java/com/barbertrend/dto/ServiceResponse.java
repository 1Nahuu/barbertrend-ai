package com.barbertrend.dto;

import com.barbertrend.domain.ServiceOffering;
import java.math.BigDecimal;

/**
 * Servicio que se devuelve al cliente.
 *
 * <p>Es un {@code record} y no la entidad directamente, por dos razones:
 * la entidad tiene campos internos que no van a la web, y si mañana cambia
 * el modelo no se rompe lo que ya consume el frontend.
 */
public record ServiceResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String currency
) {

    public static ServiceResponse from(ServiceOffering service, String currency) {
        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getPrice(),
                currency);
    }
}
