package com.barbertrend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.barbertrend.common.RateLimitExceededException;
import com.barbertrend.config.AppProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookingRateLimiterTest {

    private MutableClock clock;
    private AppProperties properties;
    private BookingRateLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-11-08T12:00:00Z"), ZoneOffset.UTC);
        properties = new AppProperties();
        properties.getRateLimit().setEnabled(true);
        properties.getRateLimit().setBookingMaxAttempts(2);
        properties.getRateLimit().setBookingWindowMinutes(10);
        limiter = new BookingRateLimiter(properties, clock);
    }

    @Test
    @DisplayName("Permite hasta el máximo y bloquea el intento siguiente")
    void permiteHastaElMaximoYBloqueaElSiguiente() {
        limiter.check("190.1.1.1");
        limiter.check("190.1.1.1");

        RateLimitExceededException error = assertThatExceptionOfType(
                RateLimitExceededException.class)
                .isThrownBy(() -> limiter.check("190.1.1.1"))
                .actual();

        assertThat(error.getCode()).isEqualTo("RATE_LIMIT_EXCEEDED");
        assertThat(error.getStatus().value()).isEqualTo(429);
        assertThat(error.getRetryAfterSeconds()).isBetween(1L, 600L);
    }

    @Test
    @DisplayName("Al terminar la ventana se vuelve a permitir")
    void liberaAlPasarLaVentana() {
        limiter.check("190.1.1.1");
        limiter.check("190.1.1.1");

        clock.advance(Duration.ofMinutes(10).plusSeconds(1));

        assertThatCode(() -> limiter.check("190.1.1.1")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Cada IP tiene su propio contador")
    void ipsDistintasNoSeAfectan() {
        limiter.check("190.1.1.1");
        limiter.check("190.1.1.1");

        assertThatCode(() -> limiter.check("190.1.1.2")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Con el límite desactivado no bloquea")
    void deshabilitadoNoLimita() {
        properties.getRateLimit().setEnabled(false);

        for (int i = 0; i < 100; i++) {
            limiter.check("190.1.1.1");
        }
    }

    @Test
    @DisplayName("El reintento informa los segundos que faltan")
    void informaSegundosDeEspera() {
        limiter.check("190.1.1.1");
        limiter.check("190.1.1.1");
        clock.advance(Duration.ofMinutes(4));

        RateLimitExceededException error = assertThatExceptionOfType(
                RateLimitExceededException.class)
                .isThrownBy(() -> limiter.check("190.1.1.1"))
                .actual();

        assertThat(error.getRetryAfterSeconds()).isBetween(300L, 360L);
    }

    /** Reloj controlable para probar el paso del tiempo sin dormir. */
    private static final class MutableClock extends Clock {

        private Instant instant;
        private final ZoneId zone;

        private MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId newZone) {
            return new MutableClock(instant, newZone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
