package com.barbertrend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.barbertrend.common.SlotNotAvailableException;
import com.barbertrend.support.IntegrationTestBase;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Prueba de la garantía central del sistema: <b>no se pueden superponer
 * dos reservas en el mismo horario</b>, ni siquiera si llegan al mismo
 * tiempo.
 *
 * <h2>Por qué esta prueba importa</h2>
 *
 * <p>El código "pregunto si está libre y si está lo grabo" tiene una
 * condición de carrera. Con dos personas reservando a la vez, las dos ven
 * el horario libre y las dos graban. No es un caso raro ni teórico: es
 * justamente lo que pasa los viernes a la tarde.
 *
 * <p>Esta prueba no verifica que "el código se acuerde de chequear". Verifica
 * que <b>la base de datos</b> rechace el segundo INSERT, por más que las dos
 * peticiones hayan llegado al mismo instante.
 */
class ConcurrentBookingTest extends IntegrationTestBase {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private com.barbertrend.repository.ServiceOfferingRepository serviceRepository;

    @Test
    @DisplayName("20 pedidos simultáneos del mismo horario: solo uno gana")
    void soloUnoGanaLaCarrera() throws Exception {
        final int PETICIONES = 20;

        LocalDate dia = nextBookableDay();
        LocalDateTime horario = dia.atTime(firstSlotOf(dia));

        Long serviceId = serviceRepository.findByActiveTrueOrderBySortOrderAsc()
                .get(0).getId();

        AtomicInteger exitosos = new AtomicInteger();
        AtomicInteger conflictos = new AtomicInteger();
        AtomicInteger otrosErrores = new AtomicInteger();

        ExecutorService executor = Executors.newFixedThreadPool(PETICIONES);

        // CountDownLatch: todos los hilos esperan en la línea de partida y
        // arrancan al mismo tiempo. Sin esto, el primero terminaría antes de
        // que empiece el último y la prueba no probaría la carrera.
        CountDownLatch largada = new CountDownLatch(1);

        List<Callable<Void>> tareas = new java.util.ArrayList<>();
        for (int i = 0; i < PETICIONES; i++) {
            final int numero = i;
            tareas.add(() -> {
                largada.await();
                try {
                    bookingService.create(
                            serviceId, horario,
                            "Cliente " + numero,
                            "549110000" + String.format("%04d", numero),
                            "prueba de concurrencia",
                            clock);
                    exitosos.incrementAndGet();
                } catch (SlotNotAvailableException ex) {
                    // Esto es lo que se espera: la base rechazó el INSERT
                    // porque el bloque ya estaba, y BookingService lo
                    // tradujo al error que la API devuelve como 409.
                    conflictos.incrementAndGet();
                } catch (Exception ex) {
                    otrosErrores.incrementAndGet();
                    System.err.println("Error inesperado #" + numero + ": " + ex);
                }
                return null;
            });
        }

        List<Future<Void>> futuros = tareas.stream().map(executor::submit).toList();
        largada.countDown();          // ¡Dale!

        for (Future<Void> futuro : futuros) {
            futuro.get(60, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // --- El resultado que importa ---
        assertThat(exitosos.get())
                .as("Solo una reserva puede ocupar un mismo horario")
                .isEqualTo(1);

        assertThat(otrosErrores.get())
                .as("No debería haber errores distintos del conflicto esperado")
                .isZero();

        // Y una comprobación directa en la base: hay un solo turno, no
        // quedaron restos de los intentos perdedores.
        assertThat(appointmentRepository.findAll())
                .as("Solo debe quedar el turno ganador")
                .hasSize(1);

        assertThat(slotRepository.findAll())
                .as("Un corte de 30 min bloquea 1 bloque")
                .hasSize(1);
    }

    @Test
    @DisplayName("20 pedidos simultáneos de servicios que se pisan: solo uno gana")
    void serviciosQueSeSuperponenNoSePisan() throws Exception {
        final int PETICIONES = 12;

        AtomicInteger exitosos = new AtomicInteger();
        AtomicInteger conflictos = new AtomicInteger();
        AtomicInteger otrosErrores = new AtomicInteger();

        LocalDate dia = nextBookableDay();
        LocalDateTime horario = dia.atTime(firstSlotOf(dia));

        // Se usa el servicio de 60 minutos, que ocupa 2 bloques (10:00 y
        // 10:30). Aunque nadie pida exactamente las 10:30, dos reservas de
        // 60 minutos sobre 10:00 se pisarían entre sí. Esto es lo que un
        // UNIQUE(barber_id, starts_at) NO detectaría.
        var servicios = serviceRepository.findByActiveTrueOrderBySortOrderAsc();
        Long servicioDeUnaHora = servicios.stream()
                .filter(s -> s.getDurationMinutes() >= 60)
                .map(s -> s.getId())
                .findFirst()
                .orElseThrow();

        ExecutorService executor = Executors.newFixedThreadPool(PETICIONES);
        CountDownLatch largada = new CountDownLatch(1);

        List<Callable<Void>> tareas = new java.util.ArrayList<>();
        for (int i = 0; i < PETICIONES; i++) {
            final int numero = i;
            tareas.add(() -> {
                largada.await();
                try {
                    // Cada uno pide un horario DISTINTO pero que se pisa:
                    // 10:00 ocupa 10:00-11:00, 10:30 ocupa 10:30-11:30.
                    LocalDateTime inicio = horario.plusMinutes(30L * (numero % 3));
                    bookingService.create(
                            servicioDeUnaHora, inicio,
                            "Cliente " + numero,
                            "549110000" + String.format("%04d", numero),
                            null,
                            clock);
                    exitosos.incrementAndGet();
                } catch (SlotNotAvailableException ex) {
                    conflictos.incrementAndGet();
                } catch (Exception ex) {
                    otrosErrores.incrementAndGet();
                    System.err.println("Error inesperado #" + numero + ": " + ex);
                }
                return null;
            });
        }

        List<Future<Void>> futuros = tareas.stream().map(executor::submit).toList();
        largada.countDown();
        for (Future<Void> futuro : futuros) {
            futuro.get(60, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(otrosErrores.get()).as("Sin errores inesperados").isZero();

        // Hay 3 horarios posibles (10:00, 10:30, 11:00) y cada uno ocupa 2
        // bloques. Solo pueden coexistir los que no se tocan:
        //   10:00 -> 10:00, 10:30  (ocupa 2 bloques)
        //   11:00 -> 11:00, 11:30  (ocupa 2 bloques)
        // 10:30 se pisa con 10:00, así que en el mejor caso quedan 2 turnos.
        assertThat(exitosos.get())
                .as("No pueden coexistir reservas que se superponen entre sí")
                .isLessThanOrEqualTo(2);

        assertThat(exitosos.get() + conflictos.get())
                .as("Cada petición debe terminar en éxito o en conflicto")
                .isEqualTo(PETICIONES);

        // Verificación final: ningún bloque puede estar repetido.
        var bloques = slotRepository.findAll().stream()
                .map(s -> s.getId().getBarberId() + "@" + s.getId().getSlotStart())
                .toList();

        assertThat(bloques)
                .as("La clave primaria compuesta garantiza bloques únicos")
                .doesNotHaveDuplicates();
    }
}
