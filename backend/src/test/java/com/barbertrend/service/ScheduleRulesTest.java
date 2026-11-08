package com.barbertrend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.barbertrend.config.AppProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de las reglas de la grilla.
 *
 * <p>No arranca Spring: son cálculos puros, así que corren en milisegundos.
 * Es la ventaja de dejar las reglas en una clase aparte sin base de datos.
 */
class ScheduleRulesTest {

    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private ScheduleRules rulesWith(int slotMinutes) {
        AppProperties properties = new AppProperties();
        properties.setSlotMinutes(slotMinutes);
        properties.setTimezone(ZONE.getId());
        return new ScheduleRules(properties);
    }

    @Nested
    @DisplayName("Redondeo de la duración")
    class Redondeo {

        private final ScheduleRules rules = rulesWith(30);

        @Test
        @DisplayName("30 min ocupan 1 bloque")
        void treintaMinutosUnBloque() {
            assertThat(rules.blocksNeeded(30)).isEqualTo(1);
            assertThat(rules.occupiedMinutes(30)).isEqualTo(30);
        }

        @Test
        @DisplayName("60 min ocupan 2 bloques")
        void sesentaMinutosDosBloques() {
            assertThat(rules.blocksNeeded(60)).isEqualTo(2);
            assertThat(rules.occupiedMinutes(60)).isEqualTo(60);
        }

        @Test
        @DisplayName("45 min ocupan 2 bloques, no 1.5")
        void cuarentaYCincoMinutosRedondeaArriba() {
            // El punto clave: un corte de 45 minutos ocupa 60 en la agenda.
            // Si se redondeara al más cercano, el barbero terminaría con el
            // cliente siguiente esperando 15 minutos en la silla.
            assertThat(rules.blocksNeeded(45)).isEqualTo(2);
            assertThat(rules.occupiedMinutes(45)).isEqualTo(60);
        }

        @Test
        @DisplayName("90 min ocupan 3 bloques")
        void noventaMinutosTresBloques() {
            assertThat(rules.blocksNeeded(90)).isEqualTo(3);
        }

        @Test
        @DisplayName("1 min ocupa 1 bloque entero")
        void unMinitoUnBloqueCompleto() {
            // El mínimo real de un servicio es 15 minutos, pero la regla
            // tiene que ser correcta también para los valores extremos.
            assertThat(rules.blocksNeeded(1)).isEqualTo(1);
        }

        @Test
        @DisplayName("Una duración de cero o negativa es un error de programación")
        void duracionInvalida() {
            assertThatThrownBy(() -> rules.blocksNeeded(0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> rules.blocksNeeded(-10))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("La regla funciona con otros tamaños de bloque")
        void otroTamanoDeBloque() {
            ScheduleRules reglasDe15 = rulesWith(15);
            assertThat(reglasDe15.blocksNeeded(45)).isEqualTo(3);
            assertThat(reglasDe15.blocksNeeded(40)).isEqualTo(3);   // 45 redondeado
        }
    }

    @Nested
    @DisplayName("Límites de la reserva")
    class Limites {

        @Test
        @DisplayName("No se puede reservar antes de la anticipación mínima")
        void anticipacionMinima() {
            AppProperties properties = new AppProperties();
            properties.setMinLeadMinutes(120);
            properties.setTimezone(ZONE.getId());

            ScheduleRules rules = new ScheduleRules(properties);
            Clock fijo = Clock.fixed(
                    Instant.parse("2026-10-19T12:00:00Z"), ZONE);

            var limite = rules.earliestBookableFrom(fijo);

            // 12:00 UTC son las 09:00 de Buenos Aires. Con 2 horas de
            // anticipación, el límite queda en las 11:00.
            assertThat(limite.getHour()).isEqualTo(11);
        }

        @Test
        @DisplayName("El horizonte máximo se cuenta en días, no en horas")
        void horizonteMaximo() {
            AppProperties properties = new AppProperties();
            properties.setMaxAdvanceDays(60);
            properties.setTimezone(ZONE.getId());

            ScheduleRules rules = new ScheduleRules(properties);
            Clock fijo = Clock.fixed(Instant.parse("2026-10-19T12:00:00Z"), ZONE);

            LocalDate hoy = LocalDate.ofInstant(fijo.instant(), ZONE);
            assertThat(rules.lastBookableDate(fijo).toLocalDate())
                    .isEqualTo(hoy.plusDays(60));
        }
    }
}
