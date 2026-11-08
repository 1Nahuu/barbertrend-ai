package com.barbertrend.service;

import com.barbertrend.common.RateLimitExceededException;
import com.barbertrend.config.AppProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Limita cuántas reservas puede intentar crear una misma IP.
 *
 * <h2>Por qué en memoria</h2>
 *
 * <p>El proyecto corre en una sola instancia, así que un mapa en memoria
 * alcanza y evita agregar Redis o una tabla solo para esto. La limitación
 * está documentada: con varias instancias, cada una contaría por separado
 * y habría que pasar el conteo a un almacenamiento compartido.
 *
 * <h2>Cómo cuenta</h2>
 *
 * <p>Usa una ventana fija: la primera petición de una IP abre la ventana y
 * las siguientes se cuentan hasta que pasa el tiempo configurado. Es
 * suficiente para frenar el abuso del formulario público; no pretende ser
 * un sistema de cuotas exacto.
 *
 * <p>Se cuenta <b>todo intento</b>, aunque después falle por una regla de
 * negocio. Así no se puede evitar el límite mandando datos inválidos.
 */
@Service
public class BookingRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(BookingRateLimiter.class);

    /** A partir de este tamaño se limpian las ventanas vencidas. */
    private static final int CLEANUP_THRESHOLD = 1000;

    private final AppProperties properties;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public BookingRateLimiter(AppProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Registra un intento y lanza {@link RateLimitExceededException} si la IP
     * ya superó el máximo.
     *
     * @param clientKey IP del cliente (o el identificador que se use para
     *                  agrupar los intentos)
     */
    public void check(String clientKey) {
        AppProperties.RateLimit config = properties.getRateLimit();
        if (!config.isEnabled() || config.getBookingMaxAttempts() <= 0) {
            return;
        }

        Duration windowDuration = Duration.ofMinutes(
                Math.max(1, config.getBookingWindowMinutes()));
        Instant now = clock.instant();
        String key = normalize(clientKey);

        Window window = windows.compute(key, (ignored, current) -> {
            if (current == null || current.isExpired(now, windowDuration)) {
                return new Window(now);
            }
            current.attempts++;
            return current;
        });

        if (window.attempts > config.getBookingMaxAttempts()) {
            log.warn("Límite de reservas alcanzado para {} ({} intentos).",
                    mask(key), window.attempts);
            throw new RateLimitExceededException(
                    window.retryAfterSeconds(now, windowDuration));
        }

        cleanupIfNeeded(now, windowDuration);
    }

    private void cleanupIfNeeded(Instant now, Duration windowDuration) {
        if (windows.size() <= CLEANUP_THRESHOLD) {
            return;
        }
        windows.entrySet().removeIf(entry -> entry.getValue().isExpired(now, windowDuration));
    }

    private static String normalize(String clientKey) {
        return clientKey == null || clientKey.isBlank() ? "desconocido" : clientKey.trim();
    }

    /** Oculta la última parte de la IP en los logs. */
    private static String mask(String key) {
        int lastDot = key.lastIndexOf('.');
        if (lastDot > 0) {
            return key.substring(0, lastDot) + ".x";
        }
        return "IP";
    }

    /** Una ventana fija: cuándo empezó y cuántos intentos lleva. */
    private static final class Window {

        private final Instant startedAt;
        private int attempts = 1;

        private Window(Instant startedAt) {
            this.startedAt = startedAt;
        }

        private boolean isExpired(Instant now, Duration duration) {
            return !now.isBefore(startedAt.plus(duration));
        }

        private long retryAfterSeconds(Instant now, Duration duration) {
            long seconds = Duration.between(now, startedAt.plus(duration)).toSeconds();
            return Math.max(1, seconds);
        }
    }
}
