package com.barbertrend.service;

import com.barbertrend.config.AppProperties;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * Reglas de la grilla de horarios.
 *
 * <p>La regla principal: <b>una duración se redondea hacia arriba</b> al
 * múltiplo del bloque más cercano. Un servicio de 45 minutos ocupa 2 bloques
 * de 30, es decir 60 minutos de silla.
 *
 * <p>Se redondea hacia arriba y no hacia el más cercano porque la silla
 * ocupada de más no se puede recuperar: si el turno termina antes de lo
 * previsto, la persona que viene después ya igual reservó su lugar.
 *
 * <p>Esta clase es "tonta" a propósito: no toca la base de datos ni la red,
 * así que se puede probar sola y rápido.
 */
@Component
public class ScheduleRules {

    private final AppProperties properties;

    public ScheduleRules(AppProperties properties) {
        this.properties = properties;
    }

    /** Longitud de un bloque, en minutos. */
    public int slotMinutes() {
        return properties.getSlotMinutes();
    }

    /** Cuántos bloques ocupa un servicio de esa duración. */
    public int blocksNeeded(int durationMinutes) {
        int slot = slotMinutes();
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("La duración tiene que ser mayor a cero.");
        }
        return (int) Math.ceil(durationMinutes / (double) slot);
    }

    /** Duración real ocupada en la agenda, ya redondeada al bloque. */
    public int occupiedMinutes(int durationMinutes) {
        return blocksNeeded(durationMinutes) * slotMinutes();
    }

    /** Primer momento en que se puede reservar, por la anticipación mínima. */
    public LocalDateTime earliestBookableFrom(java.time.Clock clock) {
        return LocalDateTime.now(clock).plusMinutes(properties.getMinLeadMinutes());
    }

    /** Último momento hasta el que se puede reservar. */
    public LocalDateTime lastBookableDate(java.time.Clock clock) {
        return LocalDateTime.now(clock).toLocalDate()
                .plusDays(properties.getMaxAdvanceDays())
                .atStartOfDay();
    }
}
