package com.barbertrend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barbertrend.repository.AppointmentRepository;
import com.barbertrend.repository.ServiceOfferingRepository;
import com.barbertrend.support.IntegrationTestBase;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Pruebas del circuito completo de una reserva:
 *
 * <ol>
 *   <li>El sitio público consulta la disponibilidad real.</li>
 *   <li>Reserva un horario y queda PENDING.</li>
 *   <li>El mismo horario se rechaza con 409.</li>
 *   <li>El panel lo lista, lo confirma y recibe el enlace de WhatsApp.</li>
 *   <li>Cancelar libera el horario.</li>
 * </ol>
 *
 * <p>Hereda la limpieza de base de datos de {@link IntegrationTestBase}. El
 * acceso al panel se simula con {@link WithMockUser}: no hace falta pasar
 * por el login en cada prueba.
 */
@AutoConfigureMockMvc
class BookingFlowApiTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ServiceOfferingRepository serviceRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    private Long serviceId() {
        return serviceRepository.findByActiveTrueOrderBySortOrderAsc().get(0).getId();
    }

    private LocalDateTime proximoTurno() {
        LocalDate dia = nextBookableDay();
        return dia.atTime(firstSlotOf(dia));
    }

    private String jsonBooking(LocalDateTime startsAt) {
        return """
                {
                  "serviceId": %d,
                  "startsAt": "%s",
                  "customerName": "Juan Pérez",
                  "phone": "2284239845",
                  "notes": "prueba de reserva"
                }
                """.formatted(serviceId(), startsAt);
    }

    @Test
    @DisplayName("La disponibilidad pública devuelve horarios libres")
    void disponibilidadPublica() throws Exception {
        LocalDate dia = nextBookableDay();

        mvc.perform(get("/api/public/availability")
                        .param("serviceId", String.valueOf(serviceId()))
                        .param("date", dia.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(false))
                .andExpect(jsonPath("$.slots").isArray())
                .andExpect(jsonPath("$.slots[0].startsAt").isNotEmpty());
    }

    @Test
    @DisplayName("Se reserva un horario y el mismo horario se rechaza")
    void reservarYRechazarDuplicado() throws Exception {
        LocalDateTime horario = proximoTurno();

        mvc.perform(post("/api/public/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBooking(horario)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference", startsWith("BT-")));

        mvc.perform(post("/api/public/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBooking(horario)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SLOT_NOT_AVAILABLE"));

        assertThat(appointmentRepository.findAll()).hasSize(1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("El panel lista, confirma y arma el WhatsApp del cliente")
    void panelListaConfirmaYWhatsApp() throws Exception {
        LocalDateTime horario = proximoTurno();

        mvc.perform(post("/api/public/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBooking(horario)))
                .andExpect(status().isCreated());

        Long id = appointmentRepository.findAll().get(0).getId();

        // Listado: el teléfono va enmascarado.
        mvc.perform(get("/api/admin/appointments").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        // Detalle: teléfono completo y enlace wa.me ya armado.
        mvc.perform(get("/api/admin/appointments/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerPhone").value("2284239845"))
                .andExpect(jsonPath("$.clientWhatsAppUrl",
                        containsString("wa.me/5492284239845")));

        Cookie csrf = csrfCookie();

        mvc.perform(patch("/api/admin/appointments/" + id + "/confirm")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Después de confirmar, el mensaje pasa a ser el de confirmación.
        mvc.perform(get("/api/admin/appointments/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.clientWhatsAppUrl",
                        containsString("te%20confirmamos")));

        // Cancelar libera el horario.
        mvc.perform(post("/api/admin/appointments/" + id + "/cancel")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"El cliente avisó que no puede\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(slotRepository.findAll()).isEmpty();
    }

    /**
     * Pide el panel y devuelve la cookie CSRF que escribe Spring Security,
     * igual que hace el navegador antes de una escritura.
     */
    private Cookie csrfCookie() throws Exception {
        MvcResult result = mvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).as("cookie XSRF-TOKEN").isNotNull();
        return cookie;
    }
}
