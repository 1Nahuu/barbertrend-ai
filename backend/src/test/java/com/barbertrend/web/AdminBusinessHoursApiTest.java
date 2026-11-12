package com.barbertrend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barbertrend.domain.BusinessHours;
import com.barbertrend.repository.ServiceOfferingRepository;
import com.barbertrend.support.IntegrationTestBase;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Pruebas de la edición del horario de atención desde el panel.
 *
 * <p>El horario es la fuente real del sitio público y del cálculo de
 * disponibilidad. Por eso se comprueba que: se puedan cerrar días, que el
 * cambio se refleje en la API pública, que la grilla empiece y termine en las
 * horas configuradas y que los datos incoherentes se rechacen.
 *
 * <p>Cada prueba restaura el horario original al terminar, para que las
 * demás pruebas sigan viendo la semana completa del local.
 */
@AutoConfigureMockMvc
class AdminBusinessHoursApiTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ServiceOfferingRepository serviceRepository;

    private Map<Integer, DayState> horarioOriginal;

    private record DayState(LocalTime openTime, LocalTime closeTime, boolean closed) {
    }

    @BeforeEach
    void guardarHorarioOriginal() {
        horarioOriginal = new HashMap<>();
        businessHoursRepository.findAll().forEach(hours ->
                horarioOriginal.put(hours.getDayOfWeek(), new DayState(
                        hours.getOpenTime(), hours.getCloseTime(), hours.isClosed())));
    }

    @AfterEach
    void restaurarHorarioOriginal() {
        horarioOriginal.forEach((dayOfWeek, state) -> {
            BusinessHours hours = businessHoursRepository.findByDayOfWeek(dayOfWeek)
                    .orElseThrow();
            hours.setOpenTime(state.openTime());
            hours.setCloseTime(state.closeTime());
            hours.setClosed(state.closed());
            businessHoursRepository.saveAndFlush(hours);
        });
    }

    private String hoursJson(String openTime, String closeTime, boolean closed) {
        return """
                {
                  "openTime": %s,
                  "closeTime": %s,
                  "closed": %s
                }
                """.formatted(
                openTime == null ? "null" : "\"" + openTime + "\"",
                closeTime == null ? "null" : "\"" + closeTime + "\"",
                closed);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Devuelve los siete días, incluidos los cerrados")
    void devuelveLosSieteDias() throws Exception {
        mvc.perform(get("/api/admin/business-hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days.length()").value(7))
                .andExpect(jsonPath("$.days[0].dayOfWeek").value(1))
                .andExpect(jsonPath("$.days[0].dayName").value("lunes"))
                .andExpect(jsonPath("$.days[6].dayOfWeek").value(7))
                .andExpect(jsonPath("$.days[6].closed").value(true))
                .andExpect(jsonPath("$.days[6].openTime").value(nullValue()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Cierra un día y la web pública deja de ofrecer turnos")
    void cierraUnDia() throws Exception {
        LocalDate dia = nextBookableDay();
        int dayOfWeek = BusinessHours.toDayOfWeek(dia.getDayOfWeek());
        Cookie csrf = csrfCookie();

        mvc.perform(put("/api/admin/business-hours/" + dayOfWeek)
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hoursJson(null, null, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(true))
                .andExpect(jsonPath("$.openTime").value(nullValue()))
                .andExpect(jsonPath("$.closeTime").value(nullValue()));

        mvc.perform(get("/api/public/business-hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[" + (dayOfWeek - 1) + "].closed").value(true));

        mvc.perform(get("/api/public/availability")
                        .param("serviceId", String.valueOf(serviceId()))
                        .param("date", dia.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(true))
                .andExpect(jsonPath("$.slots").isEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Cambia el horario y la disponibilidad usa esa ventana")
    void cambiaElHorario() throws Exception {
        LocalDate dia = nextBookableDay();
        int dayOfWeek = BusinessHours.toDayOfWeek(dia.getDayOfWeek());
        Cookie csrf = csrfCookie();

        mvc.perform(put("/api/admin/business-hours/" + dayOfWeek)
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hoursJson("10:00", "12:00", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(false))
                .andExpect(jsonPath("$.openTime").value(startsWith("10:00")))
                .andExpect(jsonPath("$.closeTime").value(startsWith("12:00")));

        mvc.perform(get("/api/public/business-hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[" + (dayOfWeek - 1) + "].openTime")
                        .value(startsWith("10:00")));

        mvc.perform(get("/api/public/availability")
                        .param("serviceId", String.valueOf(serviceId()))
                        .param("date", dia.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(false))
                .andExpect(jsonPath("$.slots[0].startsAt").value(dia + "T10:00:00"))
                .andExpect(jsonPath("$.slots[?(@.startsAt == '" + dia + "T09:00:00')]")
                        .isEmpty())
                .andExpect(jsonPath("$.slots[?(@.startsAt == '" + dia + "T12:00:00')]")
                        .isEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Rechaza días fuera de rango, rangos incoherentes y horas fuera de la grilla")
    void validaHorario() throws Exception {
        Cookie csrf = csrfCookie();

        mvc.perform(put("/api/admin/business-hours/0")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hoursJson("09:00", "18:00", false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BUSINESS_HOURS"));

        mvc.perform(put("/api/admin/business-hours/8")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hoursJson("09:00", "18:00", false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BUSINESS_HOURS"));

        mvc.perform(put("/api/admin/business-hours/1")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hoursJson(null, "18:00", false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BUSINESS_HOURS"));

        mvc.perform(put("/api/admin/business-hours/1")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hoursJson("18:00", "09:00", false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BUSINESS_HOURS"));

        mvc.perform(put("/api/admin/business-hours/1")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hoursJson("09:15", "21:00", false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BUSINESS_HOURS"));
    }

    @Test
    @DisplayName("La edición del horario exige sesión")
    void horarioRequiereSesion() throws Exception {
        mvc.perform(get("/api/admin/business-hours"))
                .andExpect(status().isUnauthorized());

        Cookie csrf = publicCsrfCookie();

        mvc.perform(put("/api/admin/business-hours/1")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hoursJson("09:00", "18:00", false)))
                .andExpect(status().isUnauthorized());
    }

    private Long serviceId() {
        return serviceRepository.findByActiveTrueOrderBySortOrderAsc().get(0).getId();
    }

    private Cookie csrfCookie() throws Exception {
        MvcResult result = mvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).as("cookie XSRF-TOKEN").isNotNull();
        return cookie;
    }

    private Cookie publicCsrfCookie() throws Exception {
        MvcResult result = mvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).as("cookie XSRF-TOKEN").isNotNull();
        return cookie;
    }
}
