package com.barbertrend.repository;

import com.barbertrend.domain.Appointment;
import com.barbertrend.domain.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByReference(String reference);

    /**
     * Turno con cliente, servicio y barbero ya cargados.
     *
     * <p>Se usa en el detalle y en los cambios de estado: esas respuestas
     * arman el DTO después de que la transacción de escritura terminó, y
     * sin traer las relaciones acá saltaría una {@code
     * LazyInitializationException} al mapear.
     */
    @EntityGraph(attributePaths = {"customer", "service", "barber"})
    @Query("SELECT a FROM Appointment a WHERE a.id = :id")
    Optional<Appointment> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"customer", "service", "barber"})
    @Query("SELECT a FROM Appointment a WHERE a.reference = :reference")
    Optional<Appointment> findDetailedByReference(@Param("reference") String reference);

    /**
     * Turnos de un rango de fechas, del más nuevo al más viejo.
     *
     * <p>El {@code EntityGraph} trae cliente, servicio y barbero en la misma
     * consulta. Sin esto, Spring haría una consulta aparte por cada turno de
     * la lista: para 50 turnos, 151 consultas en vez de una.
     */
    @EntityGraph(attributePaths = {"customer", "service", "barber"})
    @Query("""
            SELECT a FROM Appointment a
            WHERE a.startsAt >= :from AND a.startsAt < :to
            ORDER BY a.startsAt ASC
            """)
    List<Appointment> findBetween(@Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to,
                                  Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "service", "barber"})
    @Query("""
            SELECT a FROM Appointment a
            WHERE a.status = :status
            ORDER BY a.startsAt ASC
            """)
    List<Appointment> findByStatus(@Param("status") AppointmentStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "service", "barber"})
    List<Appointment> findByReferenceIn(List<String> references);

    /**
     * Cuenta turnos que siguen ocupando la agenda y se superponen con un
     * rango. Se usa antes de crear un cierre para no dejar turnos activos
     * dentro de un día cerrado.
     */
    @Query("""
            SELECT COUNT(a) FROM Appointment a
            WHERE a.status IN :statuses
              AND a.startsAt < :to AND a.endsAt > :from
            """)
    long countByStatusInAndRange(@Param("statuses") java.util.Collection<AppointmentStatus> statuses,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to);
}
