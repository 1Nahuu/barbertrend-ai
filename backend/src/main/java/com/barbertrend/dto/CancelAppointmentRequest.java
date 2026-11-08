package com.barbertrend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Petición del panel para cancelar un turno. */
public record CancelAppointmentRequest(
        @NotBlank(message = "Escribí el motivo de la cancelación.")
        @Size(max = 200, message = "El motivo es demasiado largo.")
        String reason
) {
}
