package com.barbertrend.notification;

/**
 * Evento que se publica cuando se crea un turno.
 *
 * <p>Lleva un {@link AppointmentNotification} (datos copiados) y no la
 * entidad. Se publica dentro de la transacción que guarda el turno, pero el
 * listener lo procesa <b>después del commit</b> (ver
 * {@link AppointmentNotificationListener}). Así, si la reserva falla, no se
 * avisa de un turno que no existe.
 */
public record AppointmentCreatedEvent(AppointmentNotification notification) {
}
