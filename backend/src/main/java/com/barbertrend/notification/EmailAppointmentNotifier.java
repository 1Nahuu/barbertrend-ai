package com.barbertrend.notification;

import com.barbertrend.config.AppProperties;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Avisa al barbero por email cuando entra un turno nuevo.
 *
 * <h2>Modo dry-run</h2>
 *
 * <p>Si {@code app.notifications.email-enabled} es false (el valor por
 * defecto), este componente <b>no envía nada</b>: escribe en el log lo que
 * habría mandado. Así se puede probar todo el circuito sin credenciales y
 * sin riesgo de mandar correos reales por accidente.
 *
 * <p>Cuando se activa, usa el SMTP configurado (por ejemplo, Gmail con una
 * contraseña de aplicación). Un fallo de envío <b>no rompe la reserva</b>:
 * el turno ya quedó guardado cuando este aviso se dispara.
 */
@Component
public class EmailAppointmentNotifier {

    private static final Logger log = LoggerFactory.getLogger(EmailAppointmentNotifier.class);

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("es", "AR"));

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm 'hs'", new Locale("es", "AR"));

    private final JavaMailSender mailSender;
    private final AppProperties properties;
    private final String smtpUsername;

    public EmailAppointmentNotifier(JavaMailSender mailSender,
                                    AppProperties properties,
                                    @Value("${spring.mail.username:}") String smtpUsername) {
        this.mailSender = mailSender;
        this.properties = properties;
        this.smtpUsername = smtpUsername;
    }

    /**
     * Envía (o registra, en dry-run) el aviso de un turno nuevo.
     *
     * <p>No propaga excepciones: el que llama no tiene que fallar por un
     * problema de correo.
     */
    public void notifyNewAppointment(AppointmentNotification notification) {
        AppProperties.Notifications config = properties.getNotifications();

        if (!config.isEmailEnabled()) {
            log.info("[MAIL dry-run] Turno {} para {} el {} {} — servicio {}. "
                            + "Aviso a {} (activá MAIL_ENABLED=true para enviarlo).",
                    notification.reference(),
                    notification.maskedPhone(),
                    notification.startsAt().format(DATE),
                    notification.startsAt().format(TIME),
                    notification.serviceName(),
                    blankToDash(config.getBarberEmail()));
            return;
        }

        String recipient = trim(config.getBarberEmail());
        if (recipient.isEmpty()) {
            log.warn("MAIL_ENABLED está activo pero falta BARBER_EMAIL: "
                    + "no se pudo avisar del turno {}.", notification.reference());
            return;
        }

        String from = trim(config.getFrom());
        if (from.isEmpty()) {
            from = trim(smtpUsername);
        }
        if (from.isEmpty()) {
            log.warn("No hay MAIL_FROM ni usuario SMTP configurado: "
                    + "no se pudo avisar del turno {}.", notification.reference());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(recipient);
            message.setSubject(subject(notification));
            message.setText(body(notification));
            mailSender.send(message);

            log.info("Aviso por email enviado por el turno {}.",
                    notification.reference());
        } catch (MailException ex) {
            log.error("No se pudo enviar el aviso por email del turno {}: {}. "
                            + "El turno quedó guardado igual.",
                    notification.reference(), ex.getMessage());
        }
    }

    private String subject(AppointmentNotification notification) {
        return "Nuevo turno: " + notification.customerName()
                + " — " + notification.startsAt().format(DATE)
                + " " + notification.startsAt().format(TIME);
    }

    private String body(AppointmentNotification notification) {
        StringBuilder body = new StringBuilder();
        body.append("Nuevo turno en ").append(properties.getBusinessName()).append("\n\n");
        body.append("Cliente:  ").append(notification.customerName()).append('\n');
        body.append("Teléfono: ").append(notification.customerPhone()).append('\n');
        body.append("Servicio: ").append(notification.serviceName()).append('\n');
        body.append("Fecha:    ").append(notification.startsAt().format(DATE)).append('\n');
        body.append("Hora:     ").append(notification.startsAt().format(TIME)).append('\n');
        body.append("Notas:    ").append(blankToDash(notification.notes())).append('\n');
        body.append("Código:   ").append(notification.reference()).append("\n\n");
        body.append("Confirmá el turno desde el panel:\n");
        body.append(properties.getPublicBaseUrl()).append("/admin\n\n");
        body.append("Cuando lo confirmes, el panel te muestra el botón para "
                + "avisarle por WhatsApp al cliente.");
        return body.toString();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToDash(String value) {
        return value == null || value.isBlank() ? "—" : value.trim();
    }
}
