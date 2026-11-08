package com.barbertrend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import org.springframework.data.domain.Persistable;

/**
 * Un bloque de la agenda que está ocupado.
 *
 * <h2>Por qué esta tabla existe</h2>
 *
 * <p>La forma naïve de reservar es "pregunto si está libre y, si está, lo
 * grabo". <b>Eso tiene una condición de carrera:</b> entre la consulta y el
 * {@code INSERT} entra la otra petición, que también ve el horario libre, y
 * las dos graban. Dos personas reservando el mismo horario a la vez es
 * justamente lo que pasa en la práctica.
 *
 * <p>La restricción {@code UNIQUE(barber_id, starts_at)} tampoco alcanza,
 * porque dos turnos pueden empezar en instantes distintos y pisarse:
 *
 * <pre>
 *   10:00 → 11:00   y   10:30 → 11:30
 * </pre>
 *
 * <p>Esos dos {@code starts_at} son distintos, así que la restricción no se
 * activaría y los dos clientes terminarían en la misma silla.
 *
 * <h2>La solución</h2>
 *
 * <p>La jornada se divide en bloques ({@code app.slot-minutes}, 30 minutos).
 * Una reserva ocupa <b>varios</b> bloques, y se inserta <b>una fila por
 * bloque</b>. Un corte de 60 minutos a las 10:00 inserta
 * {@code (barber, 10:00)} y {@code (barber, 10:30)}.
 *
 * <p>Si alguien intenta tomar un bloque ya ocupado, el {@code INSERT} viola
 * la clave primaria compuesta y <b>la base de datos rechaza la
 * operación</b>. La aplicación solo tiene que traducir ese error a un
 * {@code 409 Conflict}.
 *
 * <h2>Por qué una tabla y no un índice parcial de PostgreSQL</h2>
 *
 * <p>PostgreSQL permite {@code CREATE UNIQUE INDEX ... WHERE status =
 * 'PENDING'}, que sería más compacto. Pero <b>H2 no soporta índices
 * parciales</b>, así que el desarrollo local y las pruebas se comportarían
 * distinto de la producción. Con esta tabla, la lógica es idéntica en los
 * dos motores.
 */
@Entity
@Table(name = "appointment_slot")
public class AppointmentSlot implements Persistable<AppointmentSlotId> {

    @EmbeddedId
    private AppointmentSlotId id;

    /**
     * Spring Data decide entre {@code persist} e {@code merge} mirando si la
     * entidad es nueva. Con una clave compuesta asignada a mano, la clave no
     * es nula, así que por defecto la considera "existente" y usa
     * {@code merge}. Eso hace un SELECT y, si la fila no está, un INSERT;
     * pero si otro hilo ya insertó el bloque, el SELECT encuentra la fila y
     * el {@code merge} la ACTUALIZA en vez de fallar. El resultado era que
     * varios turnos se quedaban con el mismo bloque.
     *
     * <p>Con {@link Persistable} forzamos {@code persist} para una entidad
     * recién creada: el INSERT choca con la clave primaria y la base de
     * datos rechaza al perdedor, que es justo lo que necesitamos.
     */
    @Transient
    private boolean isNew = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    protected AppointmentSlot() {
        // Constructor para JPA.
    }

    public AppointmentSlot(AppointmentSlotId id, Appointment appointment) {
        this.id = id;
        this.appointment = appointment;
    }

    @Override
    public AppointmentSlotId getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    public LocalDateTime getSlotStart() {
        return id.getSlotStart();
    }

    public Appointment getAppointment() {
        return appointment;
    }
}
