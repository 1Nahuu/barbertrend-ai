package com.barbertrend.web;

import com.barbertrend.config.AppProperties;
import com.barbertrend.domain.ServiceOffering;
import com.barbertrend.dto.AppointmentCreatedResponse;
import com.barbertrend.dto.AppointmentStatusResponse;
import com.barbertrend.dto.AvailabilityResponse;
import com.barbertrend.dto.CreateAppointmentRequest;
import com.barbertrend.dto.ServiceResponse;
import com.barbertrend.repository.AppointmentRepository;
import com.barbertrend.repository.ServiceOfferingRepository;
import com.barbertrend.service.AvailabilityService;
import com.barbertrend.service.BookingRateLimiter;
import com.barbertrend.service.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API pública: la que usa el sitio web sin iniciar sesión.
 *
 * <p>El controlador solo se ocupa del HTTP: valida el formato con las
 * anotaciones de {@link Valid} y delega toda la lógica en los servicios.
 * No hay una sola regla de negocio escrita acá.
 */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final ServiceOfferingRepository serviceRepository;
    private final AppointmentRepository appointmentRepository;
    private final AvailabilityService availabilityService;
    private final BookingService bookingService;
    private final BookingRateLimiter rateLimiter;
    private final AppProperties properties;
    private final Clock clock;

    public PublicController(ServiceOfferingRepository serviceRepository,
                            AppointmentRepository appointmentRepository,
                            AvailabilityService availabilityService,
                            BookingService bookingService,
                            BookingRateLimiter rateLimiter,
                            AppProperties properties,
                            Clock clock) {
        this.serviceRepository = serviceRepository;
        this.appointmentRepository = appointmentRepository;
        this.availabilityService = availabilityService;
        this.bookingService = bookingService;
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.clock = clock;
    }

    /** Catálogo de servicios activos. */
    @GetMapping("/services")
    public List<ServiceResponse> listServices() {
        return serviceRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(service -> ServiceResponse.from(service, properties.getCurrency()))
                .toList();
    }

    /** Horarios libres para un servicio en una fecha. */
    @GetMapping("/availability")
    public AvailabilityResponse availability(
            @RequestParam Long serviceId,
            @RequestParam LocalDate date) {

        ServiceOffering service = serviceRepository.findById(serviceId)
                .filter(ServiceOffering::isActive)
                .orElseThrow(() -> com.barbertrend.common.ResourceNotFoundException
                        .service(serviceId));

        AvailabilityService.Availability availability =
                availabilityService.compute(service, date);

        return AvailabilityResponse.from(service, availability,
                properties.getTimezone(), properties.getCurrency());
    }

    /** Crea una reserva. */
    @PostMapping("/appointments")
    public ResponseEntity<AppointmentCreatedResponse> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request,
            HttpServletRequest httpRequest) {

        // Se valida antes de tocar la base: si la IP ya superó el límite,
        // no se procesa la reserva. Se usa la IP de la conexión; detrás de
        // un proxy hay que configurar server.forward-headers-strategy.
        rateLimiter.check(httpRequest.getRemoteAddr());

        var appointment = bookingService.create(
                request.serviceId(),
                request.startsAt(),
                request.customerName().trim(),
                request.phoneDigits(),
                request.notes(),
                clock);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AppointmentCreatedResponse.from(appointment));
    }

    /**
     * Consulta un turno con su código público.
     *
     * <p>Devuelve solo lo necesario para que el cliente sepa que es suyo.
     * Probar códigos ajenos no sirve de nada: la respuesta nunca trae el
     * teléfono completo ni datos de otra persona.
     */
    @GetMapping("/appointments/{reference}")
    public AppointmentStatusResponse getAppointment(@PathVariable String reference) {
        return appointmentRepository.findDetailedByReference(reference)
                .map(AppointmentStatusResponse::from)
                .orElseThrow(() -> com.barbertrend.common.ResourceNotFoundException
                        .reference(reference));
    }
}
