package com.barbertrend.dto;

import com.barbertrend.domain.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalTime;

/** Petición del panel para cambiar el estado de un turno. */
public record UpdateStatusRequest(
        @NotNull(message = "Indicá el nuevo estado.")
        AppointmentStatus status
) {
}
