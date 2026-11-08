package com.barbertrend.notification;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Arma enlaces {@code wa.me} para que el barbero abra una conversación de
 * WhatsApp ya escrita y solo tenga que tocar "enviar".
 *
 * <h2>Por qué un enlace y no la API</h2>
 *
 * <p>La API oficial de WhatsApp Business exige una cuenta de Meta, un
 * número dedicado y plantillas aprobadas, y los mensajes iniciados por el
 * negocio pueden tener costo. Para el primer uso, un enlace {@code wa.me}
 * es <b>gratis</b>, no depende de terceros y sirve desde el primer día.
 * Cuando el proyecto justifique la API oficial, esta clase se reemplaza por
 * un notificador real sin tocar el resto del panel.
 *
 * <h2>Normalización del número</h2>
 *
 * <p>{@code wa.me} necesita el número internacional (para Argentina:
 * {@code 54 9} + código de área + número). El cliente suele escribir el
 * formato local, así que acá se convierten los casos habituales:
 *
 * <pre>
 *   2284 239845        -> 5492284239845
 *   02284 15 239845    -> 5492284239845
 *   11 2233 4455       -> 5491122334455
 *   011 15 2233 4455   -> 5491122334455
 *   +54 9 2284 239845  -> 5492284239845
 * </pre>
 *
 * <p>Si el número no tiene un formato reconocible, se devuelve tal cual: el
 * barbero verá si WhatsApp lo acepta. No se inventan dígitos.
 */
public final class WhatsAppLinkBuilder {

    private WhatsAppLinkBuilder() {
        // Clase de utilidades.
    }

    /**
     * Construye el enlace, o {@code null} si no hay número.
     *
     * @param rawPhone teléfono tal como lo escribió el cliente (con o sin
     *                 signos)
     * @param message  texto que se va a pre-cargar en WhatsApp
     */
    public static String build(String rawPhone, String message) {
        String number = normalize(rawPhone);
        if (number.isBlank() || message == null || message.isBlank()) {
            return null;
        }
        return "https://wa.me/" + number + "?text=" + encode(message);
    }

    /**
     * Convierte un teléfono argentino al formato internacional de WhatsApp.
     *
     * <p>Público para poder probarlo sin levantar todo Spring.
     */
    public static String normalize(String rawPhone) {
        if (rawPhone == null) {
            return "";
        }

        String digits = rawPhone.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return "";
        }

        // Ya viene con código de país.
        if (digits.startsWith("54")) {
            String rest = digits.substring(2);
            if (rest.startsWith("0")) {
                rest = rest.substring(1);
            }
            if (!rest.startsWith("9")) {
                rest = "9" + rest;
            }
            return "54" + rest;
        }

        // Formato local con 0 inicial.
        if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }

        // Quita el "15" que se usa solo dentro del país, después del área.
        // El área argentina puede tener 2, 3 o 4 dígitos.
        for (int area = 2; area <= 4; area++) {
            if (digits.length() > area + 2 && digits.startsWith("15", area)) {
                String candidate = digits.substring(0, area) + digits.substring(area + 2);
                if (candidate.length() == 10) {
                    digits = candidate;
                    break;
                }
            }
        }

        if (digits.length() == 10) {
            return "549" + digits;
        }
        if (digits.length() == 11 && digits.startsWith("9")) {
            return "54" + digits;
        }

        return digits;
    }

    private static String encode(String message) {
        // URLEncoder usa '+' para los espacios; en un texto de WhatsApp
        // conviene el %20 para que no se vean signos raros.
        return URLEncoder.encode(message, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
