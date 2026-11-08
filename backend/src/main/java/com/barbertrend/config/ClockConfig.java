package com.barbertrend.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj de la aplicación.
 *
 * <p>Nadie debería llamar a {@code LocalDateTime.now()} directamente: si lo
 * hace, las pruebas dependen del día en que se ejecutan y no se puede
 * probar qué pasa "a las 18:00" ni "el domingo". Inyectando el reloj, una
 * prueba puede usar uno fijo y comprobar el comportamiento en cualquier
 * momento.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock barberClock(AppProperties properties) {
        return Clock.system(properties.getZoneId());
    }
}
