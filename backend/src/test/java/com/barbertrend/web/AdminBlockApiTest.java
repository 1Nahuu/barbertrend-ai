package com.barbertrend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barbertrend.repository.AppointmentRepository;
import com.barbertrend.repository.ServiceOfferingRepository;
import com.barbertrend.support.IntegrationTestBase;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Pruebas del cierre de agenda desde el panel.
 *
 * <p>Cubren el circuito completo: crear un cierre, verlo en la lista,
 * comprobar que la disponibilidad pública lo respeta, impedir reservas
 * directas en horarios bloqueados y liberar la agenda al eliminar el cierre.
 */
@AutoConfigureMockMvc
class AdminBlockApiTest extends IntegrationTestBase {

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

    private String blockJson(LocalDate startDate, LocalDate endDate,
                             boolean allDay, LocalTime startTime,
                             LocalTime endTime, String reason) {
        return """
                {
                  "startDate": "%s",
                  "endDate": %s,
                  "allDay": %s,
                  "startTime": %s,
                  "endTime": %s,
                  "reason": %s
                }
                """.formatted(
                startDate,
                endDate == null ? "null" : "\"" + endDate + "\"",
                allDay,
                startTime == null ? "null" : "\"" + startTime + "\"",
                endTime == null ? "null" : "\"" + endTime + "\"",
                reason == null ? "null" : "\"" + reason + "\"");
    }

