package com.barbertrend.config;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Alinea la zona horaria del JVM con la zona de la barbería.
 *
 * <p><b>Por qué existe:</b> Hibernate se configura con
 * {@code hibernate.jdbc.time_zone = APP_TIMEZONE}, porque las fechas y horas
 * de la agenda se guardan como hora local. Las columnas {@code TIME} se
 * convierten usando la zona por defecto del JVM. Si el JVM corre en UTC
 * (habitual en servidores Linux y en GitHub Actions) y la barbería está en
 * Argentina, un horario de 09:00 se lee como 12:00 y la agenda queda
 * desplazada tres horas.
 *
 * <p>Esta clase se ejecuta después de cargar {@code application.yml} y
 * {@code .env}, y antes de crear el contexto de Spring, así que Hibernate ya
 * encuentra la zona correcta. En desarrollo, donde el JVM suele estar en la
 * misma zona, no cambia nada.
 */
public class TimeZoneEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String DEFAULT_TIMEZONE = "America/Argentina/Buenos_Aires";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        String timezone = environment.getProperty("APP_TIMEZONE");
        if (timezone == null || timezone.isBlank()) {
            timezone = environment.getProperty("app.timezone", DEFAULT_TIMEZONE);
        }
        TimeZone.setDefault(TimeZone.getTimeZone(timezone));
    }

    @Override
    public int getOrder() {
        // Después de ConfigDataEnvironmentPostProcessor (HIGHEST_PRECEDENCE + 10),
        // que es el que carga application.yml y el .env.
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
