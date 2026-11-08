package com.barbertrend.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reglas de la agenda, leídas del archivo de configuración.
 *
 * <p>Los valores vienen de variables de entorno (ver .env.example), así que
 * se pueden cambiar sin tocar el código.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** Zona horaria de la barbería. Todos los horarios se calculan en ella. */
    private String timezone = "America/Argentina/Buenos_Aires";

    /** Moneda en la que se muestran los precios. */
    private String currency = "ARS";

    /** Duración de cada bloque de la grilla de horarios, en minutos. */
    private int slotMinutes = 30;

    /** Con cuánta anticipación mínima se puede reservar. */
    private int minLeadMinutes = 120;

    /** Con cuántos días de anticipación máxima se puede reservar. */
    private int maxAdvanceDays = 60;

    /** URL pública del sitio, para armar enlaces en los mensajes. */
    private String publicBaseUrl = "http://localhost:8080";

    /** Nombre comercial que aparece en los avisos. */
    private String businessName = "Doble S Barbería";

    /** Dirección que aparece en los mensajes de confirmación. */
    private String address = "Bolívar 3008";

    /** Nombre del usuario del administrador. */
    private String adminUsername = "admin";

    /** Avisos al barbero (email; WhatsApp se arma a mano en el panel). */
    private Notifications notifications = new Notifications();

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public int getSlotMinutes() {
        return slotMinutes;
    }

    public void setSlotMinutes(int slotMinutes) {
        this.slotMinutes = slotMinutes;
    }

    public int getMinLeadMinutes() {
        return minLeadMinutes;
    }

    public void setMinLeadMinutes(int minLeadMinutes) {
        this.minLeadMinutes = minLeadMinutes;
    }

    public int getMaxAdvanceDays() {
        return maxAdvanceDays;
    }

    public void setMaxAdvanceDays(int maxAdvanceDays) {
        this.maxAdvanceDays = maxAdvanceDays;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Notifications getNotifications() {
        return notifications;
    }

    public void setNotifications(Notifications notifications) {
        this.notifications = notifications;
    }

    /**
     * Configuración de los avisos.
     *
     * <p>El envío de email se activa solo si {@code email-enabled} es true.
     * Si no, la aplicación funciona igual pero deja el aviso en el log
     * (modo dry-run), que es lo que se usa mientras no haya credenciales.
     */
    public static class Notifications {

        private boolean emailEnabled = false;

        /** Casilla del barbero que recibe el aviso de cada turno nuevo. */
        private String barberEmail = "";

        /**
         * WhatsApp del barbero. Todavía no se usa para enviar: en esta
         * etapa el aviso al cliente se manda a mano desde el panel con un
         * enlace wa.me. Queda guardado para la etapa automática.
         */
        private String barberWhatsapp = "";

        /** Remitente de los emails. Si está vacío se usa el usuario SMTP. */
        private String from = "";

        public boolean isEmailEnabled() {
            return emailEnabled;
        }

        public void setEmailEnabled(boolean emailEnabled) {
            this.emailEnabled = emailEnabled;
        }

        public String getBarberEmail() {
            return barberEmail;
        }

        public void setBarberEmail(String barberEmail) {
            this.barberEmail = barberEmail;
        }

        public String getBarberWhatsapp() {
            return barberWhatsapp;
        }

        public void setBarberWhatsapp(String barberWhatsapp) {
            this.barberWhatsapp = barberWhatsapp;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }
    }

    public ZoneId getZoneId() {
        return ZoneId.of(timezone);
    }
}
