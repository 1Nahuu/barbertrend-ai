package com.barbertrend.dto;

import com.barbertrend.domain.BusinessHours;
import java.time.LocalTime;
import java.util.List;

/**
 * Horario semanal completo, como lo ve y lo edita el dueño.
 *
 * <p>Siempre se devuelven los 7 días, incluso los cerrados, para que el
 * panel muestre la semana entera.
 */
public record BusinessHoursResponse(
        List<DayHours> days
) {

    public static BusinessHoursResponse from(List<BusinessHours> all) {
        return new BusinessHoursResponse(all.stream()
                .sorted(java.util.Comparator.comparing(BusinessHours::getDayOfWeek))
                .map(DayHours::from)
                .toList());
    }

    public record DayHours(
            int dayOfWeek,
            String dayName,
            LocalTime openTime,
            LocalTime closeTime,
            boolean closed
    ) {
        public static DayHours from(BusinessHours hours) {
            return new DayHours(
                    hours.getDayOfWeek(),
                    java.time.DayOfWeek.of(hours.getDayOfWeek())
                            .getDisplayName(
                                    java.time.format.TextStyle.FULL,
                                    java.util.Locale.forLanguageTag("es")),
                    hours.getOpenTime(),
                    hours.getCloseTime(),
                    hours.isClosed());
        }
    }

    /** Petición para cambiar el horario de un día. */
    public record DayHoursRequest(
            LocalTime openTime,
            LocalTime closeTime,
            boolean closed
    ) {
        public boolean isValid() {
            if (closed) {
                return true;
            }
            return openTime != null && closeTime != null && closeTime.isAfter(openTime);
        }
    }
}
