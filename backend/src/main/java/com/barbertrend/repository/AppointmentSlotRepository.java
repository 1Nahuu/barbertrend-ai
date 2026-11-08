package com.barbertrend.repository;

import com.barbertrend.domain.AppointmentSlot;
import com.barbertrend.domain.AppointmentSlotId;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a los bloques ocupados de la agenda.
 *
 * <p>Esta interfaz es el corazón de la garantía de no superposición:
 * cuando se reserva, se insertan filas acá, y si un bloque ya existe, la
 * base las rechaza (ver {@link AppointmentSlot}).
 */
public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, AppointmentSlotId> {

    /**
     * Bloques ocupados en una ventana de tiempo, para un barbero.
     *
     * <p>En JPQL, {@code barberId} y {@code slotStart} están dentro de la
     * clave compuesta {@code id}, así que el camino es {@code s.id.slotStart}.
     * Por eso no se pueden referenciar como atributos sueltos de la entidad.
     */
    @Query("""
            SELECT s.id.slotStart FROM AppointmentSlot s
            WHERE s.id.barberId = :barberId
              AND s.id.slotStart >= :from AND s.id.slotStart < :to
            """)
    List<LocalDateTime> findOccupiedSlots(@Param("barberId") Long barberId,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);

    /** Libera el horario: borra los bloques de un turno. */
    @Modifying
    @Query("DELETE FROM AppointmentSlot s WHERE s.appointment.id = :appointmentId")
    int deleteByAppointmentId(@Param("appointmentId") Long appointmentId);

    /** ¿Está ocupado ese bloque exacto? */
    boolean existsById(AppointmentSlotId id);
}
