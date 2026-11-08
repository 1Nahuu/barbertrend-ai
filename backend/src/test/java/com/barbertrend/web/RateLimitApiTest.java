package com.barbertrend.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.barbertrend.repository.ServiceOfferingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba el límite de intentos sobre la API real.
 *
 * <p>Se usa una fecha pasada a propósito: el intento llega al limitador y
 * después falla por una regla de negocio, sin crear turnos. Así se prueba
 * que se cuentan <b>todos</b> los intentos, no solo los exitosos.
 */
@SpringBootTest(properties = {
        "app.rate-limit.booking-max-attempts=2",
        "app.rate-limit.booking-window-minutes=10"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ServiceOfferingRepository serviceRepository;

    @Test
    @DisplayName("Después del máximo de intentos responde 429 con Retry-After")
    void bloqueaDespuesDelMaximo() throws Exception {
        Long serviceId = serviceRepository.findByActiveTrueOrderBySortOrderAsc()
                .get(0).getId();

        String body = """
                {
                  "serviceId": %d,
                  "startsAt": "2020-01-01T09:00:00",
                  "customerName": "Prueba de límite",
                  "phone": "2284239845",
                  "notes": ""
                }
                """.formatted(serviceId);

        // Los primeros dos intentos pasan el limitador y fallan por fecha
        // pasada. El tercero ya no llega a la regla de negocio.
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/public/appointments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("PAST_DATE"));
        }

        mvc.perform(post("/api/public/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
    }
}
