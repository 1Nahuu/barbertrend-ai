package com.barbertrend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Datos para cerrar la agenda desde el panel.
 *
 * <p>Un cierre puede ser de día completo (incluso varios días seguidos) o de
 * una franja horaria dentro de una fecha. Las horas se guardan como hora
 * local de la barbería, igual que el resto de la agenda.
 *
 * @param startDate primer día del cierre
 * @param endDate   último día del cierre; si es null se usa {@code startDate}
 * @param allDay    true para cerrar los días completos
 * @param startTime hora de inicio cuando no es un cierre de día completo
 * @param endTime   hora de fin cuando no es un cierre de día completo
 * @param reason    motivo opcional, por ejemplo "Feriado" o "Vacaciones"
 */
public record CreateScheduleBlockRequest(
        @NotNull(message = "Elegí una fecha de inicio.")
        LocalDate startDate,

        LocalDate endDate,

        boolean allDay,

        LocalTime startTime,

        LocalTime endTime,

        @Size(max = 200, message = "El motivo es demasiado largo.")
        String reason
) {
}
