package com.barbertrend.service;

import com.barbertrend.common.BookingRuleException;
import com.barbertrend.config.AppProperties;
import com.barbertrend.domain.Barber;
import com.barbertrend.domain.BusinessHours;
import com.barbertrend.domain.ServiceOffering;
import com.barbertrend.repository.AppointmentSlotRepository;
import com.barbertrend.repository.BarberRepository;
import com.barbertrend.repository.BusinessHoursRepository;
import com.barbertrend.repository.TimeOffRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calcula qué horarios hay disponibles.
 *
 * <p>El mismo cálculo se usa para mostrar la grilla al cliente y para
 * validar una reserva, así que lo que se ve en pantalla y lo que se acepta
 * como reserva nunca se contradicen.
 *
 * <h2>De dónde sale la lista</h2>
 * <ol>
 *   <li>Se busca el horario de atención de ese día. Si está cerrado, no hay
 *       horarios: es un resultado válido, no un error.</li>
 *   <li>Se genera la grilla de bloques desde la apertura hasta el cierre.</li>
 *   <li>Se descartan los bloques que: están en el pasado, están dentro de
 *       la anticipación mínima, están fuera del horizonte máximo, están
 *       ocupados, se superponen con un bloqueo, o no dejan que el servicio
 *       termine antes del cierre.</li>
 * </ol>
 */
@Service
public class AvailabilityService {

    private final BarberRepository barberRepository;
    private final BusinessHoursRepository businessHoursRepository;
    private final AppointmentSlotRepository slotRepository;
    private final TimeOffRepository timeOffRepository;
    private final ScheduleRules rules;
    private final AppProperties properties;
    private final Clock clock;

    public AvailabilityService(BarberRepository barberRepository,
                               BusinessHoursRepository businessHoursRepository,
                               AppointmentSlotRepository slotRepository,
                               TimeOffRepository timeOffRepository,
                               ScheduleRules rules,
                               AppProperties properties,
                               Clock clock) {
        this.barberRepository = barberRepository;
        this.businessHoursRepository = businessHoursRepository;
        this.slotRepository = slotRepository;
        this.timeOffRepository = timeOffRepository;
        this.rules = rules;
        this.properties = properties;
        this.clock = clock;
    }

    /** Resultado de consultar la disponibilidad de un día. */
    public record Availability(LocalDate date,
                               boolean closed,
                               List<LocalDateTime> availableStarts) {
    }

    /**
     * Devuelve los horarios de inicio libres para un servicio en una fecha.
     *
     * @throws BookingRuleException si la fecha está fuera del rango permitido
     */
    @Transactional(readOnly = true)
    public Availability compute(ServiceOffering service, LocalDate date) {
        validateDateRange(date);

        Barber barber = barberRepository.findFirstByActiveTrue()
                .orElseThrow(BookingRuleException::barberNotAvailable);

        BusinessHours hours = businessHoursRepository
                .findByDayOfWeek(BusinessHours.toDayOfWeek(date.getDayOfWeek()))
                .orElse(null);

        if (hours == null || hours.isClosed()) {
            return new Availability(date, true, List.of());
        }

        // --- Ventana del día ---
        LocalDateTime dayStart = date.atTime(LocalTime.MIN);
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        // Un cierre de día completo se muestra igual que un día cerrado:
        // "Ese día estamos cerrados", no "No quedan horarios libres".
        boolean fullyBlocked = timeOffRepository
                .findOverlappingForBarber(dayStart, dayEnd, barber.getId())
                .stream()
                .anyMatch(t -> !t.getStartsAt().isAfter(dayStart)
                        && !t.getEndsAt().isBefore(dayEnd));
        if (fullyBlocked) {
            return new Availability(date, true, List.of());
        }

        // --- Bloques ya ocupados ---
        Set<LocalDateTime> occupied = new HashSet<>(
                slotRepository.findOccupiedSlots(barber.getId(), dayStart, dayEnd));

        // --- Bloqueos (vacaciones, feriados) ---
        // Se consulta la ventana completa del día una sola vez.
        Set<LocalDateTime> blockedByTimeOff = new HashSet<>();
        int slot = rules.slotMinutes();
        LocalDateTime blockCursor = dayStart;
        while (blockCursor.isBefore(dayEnd)) {
            boolean overlapping = !timeOffRepository.findOverlappingForBarber(
                    blockCursor, blockCursor.plusMinutes(slot), barber.getId()).isEmpty();
            if (overlapping) {
                blockedByTimeOff.add(blockCursor);
            }
            blockCursor = blockCursor.plusMinutes(slot);
        }

        // --- Recorrer la grilla ---
        int blocksNeeded = rules.blocksNeeded(service.getDurationMinutes());
        int occupiedMinutes = rules.occupiedMinutes(service.getDurationMinutes());

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime earliest = rules.earliestBookableFrom(clock);

        // Solo el día de hoy tiene un límite por hora. Los días futuros no.
        LocalDateTime effectiveEarliest = date.equals(now.toLocalDate())
                ? earliest
                : date.atStartOfDay();

        List<LocalDateTime> available = new java.util.ArrayList<>();

        LocalTime cursor = hours.getOpenTime();
        while (cursor.isBefore(hours.getCloseTime())) {
            LocalDateTime start = date.atTime(cursor);
            LocalDateTime end = start.plusMinutes(occupiedMinutes);

            boolean fitsInDay = !end.isAfter(date.atTime(hours.getCloseTime()));
            boolean isNotPast = !start.isBefore(effectiveEarliest);

            boolean anyBlockOccupied = false;
            boolean anyBlockBlocked = false;
            LocalDateTime probe = start;
            for (int i = 0; i < blocksNeeded && !anyBlockOccupied && !anyBlockBlocked; i++) {
                if (occupied.contains(probe)) {
                    anyBlockOccupied = true;
                }
                if (blockedByTimeOff.contains(probe)) {
                    anyBlockBlocked = true;
                }
                probe = probe.plusMinutes(slot);
            }

            if (fitsInDay && isNotPast && !anyBlockOccupied && !anyBlockBlocked) {
                available.add(start);
            }

            cursor = cursor.plusMinutes(slot);
        }

        return new Availability(date, false, available);
    }

    /** Chequea que la fecha esté dentro del rango en el que se puede reservar. */
    private void validateDateRange(LocalDate date) {
        LocalDate today = LocalDate.now(clock);

        if (date.isBefore(today)) {
            throw BookingRuleException.pastDate();
        }

        LocalDate maxDate = LocalDate.now(clock).plusDays(properties.getMaxAdvanceDays());
        if (date.isAfter(maxDate)) {
            throw BookingRuleException.tooFarAhead();
        }
    }

    /** Minutos entre dos instantes, sin importar el día. */
    public static long minutesBetween(LocalDateTime from, LocalDateTime to) {
        return ChronoUnit.MINUTES.between(from, to);
    }
}
