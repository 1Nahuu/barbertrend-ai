package com.barbertrend.service;

import com.barbertrend.common.ResourceNotFoundException;
import com.barbertrend.common.ScheduleBlockException;
import com.barbertrend.domain.AppointmentStatus;
import com.barbertrend.domain.TimeOff;
import com.barbertrend.dto.CreateScheduleBlockRequest;
import com.barbertrend.repository.AppointmentRepository;
import com.barbertrend.repository.TimeOffRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cierres puntuales de la agenda: un día completo, varios días seguidos o
 * una franja horaria de una fecha.
 *
 * <p>Se apoyan en la tabla {@code time_off}, que ya usa
 * {@link AvailabilityService} para ocultar horarios. Un cierre creado desde
 * el panel también se valida al reservar, para que una petición directa a la
 * API no pueda saltearse el bloqueo.
 */
@Service
public class ScheduleBlockService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleBlockService.class);

    private final TimeOffRepository timeOffRepository;
    private final AppointmentRepository appointmentRepository;
    private final ScheduleRules rules;
    private final Clock clock;

    public ScheduleBlockService(TimeOffRepository timeOffRepository,
                                AppointmentRepository appointmentRepository,
                                ScheduleRules rules,
                                Clock clock) {
        this.timeOffRepository = timeOffRepository;
        this.appointmentRepository = appointmentRepository;
        this.rules = rules;
        this.clock = clock;
    }

    /** Cierres que todavía no terminaron, del más próximo al más lejano. */
    @Transactional(readOnly = true)
    public List<TimeOff> listUpcoming() {
        return timeOffRepository.findByEndsAtAfterOrderByStartsAtAsc(
                LocalDateTime.now(clock));
    }

    /**
     * Crea un cierre después de validar el rango, los solapamientos y los
     * turnos que todavía ocupan la agenda.
     */
    @Transactional
    public TimeOff create(CreateScheduleBlockRequest request) {
        if (request == null || request.startDate() == null) {
            throw ScheduleBlockException.invalidRange(
                    "Elegí una fecha de inicio.", "startDate");
        }

        LocalDate startDate = request.startDate();
        LocalDate endDate = request.endDate() == null ? startDate : request.endDate();

        if (endDate.isBefore(startDate)) {
            throw ScheduleBlockException.invalidRange(
                    "La fecha final no puede ser anterior a la inicial.", "endDate");
        }

        LocalDateTime startsAt;
        LocalDateTime endsAt;

        if (request.allDay()) {
            startsAt = startDate.atStartOfDay();
            endsAt = endDate.plusDays(1).atStartOfDay();
        } else {
            if (!endDate.equals(startDate)) {
                throw ScheduleBlockException.invalidRange(
                        "Un cierre por horas tiene que ser de un solo día.", "endDate");
            }
            if (request.startTime() == null || request.endTime() == null) {
                throw ScheduleBlockException.invalidRange(
                        "Indicá la hora de inicio y la hora de fin.", "startTime");
            }
            validateSlotTime(request.startTime(), "startTime");
            validateSlotTime(request.endTime(), "endTime");

            if (!request.endTime().isAfter(request.startTime())) {
                throw ScheduleBlockException.invalidRange(
                        "La hora final tiene que ser posterior a la inicial.", "endTime");
            }

            startsAt = startDate.atTime(request.startTime());
            endsAt = startDate.atTime(request.endTime());
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (!endsAt.isAfter(now)) {
            throw ScheduleBlockException.past();
        }

        if (!timeOffRepository.findOverlapping(startsAt, endsAt).isEmpty()) {
            throw ScheduleBlockException.overlap();
        }

        long activeAppointments = appointmentRepository.countByStatusInAndRange(
                List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED),
                startsAt, endsAt);
        if (activeAppointments > 0) {
            throw ScheduleBlockException.hasAppointments(activeAppointments);
        }

        String reason = request.reason() == null ? null : request.reason().trim();
        if (reason != null && reason.isEmpty()) {
            reason = null;
        }

        TimeOff block = timeOffRepository.save(new TimeOff(null, startsAt, endsAt, reason));
        log.info("Cierre de agenda creado del {} al {} (motivo: {})",
                startsAt, endsAt, reason == null ? "sin motivo" : reason);
        return block;
    }

    /** Elimina un cierre y deja la agenda disponible otra vez. */
    @Transactional
    public void delete(Long id) {
        TimeOff block = timeOffRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.block(id));
        timeOffRepository.delete(block);
        log.info("Cierre de agenda {} eliminado ({} a {})",
                id, block.getStartsAt(), block.getEndsAt());
    }

    /**
     * Las horas de un cierre parcial tienen que caer en la misma grilla que
     * los turnos. Así un cierre de 13:15 no bloquea media hora completa de
     * forma impredecible.
     */
    private void validateSlotTime(LocalTime time, String field) {
        int minutes = time.getHour() * 60 + time.getMinute();

        if (time.getSecond() != 0 || time.getNano() != 0
                || minutes % rules.slotMinutes() != 0) {
            throw ScheduleBlockException.invalidRange(
                    "Las horas tienen que ir en bloques de "
                            + rules.slotMinutes() + " minutos.", field);
        }
    }
}
