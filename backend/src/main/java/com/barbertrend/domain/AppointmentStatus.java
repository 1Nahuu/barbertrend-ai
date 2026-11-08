package com.barbertrend.domain;

/**
 * Estados posibles de un turno.
 *
 * <p>El recorrido normal es:
 *
 * <pre>
 *   PENDING ──confirmar──► CONFIRMED ──atender──► COMPLETED
 *      │                     │
 *      └── cancelar ─────────┴──► CANCELLED
 * </pre>
 *
 * <p><b>PENDING</b>: el cliente reserve y el dueño todavía no lo confirmó.
 * Ocupa el horario igual que CONFIRMED, porque si no, dos personas podrían
 * pedir las 10:00 y quedar las dos en la lista de pendientes.
 *
 * <p><b>CANCELLED</b>: libera el horario (se borran sus filas de
 * {@code appointment_slot}), pero el turno no se elimina de la base: queda
 * el registro de qué pasó.
 */
public enum AppointmentStatus {

    /** Reserva recibida, esperando que el dueño la confirme. */
    PENDING,

    /** Confirmada por el dueño. */
    CONFIRMED,

    /** El servicio se prestó. */
    COMPLETED,

    /** Cancelada, con motivo. */
    CANCELLED;

    /**
     * Indica si un turno en este estado ocupa el horario en la agenda.
     *
     * <p>Un turno cancelado libera los bloques.uno completado ya es
     * pasado de fecha, así que tampoco ocupa.
     */
    public boolean occupiesSlot() {
        return this == PENDING || this == CONFIRMED || this == COMPLETED;
    }

    /** Indica si desde este estado se puede pasar a otro. */
    public boolean canTransitionTo(AppointmentStatus target) {
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == COMPLETED || target == CANCELLED;
            case COMPLETED -> false;
            case CANCELLED -> false;
        };
    }
}
