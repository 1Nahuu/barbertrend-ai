package com.barbertrend.dto;

import com.barbertrend.domain.ServiceOffering;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Alta o edición de un servicio desde el panel.
 *
 * <p>La duración es NOMINAL (ver {@link ServiceOffering}): el dueño la usa
 * como referencia para la grilla, y el sistema la redondea hacia arriba al
 * bloque de 30 minutos más cercano.
 */
public record ServiceRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 80, message = "El nombre es demasiado largo.")
        String name,

        @Size(max = 500, message = "La descripción es demasiado larga.")
        String description,

        @NotNull(message = "El precio es obligatorio.")
        @DecimalMin(value = "0", message = "El precio no puede ser negativo.")
        BigDecimal price,

        @NotNull(message = "La duración es obligatoria.")
        @Min(value = 1, message = "La duración tiene que ser mayor a cero.")
        Integer durationMinutes,

        Boolean active,

        Integer sortOrder
) {

    public void applyTo(ServiceOffering service) {
        service.setName(name.trim());
        service.setDescription(description);
        service.setPrice(price);
        service.setDurationMinutes(durationMinutes);
        if (active != null) {
            service.setActive(active);
        }
        if (sortOrder != null) {
            service.setSortOrder(sortOrder);
        }
    }
}
