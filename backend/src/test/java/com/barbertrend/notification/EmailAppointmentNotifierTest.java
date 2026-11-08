package com.barbertrend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.barbertrend.config.AppProperties;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Pruebas del aviso por email al barbero.
 *
 * <p>Se usa un {@link JavaMailSender} falso: no se manda ningún correo real.
 * Lo que se verifica es la lógica que decide si enviar y qué contenido
 * llevarían los mensajes.
 */
class EmailAppointmentNotifierTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final AppProperties properties = new AppProperties();

    private AppointmentNotification notificacion() {
        return new AppointmentNotification(
                "BT-TEST01",
                "Juan Pérez",
                "5491122334455",
                "Corte clásico",
                LocalDateTime.of(2026, 10, 24, 9, 0),
                LocalDateTime.of(2026, 10, 24, 9, 30),
                "corto los lados");
    }

    private EmailAppointmentNotifier notifier() {
        return new EmailAppointmentNotifier(mailSender, properties, "cuenta@gmail.com");
    }

    @Test
    @DisplayName("En dry-run no se envía ningún correo")
    void enDryRunNoEnvia() {
        properties.getNotifications().setEmailEnabled(false);

        notifier().notifyNewAppointment(notificacion());

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Con el email activo se envía al barbero, con el turno adentro")
    void conEmailActivoEnvia() {
        properties.getNotifications().setEmailEnabled(true);
        properties.getNotifications().setBarberEmail("barbero@ejemplo.com");
        properties.getNotifications().setFrom("cuenta@gmail.com");

        notifier().notifyNewAppointment(notificacion());

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("barbero@ejemplo.com");
        assertThat(message.getFrom()).isEqualTo("cuenta@gmail.com");
        assertThat(message.getSubject()).contains("Juan Pérez");
        assertThat(message.getText())
                .contains("BT-TEST01")
                .contains("5491122334455")
                .contains("Corte clásico")
                .contains("/admin");
    }

    @Test
    @DisplayName("Activo pero sin destinatario: no se envía y no explota")
    void activoPeroSinDestinatario() {
        properties.getNotifications().setEmailEnabled(true);
        properties.getNotifications().setBarberEmail("");

        notifier().notifyNewAppointment(notificacion());

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
