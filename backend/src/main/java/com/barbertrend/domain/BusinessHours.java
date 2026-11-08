package com.barbertrend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;

/**
 * Horario de atención de un día de la semana.
 *
 * <p>Siempre hay 7 filas, una por día, incluso los cerrados: así el dueño ve
 * el horario completo en el panel y puede modificarlo.
 */
@Entity
@Table(name = "business_hours")
public class BusinessHours {

    /** 1 = lunes … 7 = domingo. */
    public static final int MONDAY = 1;
    public static final int SUNDAY = 7;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "day_of_week", nullable = false, unique = true)
    private int dayOfWeek;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Column(name = "is_closed", nullable = false)
    private boolean closed = false;

    protected BusinessHours() {
        // Constructor para JPA.
    }

    public BusinessHours(int dayOfWeek, LocalTime openTime, LocalTime closeTime, boolean closed) {
        this.dayOfWeek = dayOfWeek;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.closed = closed;
    }

    /**
     * Convierte el {@link java.time.DayOfWeek} de Java al número que usa la base.
     * Java usa 1 = lunes … 7 = domingo, que es el mismo esquema.
     */
    public static int toDayOfWeek(java.time.DayOfWeek day) {
        return day.getValue();
    }

    public Long getId() {
        return id;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getOpenTime() {
        return openTime;
    }

    public void setOpenTime(LocalTime openTime) {
        this.openTime = openTime;
    }

    public LocalTime getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(LocalTime closeTime) {
        this.closeTime = closeTime;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }
}
