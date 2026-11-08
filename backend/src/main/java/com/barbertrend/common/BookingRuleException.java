package com.barbertrend.common;

import org.springframework.http.HttpStatus;

/**
 * La reserva no cumple una regla de la agenda.
 *
 * <p>Los datos están bien formados (por eso no es un 400), pero no tienen
 * sentido: una fecha en el pasado, un día cerrado, un horario que se pasó
 * del cierre, más lejos de lo que se puede reservar.
 *
 * <p>Se usa 422 en vez de 400 porque el frontend puede distinguir "escribiste
 * mal el formulario" de "ese horario no existe", y en el segundo caso
 * corresponde ofrecer otros horarios en vez de corregir el texto.
 */
public class BookingRuleException extends ApiException {

    // --- Reglas concretas, con su propio código ---

    public static BookingRuleException pastDate() {
        return new BookingRuleException("PAST_DATE",
                "Ese horario ya pasó. Elegí uno de los próximos.", "startsAt");
    }

    public static BookingRuleException tooSoon() {
        return new BookingRuleException("TOO_SOON",
                "Hay que reservar con más anticipación.", "startsAt");
    }

    public static BookingRuleException tooFarAhead() {
        return new BookingRuleException("TOO_FAR_AHEAD",
                "No se puede reservar con tanta anticipación.", "date");
    }

    public static BookingRuleException outsideSchedule() {
        return new BookingRuleException("OUTSIDE_SCHEDULE",
                "Ese horario está fuera del horario de atención.", "startsAt");
    }

    public static BookingRuleException doesNotFitBeforeClose() {
        return new BookingRuleException("DOES_NOT_FIT",
                "El servicio no termina antes del cierre.", "startsAt");
    }

    public static BookingRuleException blockedTime() {
        return new BookingRuleException("BLOCKED_TIME",
                "Ese horario está bloqueado por un cierre de la barbería.",
                "startsAt");
    }

    public static BookingRuleException barberNotAvailable() {
        return new BookingRuleException("BARBER_NOT_AVAILABLE",
                "No hay un barbero disponible para tomar reservas.", null);
    }

    private BookingRuleException(String code, String message, String field) {
        super(code, HttpStatus.UNPROCESSABLE_ENTITY, message, field);
    }
}
