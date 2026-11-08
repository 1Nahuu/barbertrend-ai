package com.barbertrend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Clave compuesta de {@link AppointmentSlot}: (barberId, slotStart).
 *
 * <p>Es la parte que hace que la base de datos, y no el código, sea la que
 * garantiza que no se superpongan los turnos. Ver {@link AppointmentSlot}.
 */
@Embeddable
public class AppointmentSlotId implements Serializable {

    @Column(name = "barber_id")
    private Long barberId;

    @Column(name = "slot_start")
    private LocalDateTime slotStart;

    protected AppointmentSlotId() {
        // Constructor para JPA.
    }

    public AppointmentSlotId(Long barberId, LocalDateTime slotStart) {
        this.barberId = barberId;
        this.slotStart = slotStart;
    }

    public Long getBarberId() {
        return barberId;
    }

    public LocalDateTime getSlotStart() {
        return slotStart;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AppointmentSlotId that)) {
            return false;
        }
        return Objects.equals(barberId, that.barberId)
                && Objects.equals(slotStart, that.slotStart);
    }

    @Override
    public int hashCode() {
        return Objects.hash(barberId, slotStart);
    }
}
