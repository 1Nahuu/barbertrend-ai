package com.barbertrend.web;

import com.barbertrend.common.ResourceNotFoundException;
import com.barbertrend.domain.Appointment;
import com.barbertrend.domain.AppointmentStatus;
import com.barbertrend.dto.AdminAppointmentDetailResponse;
import com.barbertrend.dto.AdminAppointmentResponse;
import com.barbertrend.dto.AppointmentStatusResponse;
import com.barbertrend.dto.BusinessHoursResponse;
import com.barbertrend.dto.CancelAppointmentRequest;
import com.barbertrend.dto.ServiceRequest;
import com.barbertrend.dto.ServiceResponse;
import com.barbertrend.dto.UpdateStatusRequest;
import com.barbertrend.config.AppProperties;
import com.barbertrend.repository.AppointmentRepository;
import com.barbertrend.repository.BusinessHoursRepository;
import com.barbertrend.repository.ServiceOfferingRepository;
import com.barbertrend.service.BookingService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API privada del dueño. Todas las rutas exigen sesión iniciada.
 *
 * <p>La protección no está en este archivo sino en {@code SecurityConfig}:
 * ahí se define que {@code /api/admin/**} requiere autenticación. Este
 * controlador asume que si está llegando, ya pasó el filtro.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    /** Tope de filas por página, para no mandar la agenda entera de un saque. */
    private static final int MAX_PAGE_SIZE = 200;

    private final AppointmentRepository appointmentRepository;
    private final ServiceOfferingRepository serviceRepository;
    private final BusinessHoursRepository businessHoursRepository;
    private final BookingService bookingService;
    private final AppProperties properties;
    private final Clock clock;

    public AdminController(AppointmentRepository appointmentRepository,
                           ServiceOfferingRepository serviceRepository,
                           BusinessHoursRepository businessHoursRepository,
                           BookingService bookingService,
                           AppProperties properties,
                           Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.serviceRepository = serviceRepository;
        this.businessHoursRepository = businessHoursRepository;
        this.bookingService = bookingService;
        this.properties = properties;
        this.clock = clock;
    }

    // =====================================================================
    // TURNOS
    // =====================================================================

    /**
     * Listado de turnos, con filtros opcionales.
     *
     * <p>Si no se pasa {@code from}, arranca desde el inicio del día de hoy.
     * Si no se pasa {@code to}, muestra los próximos días.
     */
    @GetMapping("/appointments")
    public List<AdminAppointmentResponse> listAppointments(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {

        PageRequest pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(Math.max(1, size), MAX_PAGE_SIZE));

        LocalDateTime fromAt = (from != null ? from : LocalDate.now(clock))
                .atStartOfDay();
        LocalDateTime toAt = (to != null ? to : LocalDate.now(clock).plusDays(7))
                .plusDays(1)
                .atStartOfDay();

        List<Appointment> appointments = status != null
                ? appointmentRepository.findByStatus(status, pageable)
                : appointmentRepository.findBetween(fromAt, toAt, pageable);

        return appointments.stream()
                .map(AdminAppointmentResponse::from)
                .toList();
    }

    /**
     * Detalle de un turno. A diferencia del listado, acá el teléfono va
     * COMPLETO y se incluye el enlace de WhatsApp: es la pantalla donde el
     * dueño necesita el número para avisarle al cliente.
     */
    @GetMapping("/appointments/{id}")
    public AdminAppointmentDetailResponse getAppointment(@PathVariable Long id) {
        return appointmentRepository.findDetailedById(id)
                .map(appointment -> AdminAppointmentDetailResponse.from(
                        appointment,
                        properties.getBusinessName(),
                        properties.getAddress()))
                .orElseThrow(() -> ResourceNotFoundException.appointment(id));
    }

    /** Confirma un turno pendiente. */
    @PatchMapping("/appointments/{id}/confirm")
    public ResponseEntity<AppointmentStatusResponse> confirm(@PathVariable Long id) {
        Appointment appointment = bookingService.changeStatus(id, AppointmentStatus.CONFIRMED, clock);
        return ResponseEntity.ok(AppointmentStatusResponse.from(appointment));
    }

    /** Marca un turno como completado. */
    @PatchMapping("/appointments/{id}/complete")
    public ResponseEntity<AppointmentStatusResponse> complete(@PathVariable Long id) {
        Appointment appointment = bookingService.changeStatus(id, AppointmentStatus.COMPLETED, clock);
        return ResponseEntity.ok(AppointmentStatusResponse.from(appointment));
    }

    /** Cancela un turno y libera el horario. */
    @PostMapping("/appointments/{id}/cancel")
    public ResponseEntity<AppointmentStatusResponse> cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelAppointmentRequest request) {

        Appointment appointment = bookingService.cancel(
                id, request.reason().trim(), clock);
        return ResponseEntity.ok(AppointmentStatusResponse.from(appointment));
    }

    // =====================================================================
    // SERVICIOS
    // =====================================================================

    /** Todos los servicios, incluidos los desactivados. */
    @GetMapping("/services")
    public List<ServiceResponse> listServices() {
        return serviceRepository.findAll().stream()
                .sorted(java.util.Comparator.comparingInt(
                        service -> service.getSortOrder()))
                .map(service -> ServiceResponse.from(service, properties.getCurrency()))
                .toList();
    }

    @PostMapping("/services")
    public ResponseEntity<ServiceResponse> createService(
            @Valid @RequestBody ServiceRequest request) {

        var service = serviceRepository.save(new com.barbertrend.domain.ServiceOffering(
                request.name().trim(),
                request.description(),
                request.price(),
                request.durationMinutes(),
                request.sortOrder() == null ? 0 : request.sortOrder()));

        if (request.active() != null) {
            service.setActive(request.active());
            serviceRepository.save(service);
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ServiceResponse.from(service, properties.getCurrency()));
    }

    @PutMapping("/services/{id}")
    public ServiceResponse updateService(@PathVariable Long id,
                                         @Valid @RequestBody ServiceRequest request) {
        var service = serviceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.service(id));

        request.applyTo(service);
        return ServiceResponse.from(serviceRepository.save(service), properties.getCurrency());
    }

    /**
     * Activa o desactiva un servicio.
     *
     * <p>No se borran: los turnos ya reservados apuntan al servicio, y
     * borrarlo rompería el historial.
     */
    @PatchMapping("/services/{id}/active")
    public ServiceResponse setServiceActive(@PathVariable Long id,
                                            @RequestParam boolean active) {
        var service = serviceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.service(id));

        service.setActive(active);
        return ServiceResponse.from(serviceRepository.save(service), properties.getCurrency());
    }

    // =====================================================================
    // HORARIO DE ATENCIÓN
    // =====================================================================

    @GetMapping("/business-hours")
    public BusinessHoursResponse getBusinessHours() {
        return BusinessHoursResponse.from(
                businessHoursRepository.findAllByOrderByDayOfWeekAsc());
    }

    @PutMapping("/business-hours/{dayOfWeek}")
    @Transactional
    public BusinessHoursResponse.DayHours updateBusinessHours(
            @PathVariable int dayOfWeek,
            @RequestBody BusinessHoursResponse.DayHoursRequest request) {

        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new IllegalArgumentException(
                    "El día tiene que estar entre 1 (lunes) y 7 (domingo).");
        }

        if (!request.isValid()) {
            throw new IllegalArgumentException(
                    "Si el día no está cerrado, tiene que tener hora de apertura "
                            + "y de cierre, y el cierre tiene que ser posterior.");
        }

        var hours = businessHoursRepository.findByDayOfWeek(dayOfWeek)
                .orElseThrow(() -> new IllegalStateException(
                        "No está cargado el horario del día " + dayOfWeek + "."));

        hours.setOpenTime(request.openTime());
        hours.setCloseTime(request.closeTime());
        hours.setClosed(request.closed());

        return BusinessHoursResponse.DayHours.from(businessHoursRepository.save(hours));
    }
}
