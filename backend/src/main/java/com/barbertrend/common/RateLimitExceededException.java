package com.barbertrend.common;

import org.springframework.http.HttpStatus;

/**
 * Se superó el límite de intentos de reserva desde una misma IP.
 *
 * <p>Se responde {@code 429 Too Many Requests} y se indica cuántos segundos
 * conviene esperar antes de reintentar. No se revela la IP ni el conteo
 * interno.
 */
public class RateLimitExceededException extends ApiException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("RATE_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS,
                "Hiciste demasiados intentos de reserva. Esperá unos minutos "
                        + "y volvé a probar.", null);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /** Segundos que faltan para que se libere la ventana. */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
