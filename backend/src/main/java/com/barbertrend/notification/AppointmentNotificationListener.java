package com.barbertrend.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Escucha la creación de turnos y dispara el aviso al barbero.
 *
 * <p>Usa {@link TransactionalEventListener} en fase {@code AFTER_COMMIT}:
 * el aviso sale solo si la reserva se guardó de verdad. Si la transacción
 * se cae (por ejemplo, porque otro cliente tomó el horario), no se avisa
 * nada.
 *
 * <p>El listener envuelve todo en un {@code try/catch}: un problema de
 * correo no puede romper la respuesta al cliente, que ya tiene su turno
 * guardado.
 */
@Component
public class AppointmentNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(AppointmentNotificationListener.class);

    private final EmailAppointmentNotifier emailNotifier;

    public AppointmentNotificationListener(EmailAppointmentNotifier emailNotifier) {
        this.emailNotifier = emailNotifier;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAppointmentCreated(AppointmentCreatedEvent event) {
        try {
            emailNotifier.notifyNewAppointment(event.notification());
        } catch (RuntimeException ex) {
            log.error("Fallo inesperado al avisar del turno {}: {}",
                    event.notification().reference(), ex.getMessage(), ex);
        }
    }
}
