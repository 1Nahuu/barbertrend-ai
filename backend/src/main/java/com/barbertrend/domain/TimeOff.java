package com.barbertrend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Bloqueo de la agenda: vacaciones, feriados, un almuerzo largo, un curso.
 *
 * <p>Cualquier bloque que se superponga con uno de estos queda fuera de la
 * disponibilidad. Si {@code barberId} es {@code null}, el bloqueo afecta a
 * todo el local.
 */
@Entity
@Table(name = "time_off")
public class TimeOff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barber_id")
    private Barber barber;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(name = "reason", length = 200)
    private String reason;

    protected TimeOff() {
        // Constructor para JPA.
    }

    public TimeOff(Barber barber, LocalDateTime startsAt, LocalDateTime endsAt, String reason) {
        this.barber = barber;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public Barber getBarber() {
        return barber;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }

    public String getReason() {
        return reason;
    }
}
