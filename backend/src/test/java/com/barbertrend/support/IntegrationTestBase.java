package com.barbertrend.support;

import com.barbertrend.domain.BusinessHours;
import com.barbertrend.repository.AppointmentRepository;
import com.barbertrend.repository.AppointmentSlotRepository;
import com.barbertrend.repository.BusinessHoursRepository;
import com.barbertrend.repository.CustomerRepository;
import com.barbertrend.repository.TimeOffRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base de las pruebas de integración.
 *
 * <p><b>Por qué no lleva {@code @Transactional}:</b> una prueba de
 * concurrencia necesita que cada petición corra en su PROPIA transacción, como
 * en producción. Si la prueba abriera una transacción y la envolviera
 * todo, todo se ejecutaría en una sola y la prueba no probaría nada.
 *
 * <p>Por eso, en cambio, cada prueba limpia la base antes de empezar.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Autowired
    protected AppointmentRepository appointmentRepository;

    @Autowired
    protected AppointmentSlotRepository slotRepository;

    @Autowired
    protected CustomerRepository customerRepository;

    @Autowired
    protected BusinessHoursRepository businessHoursRepository;

    @Autowired
    protected TimeOffRepository timeOffRepository;

    @Autowired
    protected Clock clock;

    @BeforeEach
    void cleanDatabase() {
        // El orden importa por las claves foráneas: primero los bloques,
        // después los turnos, y por último los clientes. Los cierres de
        // agenda no dependen de un turno, pero se limpian para que una
        // prueba no vea los bloqueos de otra.
        timeOffRepository.deleteAllInBatch();
        slotRepository.deleteAllInBatch();
        appointmentRepository.deleteAllInBatch();
        customerRepository.deleteAllInBatch();
    }

    /**
     * Devuelve el próximo día abierto que todavía se puede reservar.
     *
     * <p>No se usa una fecha fija porque las pruebas tienen que seguir
     * funcionando dentro de un mes, dentro de un año y un domingo a la
     * noche. Si la fecha estuviera fija, la prueba empezaría a fallar sola.
     */
    protected LocalDate nextBookableDay() {
        LocalDate candidate = LocalDate.now(clock).plusDays(1);

        for (int i = 0; i < 14; i++) {
            BusinessHours hours = businessHoursRepository
                    .findByDayOfWeek(BusinessHours.toDayOfWeek(candidate.getDayOfWeek()))
                    .orElse(null);

            if (hours != null && !hours.isClosed()) {
                return candidate;
            }
            candidate = candidate.plusDays(1);
        }

        throw new IllegalStateException(
                "No se encontró ningún día abierto en los próximos 14 días. "
                        + "Revisá la migración V2__datos_iniciales.sql");
    }

    /** Primer horario de la grilla de un día abierto (09:00 en la barbería). */
    protected LocalTime firstSlotOf(LocalDate date) {
        return businessHoursRepository
                .findByDayOfWeek(BusinessHours.toDayOfWeek(date.getDayOfWeek()))
                .map(BusinessHours::getOpenTime)
                .orElseThrow();
    }
}
