package com.barbertrend.repository;

import com.barbertrend.domain.TimeOff;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimeOffRepository extends JpaRepository<TimeOff, Long> {

    /**
     * Bloqueos que se superponen con una ventana de tiempo.
     *
     * <p>Dos intervalos se superponen cuando el inicio de uno es anterior al
     * fin del otro, y el fin de uno es posterior al inicio del otro.
     */
    @Query("""
            SELECT t FROM TimeOff t
            WHERE t.startsAt < :to AND t.endsAt > :from
            """)
    List<TimeOff> findOverlapping(@Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);

    /**
     * Bloqueos que afectan a un barbero: los del local completo
     * ({@code barber IS NULL}) y los propios de ese barbero.
     */
    @Query("""
            SELECT t FROM TimeOff t
            WHERE t.startsAt < :to AND t.endsAt > :from
              AND (t.barber IS NULL OR t.barber.id = :barberId)
            """)
    List<TimeOff> findOverlappingForBarber(@Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to,
                                           @Param("barberId") Long barberId);

    /** Bloqueos que todavía no terminaron, para listarlos en el panel. */
    List<TimeOff> findByEndsAtAfterOrderByStartsAtAsc(LocalDateTime from);

    List<TimeOff> findByStartsAtBetweenOrderByStartsAtAsc(LocalDateTime from, LocalDateTime to);
}
