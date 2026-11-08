package com.barbertrend.service;

import com.barbertrend.common.BookingRuleException;
import com.barbertrend.common.SlotNotAvailableException;
import com.barbertrend.domain.Appointment;
import com.barbertrend.domain.AppointmentSlot;
import com.barbertrend.domain.AppointmentSlotId;
import com.barbertrend.domain.AppointmentStatus;
import com.barbertrend.domain.Barber;
import com.barbertrend.domain.BusinessHours;
import com.barbertrend.domain.Customer;
import com.barbertrend.domain.ServiceOffering;
import com.barbertrend.notification.AppointmentCreatedEvent;
import com.barbertrend.notification.AppointmentNotification;
import com.barbertrend.repository.AppointmentRepository;
import com.barbertrend.repository.AppointmentSlotRepository;
import com.barbertrend.repository.BarberRepository;
import com.barbertrend.repository.BusinessHoursRepository;
import com.barbertrend.repository.CustomerRepository;
import com.barbertrend.repository.ServiceOfferingRepository;
import com.barbertrend.repository.TimeOffRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea y modifica turnos.
 *
 * <h2>La garantía de que no se superponen</h2>
 *
 * <p>Todo el método {@link #create} está dentro de una sola transacción: el
 * turno y sus bloques se graban juntos, o no se graba nada. No puede quedar
 * un turno guardado sin bloquear la hora, ni una hora bloqueada sin un turno
 * que la justifique.
 *
 * <p>El choque de horarios lo detecta <b>la base de datos</b>, no este código.
 * El servicio inserta una fila por cada bloque que ocupa el turno; si un
 * bloque ya existe, la clave primaria compuesta de {@code appointment_slot}
 * viola la restricción y H2 o PostgreSQL rechazan el INSERT. La excepción
 * que llega hasta acá es {@link DataIntegrityViolationException}, y el
 * {@link com.barbertrend.common.GlobalExceptionHandler} la traduce a un
 * 409 con un mensaje entendible.
 *
 * <p>Esto funciona aunque entren muchas reservas a la vez: no depende de que
 * el código de la aplicación se acuerde de chequear antes, porque el que
 * decide es la base.
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    /** Caracteres del código público, sin letras que se confundan (0/O, 1/I/L). */
    private static final String REFERENCE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    private final AppointmentRepository appointmentRepository;
    private final AppointmentSlotRepository slotRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final BarberRepository barberRepository;
    private final BusinessHoursRepository businessHoursRepository;
    private final CustomerRepository customerRepository;
    private final TimeOffRepository timeOffRepository;
    private final ScheduleRules rules;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom random = new SecureRandom();

    public BookingService(AppointmentRepository appointmentRepository,
                          AppointmentSlotRepository slotRepository,
                          ServiceOfferingRepository serviceRepository,
                          BarberRepository barberRepository,
                          BusinessHoursRepository businessHoursRepository,
                          CustomerRepository customerRepository,
                          TimeOffRepository timeOffRepository,
                          ScheduleRules rules,
                          ApplicationEventPublisher eventPublisher) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
        this.serviceRepository = serviceRepository;
        this.barberRepository = barberRepository;
        this.businessHoursRepository = businessHoursRepository;
        this.customerRepository = customerRepository;
        this.timeOffRepository = timeOffRepository;
        this.rules = rules;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Crea un turno.
     *
     * @throws SlotNotAvailableException si otro cliente ya tomó ese horario
     */
    @Transactional
    public Appointment create(Long serviceId, LocalDateTime startsAt,
                              String customerName, String phoneDigits, String notes,
                              Clock clock) {

        ServiceOffering service = serviceRepository.findById(serviceId)
                .filter(ServiceOffering::isActive)
                .orElseThrow(() -> com.barbertrend.common.ResourceNotFoundException.service(serviceId));

        Barber barber = barberRepository.findFirstByActiveTrue()
                .orElseThrow(BookingRuleException::barberNotAvailable);

        // --- Validar que el horario esté dentro de la agenda ---
        validateAgainstSchedule(service, startsAt, clock);
        ensureNotBlocked(service, barber, startsAt);

        // --- Cuánto ocupa el turno ---
        int occupiedMinutes = rules.occupiedMinutes(service.getDurationMinutes());
        LocalDateTime endsAt = startsAt.plusMinutes(occupiedMinutes);

        // --- Cliente ---
        LocalDateTime now = LocalDateTime.now(clock);
        Customer customer = customerRepository.save(
                new Customer(customerName, phoneDigits, now));

        // --- Turno ---
        Appointment appointment = appointmentRepository.save(
                new Appointment(generateReference(), customer, barber,
                        service, startsAt, endsAt, now));
        if (notes != null && !notes.isBlank()) {
            appointment.setNotes(notes.trim());
        }

        // --- Bloquear los bloques ---
        // Acá es donde la base de datos dice "no" si el horario ya estaba.
        int blocks = rules.blocksNeeded(service.getDurationMinutes());
        int slot = rules.slotMinutes();

        try {
            LocalDateTime cursor = startsAt;
            for (int i = 0; i < blocks; i++) {
                slotRepository.saveAndFlush(new AppointmentSlot(
                        new AppointmentSlotId(barber.getId(), cursor), appointment));
                cursor = cursor.plusMinutes(slot);
            }
        } catch (DataIntegrityViolationException ex) {
            // Dos personas pidieron el mismo horario al mismo tiempo.
            // Se registra con el código, sin dato personal del cliente.
            log.info("Choque de horario al reservar {}: el bloque ya estaba ocupado.",
                    startsAt);
            throw new SlotNotAvailableException();
        }

        log.info("Turno {} creado para el {} (servicio {})",
                appointment.getReference(), startsAt, service.getName());

        // Se publica dentro de la transacción; el listener lo procesa recién
        // después del commit (ver AppointmentNotificationListener). Si esta
        // reserva termina en rollback, el aviso nunca sale.
        eventPublisher.publishEvent(
                new AppointmentCreatedEvent(AppointmentNotification.from(appointment)));

        return appointment;
    }

    /**
     * Comprueba que el horario pedido esté dentro de la agenda del día.
     *
     * <p>No consulta si está ocupado: eso lo decide la base de datos al
     * insertar los bloques. Esta validación es para dar mensajes claros
     * sobre las reglas que no dependen de la concurrencia.
     */
    private void validateAgainstSchedule(ServiceOffering service,
                                         LocalDateTime startsAt, Clock clock) {

        if (startsAt.isBefore(LocalDateTime.now(clock).minusMinutes(1))) {
            throw BookingRuleException.pastDate();
        }

        BusinessHours hours = businessHoursRepository
                .findByDayOfWeek(BusinessHours.toDayOfWeek(startsAt.getDayOfWeek()))
                .orElseThrow(BookingRuleException::outsideSchedule);

        if (hours.isClosed()) {
            throw BookingRuleException.outsideSchedule();
        }

        LocalTime start = startsAt.toLocalTime();
        if (start.isBefore(hours.getOpenTime()) || !start.isBefore(hours.getCloseTime())) {
            throw BookingRuleException.outsideSchedule();
        }

        // El servicio tiene que terminar antes del cierre.
        LocalDateTime endsAt = startsAt.plusMinutes(
                rules.occupiedMinutes(service.getDurationMinutes()));
        if (endsAt.toLocalTime().isAfter(hours.getCloseTime())) {
            throw BookingRuleException.doesNotFitBeforeClose();
        }

        // El inicio tiene que caer justo en una línea de la grilla.
        int minutesFromOpen = (int) java.time.Duration
                .between(hours.getOpenTime(), start).toMinutes();
        if (minutesFromOpen % rules.slotMinutes() != 0) {
            throw BookingRuleException.outsideSchedule();
        }
    }

    /**
     * Verifica que el turno no caiga dentro de un cierre cargado desde el
     * panel. La disponibilidad ya oculta esos horarios, pero esta validación
     * es imprescindible para una petición directa a la API.
     */
    private void ensureNotBlocked(ServiceOffering service, Barber barber,
                                  LocalDateTime startsAt) {

        LocalDateTime endsAt = startsAt.plusMinutes(
                rules.occupiedMinutes(service.getDurationMinutes()));

        if (!timeOffRepository.findOverlappingForBarber(
                startsAt, endsAt, barber.getId()).isEmpty()) {
            throw BookingRuleException.blockedTime();
        }
    }

    /**
     * Genera un código público como {@code BT-7K2M9Q}.
     *
     * <p>No es secuencial a propósito. Si fuera un contador, cualquiera
     * podría probar BT-000001, BT-000002 y ver los turnos de otros.
     */
    private String generateReference() {
        StringBuilder code = new StringBuilder("BT-");
        for (int i = 0; i < 6; i++) {
            code.append(REFERENCE_ALPHABET.charAt(random.nextInt(REFERENCE_ALPHABET.length())));
        }
        return code.toString();
    }

    /** Cancela un turno y libera su horario. */
    @Transactional
    public Appointment cancel(Long appointmentId, String reason, Clock clock) {
        Appointment appointment = appointmentRepository.findDetailedById(appointmentId)
                .orElseThrow(() -> com.barbertrend.common.ResourceNotFoundException
                        .appointment(appointmentId));

        if (!appointment.getStatus().canTransitionTo(AppointmentStatus.CANCELLED)) {
            throw new IllegalStateException(
                    "Un turno " + appointment.getStatus() + " no se puede cancelar.");
        }

        appointment.cancel(reason, LocalDateTime.now(clock));

        // Liberar los bloques: el horario vuelve a estar disponible.
        // Va en la misma transacción que el cambio de estado, así que no
        // puede quedar un turno cancelado con la hora bloqueada.
        slotRepository.deleteByAppointmentId(appointmentId);

        log.info("Turno {} cancelado: {}", appointment.getReference(), reason);
        return appointment;
    }

    /** Cambia el estado de un turno, si el cambio está permitido. */
    @Transactional
    public Appointment changeStatus(Long appointmentId, AppointmentStatus target, Clock clock) {
        Appointment appointment = appointmentRepository.findDetailedById(appointmentId)
                .orElseThrow(() -> com.barbertrend.common.ResourceNotFoundException
                        .appointment(appointmentId));

        if (!appointment.getStatus().canTransitionTo(target)) {
            throw new IllegalStateException("No se puede pasar de "
                    + appointment.getStatus() + " a " + target + ".");
        }

        appointment.changeStatus(target, LocalDateTime.now(clock));

        // Si pasa a COMPLETED, el turno ya no ocupa la agenda.
        if (target == AppointmentStatus.COMPLETED) {
            slotRepository.deleteByAppointmentId(appointmentId);
        }

        log.info("Turno {} pasó a {}", appointment.getReference(), target);
        return appointment;
    }
}
