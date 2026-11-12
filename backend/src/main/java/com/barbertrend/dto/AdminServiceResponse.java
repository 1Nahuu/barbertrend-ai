package com.barbertrend.dto;

import com.barbertrend.domain.ServiceOffering;
import java.math.BigDecimal;

/**
 * Servicio tal como lo ve el panel del dueño.
 *
 * <p>Agrega los campos internos que el público no necesita: si está activo y
 * en qué orden se muestra. El dueño los usa para editar el catálogo sin tocar
 * archivos ni SQL.
 */
public record AdminServiceResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String currency,
        int durationMinutes,
        boolean active,
        int sortOrder
) {

    public static AdminServiceResponse from(ServiceOffering service, String currency) {
        return new AdminServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getPrice(),
                currency,
                service.getDurationMinutes(),
                service.isActive(),
                service.getSortOrder());
    }
}
