package com.barbertrend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barbertrend.repository.ServiceOfferingRepository;
import com.barbertrend.support.IntegrationTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Pruebas de la administración del catálogo desde el panel.
 *
 * <p>Cubren el alta, la edición, la validación de nombres repetidos y la
 * activación/desactivación. También comprueban que el sitio público solo vea
 * los servicios activos y reciba la duración que necesita la agenda.
 *
 * <p>Los servicios creados en estas pruebas se borran al terminar para no
 * alterar el catálogo real que usan las demás pruebas.
 */
@AutoConfigureMockMvc
class AdminServiceApiTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ServiceOfferingRepository serviceRepository;

    @AfterEach
    void borrarServiciosDePrueba() {
        serviceRepository.deleteAll(serviceRepository.findAll().stream()
                .filter(service -> service.getName().startsWith("Prueba "))
                .toList());
    }

    private String serviceJson(String name, String description, String price,
                               int durationMinutes, Boolean active, int sortOrder) {
        return """
                {
                  "name": "%s",
                  "description": %s,
                  "price": %s,
                  "durationMinutes": %d,
                  "active": %s,
                  "sortOrder": %d
                }
                """.formatted(
                name,
                description == null ? "null" : "\"" + description + "\"",
                price,
                durationMinutes,
                active == null ? "null" : active.toString(),
                sortOrder);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Crea un servicio y el sitio público lo muestra con su duración")
    void creaServicioYApareceEnPublico() throws Exception {
        Cookie csrf = csrfCookie();

        mvc.perform(post("/api/admin/services")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(serviceJson("Prueba corte", "Corte de prueba",
                                "12345.50", 45, true, 9)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Prueba corte"))
                .andExpect(jsonPath("$.durationMinutes").value(45))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.sortOrder").value(9))
                .andExpect(jsonPath("$.currency").value("ARS"));

        mvc.perform(get("/api/public/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Prueba corte')].durationMinutes")
                        .value(contains(45)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("El nombre del servicio no se puede repetir, sin importar mayúsculas")
    void nombreDuplicado() throws Exception {
        Cookie csrf = csrfCookie();

        mvc.perform(post("/api/admin/services")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(serviceJson("Prueba corte", null,
                                "12000", 30, true, 1)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/admin/services")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(serviceJson("prueba CORTE", null,
                                "13000", 30, true, 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_SERVICE_NAME"));

        assertThat(serviceRepository.findAll().stream()
                .filter(service -> service.getName().equalsIgnoreCase("Prueba corte")))
                .hasSize(1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Rechaza nombres vacíos, precios negativos y duraciones inválidas")
    void validaDatosDelServicio() throws Exception {
        Cookie csrf = csrfCookie();

        mvc.perform(post("/api/admin/services")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(serviceJson("   ", null, "12000", 30, true, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mvc.perform(post("/api/admin/services")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(serviceJson("Prueba precio", null,
                                "-1", 30, true, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mvc.perform(post("/api/admin/services")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(serviceJson("Prueba duración", null,
                                "12000", 0, true, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Edita un servicio y la descripción vacía se guarda como nula")
    void actualizaServicio() throws Exception {
        Cookie csrf = csrfCookie();

        MvcResult creado = mvc.perform(post("/api/admin/services")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(serviceJson("Prueba corte", "Descripción original",
                                "12000", 30, true, 1)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = ((Number) com.jayway.jsonpath.JsonPath.read(
                creado.getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(put("/api/admin/services/" + id)
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(serviceJson("Prueba corte editado", "   ",
                                "15000", 60, false, 5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Prueba corte editado"))
                .andExpect(jsonPath("$.description").value(nullValue()))
                .andExpect(jsonPath("$.durationMinutes").value(60))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.sortOrder").value(5));

        mvc.perform(get("/api/public/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Prueba corte editado')]").isEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Activa y desactiva un servicio sin borrar sus datos")
    void activaYDesactiva() throws Exception {
        Cookie csrf = csrfCookie();

        MvcResult creado = mvc.perform(post("/api/admin/services")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(serviceJson("Prueba barba", null,
                                "9000", 30, true, 2)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = ((Number) com.jayway.jsonpath.JsonPath.read(
                creado.getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(patch("/api/admin/services/" + id + "/active")
                        .param("active", "false")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(get("/api/public/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Prueba barba')]").isEmpty());

        mvc.perform(patch("/api/admin/services/" + id + "/active")
                        .param("active", "true")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mvc.perform(get("/api/public/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Prueba barba')].name")
                        .value(contains("Prueba barba")));
    }

    @Test
    @DisplayName("La administración de servicios exige sesión")
    void serviciosRequierenSesion() throws Exception {
        mvc.perform(get("/api/admin/services"))
                .andExpect(status().isUnauthorized());

        Cookie csrf = publicCsrfCookie();

        mvc.perform(post("/api/admin/services")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
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
