package com.barbertrend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de BarberTrend AI.
 *
 * <p>En la Etapa 2 esta clase solo sirve para levantar el sitio web público que
 * vive en {@code src/main/resources/static}. La base de datos, la API de
 * reservas y el panel del dueño se agregan en la Etapa 3.
 *
 * <p>Se ejecuta con: {@code ./mvnw spring-boot:run}
 */
@SpringBootApplication
public class BarberTrendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BarberTrendApplication.class, args);
    }
}
