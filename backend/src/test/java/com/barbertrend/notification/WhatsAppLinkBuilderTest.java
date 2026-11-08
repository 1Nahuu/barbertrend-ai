package com.barbertrend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de la normalización del teléfono y del enlace de WhatsApp.
 *
 * <p>Es la parte que el barbero usa todos los días: si el número queda mal,
 * el mensaje no sale. Por eso se prueban los formatos argentinos más
 * habituales sin necesidad de levantar Spring.
 */
class WhatsAppLinkBuilderTest {

    @Test
    @DisplayName("Número local de 10 dígitos: se le agrega 54 9")
    void numeroLocalDeDiezDigitos() {
        assertThat(WhatsAppLinkBuilder.normalize("2284 239845"))
                .isEqualTo("5492284239845");
    }

    @Test
    @DisplayName("Formato con 0 y 15: se quitan y se agrega 54 9")
    void formatoConCeroYQuince() {
        assertThat(WhatsAppLinkBuilder.normalize("02284 15 239845"))
                .isEqualTo("5492284239845");
    }

    @Test
    @DisplayName("Área de 2 dígitos con 15")
    void areaDeDosDigitosConQuince() {
        assertThat(WhatsAppLinkBuilder.normalize("011 15 2233 4455"))
                .isEqualTo("5491122334455");
    }

    @Test
    @DisplayName("Número ya internacional: se deja igual")
    void numeroInternacional() {
        assertThat(WhatsAppLinkBuilder.normalize("+54 9 2284 239845"))
                .isEqualTo("5492284239845");
        assertThat(WhatsAppLinkBuilder.normalize("5492284239845"))
                .isEqualTo("5492284239845");
    }

    @Test
    @DisplayName("Sin teléfono no se arma enlace")
    void sinTelefonoNoHayEnlace() {
        assertThat(WhatsAppLinkBuilder.build("", "Hola")).isNull();
        assertThat(WhatsAppLinkBuilder.build("   ", "Hola")).isNull();
    }

    @Test
    @DisplayName("El mensaje viaja codificado en la URL")
    void mensajeCodificado() {
        String url = WhatsAppLinkBuilder.build("2284239845",
                "Hola Juan, te confirmamos el turno.");

        assertThat(url).startsWith("https://wa.me/5492284239845?text=");
        assertThat(url).contains("Hola%20Juan");
        assertThat(url).doesNotContain(" ");
    }
}