    private String bookingJson(LocalDateTime startsAt) {
        return """
                {
                  "serviceId": %d,
                  "startsAt": "%s",
                  "customerName": "Juan Pérez",
                  "phone": "2284239845",
                  "notes": "prueba de cierre"
                }
                """.formatted(serviceId(), startsAt);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Cierra un día completo y la disponibilidad queda cerrada")
    void cierraDiaCompleto() throws Exception {
        LocalDate dia = nextBookableDay();
        Cookie csrf = csrfCookie();

        mvc.perform(post("/api/admin/blocks")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockJson(dia, null, true, null, null, "Feriado")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.allDay").value(true))
                .andExpect(jsonPath("$.reason").value("Feriado"));

        mvc.perform(get("/api/admin/blocks")
                        .cookie(csrf))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].allDay").value(true));

        mvc.perform(get("/api/public/availability")
                        .param("serviceId", String.valueOf(serviceId()))
                        .param("date", dia.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(true))
                .andExpect(jsonPath("$.slots").isEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Cierra una franja horaria y solo desaparecen esos horarios")
    void cierraFranjaHoraria() throws Exception {
        LocalDate dia = nextBookableDay();
        LocalTime inicio = firstSlotOf(dia);
        LocalTime fin = inicio.plusHours(1);
        Cookie csrf = csrfCookie();

        mvc.perform(post("/api/admin/blocks")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockJson(dia, null, false, inicio, fin, "Almuerzo")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.allDay").value(false));

        MvcResult result = mvc.perform(get("/api/public/availability")
                        .param("serviceId", String.valueOf(serviceId()))
                        .param("date", dia.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(false))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain(dia.atTime(inicio).toString());
        assertThat(body).contains(dia.atTime(fin).toString());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Una reserva directa en un horario bloqueado se rechaza")
    void reservaDirectaBloqueada() throws Exception {
        LocalDate dia = nextBookableDay();
        LocalDateTime horario = dia.atTime(firstSlotOf(dia));
        Cookie csrf = csrfCookie();

        mvc.perform(post("/api/admin/blocks")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockJson(dia, null, true, null, null, null)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/public/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(horario)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BLOCKED_TIME"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("No se puede cerrar un día con turnos activos")
    void noCierraConTurnosActivos() throws Exception {
        LocalDate dia = nextBookableDay();
        LocalDateTime horario = dia.atTime(firstSlotOf(dia));
        Cookie csrf = csrfCookie();

        mvc.perform(post("/api/public/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(horario)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/admin/blocks")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockJson(dia, null, true, null, null, "Feriado")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BLOCK_HAS_APPOINTMENTS"));

        Long appointmentId = appointmentRepository.findAll().get(0).getId();
        mvc.perform(post("/api/admin/appointments/" + appointmentId + "/cancel")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Cierre del local\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/admin/blocks")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockJson(dia, null, true, null, null, "Feriado")))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("No se permiten cierres superpuestos")
    void noPermiteSuperposicion() throws Exception {
        LocalDate dia = nextBookableDay();
        LocalTime inicio = firstSlotOf(dia);
        Cookie csrf = csrfCookie();

        mvc.perform(post("/api/admin/blocks")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockJson(dia, null, false, inicio, inicio.plusHours(2), null)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/admin/blocks")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockJson(dia, null, false, inicio.plusHours(1),
                                inicio.plusHours(3), null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BLOCK_OVERLAP"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Eliminar el cierre vuelve a liberar la agenda")
    void eliminarCierreLiberaAgenda() throws Exception {
        LocalDate dia = nextBookableDay();
        Cookie csrf = csrfCookie();

        MvcResult created = mvc.perform(post("/api/admin/blocks")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockJson(dia, null, true, null, null, null)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = ((Number) com.jayway.jsonpath.JsonPath.read(
                created.getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(delete("/api/admin/blocks/" + id)
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNoContent());

        mvc.perform(delete("/api/admin/blocks/" + id)
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BLOCK_NOT_FOUND"));

        mvc.perform(get("/api/public/availability")
                        .param("serviceId", String.valueOf(serviceId()))
                        .param("date", dia.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(false))
                .andExpect(jsonPath("$.slots").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Se puede cerrar un rango de varios días")
    void cierraVariosDias() throws Exception {
        LocalDate desde = nextBookableDay();
        LocalDate hasta = desde.plusDays(1);
        Cookie csrf = csrfCookie();

        mvc.perform(post("/api/admin/blocks")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockJson(desde, hasta, true, null, null, "Vacaciones")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.endsAt").value(hasta.plusDays(1) + "T00:00:00"));

        mvc.perform(get("/api/public/availability")
                        .param("serviceId", String.valueOf(serviceId()))
                        .param("date", desde.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Valida el rango y la alineación de las horas")
    void validaRangoYHoras() throws Exception {
        LocalDate dia = nextBookableDay();
        Cookie csrf = csrfCookie();

        mvc.perform(post("/api/admin/blocks")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockJson(dia, null, false,
                                LocalTime.of(15, 0), LocalTime.of(14, 0), null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_BLOCK"));

        mvc.perform(post("/api/admin/blocks")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockJson(dia, null, false,
                                LocalTime.of(15, 15), LocalTime.of(16, 0), null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_BLOCK"));

        mvc.perform(post("/api/admin/blocks")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockJson(dia.plusDays(2), dia.plusDays(1),
                                true, null, null, null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_BLOCK"));

        mvc.perform(post("/api/admin/blocks")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockJson(dia, dia.plusDays(1), false,
                                LocalTime.of(13, 0), LocalTime.of(14, 0), null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_BLOCK"));

        mvc.perform(post("/api/admin/blocks")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockJson(LocalDate.now(clock).minusDays(1),
                                null, true, null, null, null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BLOCK_IN_PAST"));
    }

    @Test
    @DisplayName("Los cierres exigen sesión iniciada")
    void cierresRequierenSesion() throws Exception {
        mvc.perform(get("/api/admin/blocks"))
                .andExpect(status().isUnauthorized());

        Cookie csrf = publicCsrfCookie();

        mvc.perform(post("/api/admin/blocks")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(delete("/api/admin/blocks/1")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Pide el panel autenticado y devuelve la cookie CSRF que escribe Spring
     * Security, igual que hace el navegador antes de una escritura.
     */
    private Cookie csrfCookie() throws Exception {
        MvcResult result = mvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).as("cookie XSRF-TOKEN").isNotNull();
        return cookie;
    }

    /**
     * Pide el login público para obtener un token CSRF sin sesión. Así se
     * puede comprobar que, aun con un token válido, la API privada exige
     * autenticación.
     */
    private Cookie publicCsrfCookie() throws Exception {
        MvcResult result = mvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).as("cookie XSRF-TOKEN").isNotNull();
        return cookie;
    }
}
