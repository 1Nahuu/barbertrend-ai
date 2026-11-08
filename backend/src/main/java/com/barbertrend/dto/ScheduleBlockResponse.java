package com.barbertrend.dto;

import com.barbertrend.domain.TimeOff;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Cierre de agenda que se muestra en el panel.
 *
 * <p>{@code allDay} se calcula a partir del rango: un cierre de día completo
 * empieza y termina a las 00:00. El final es exclusivo, así que un cierre del
 * 12/11 termina el 13/11 a las 00:00.
 */
public record ScheduleBlockResponse(
        Long id,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        boolean allDay,
        String reason
) {

    public static ScheduleBlockResponse from(TimeOff block) {
        return new ScheduleBlockResponse(
                block.getId(),
                block.getStartsAt(),
                block.getEndsAt(),
                isAllDay(block.getStartsAt(), block.getEndsAt()),
                block.getReason());
    }

    private static boolean isAllDay(LocalDateTime startsAt, LocalDateTime endsAt) {
        return startsAt.toLocalTime().equals(LocalTime.MIDNIGHT)
                && endsAt.toLocalTime().equals(LocalTime.MIDNIGHT);
    }
}
