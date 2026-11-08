package com.barbertrend.common;

import org.springframework.http.HttpStatus;

/**
 * Un cierre de agenda no se puede crear o no existe.
 *
 * <p>Son errores de negocio del panel: el barbero envió datos bien formados,
 * pero el cierre choca con turnos existentes, con otro cierre o con una regla
 * de la agenda. Se responden con un código estable para que el JavaScript
 * pueda mostrar un mensaje claro.
 */
public class ScheduleBlockException extends ApiException {

    public static ScheduleBlockException overlap() {
        return new ScheduleBlockException("BLOCK_OVERLAP", HttpStatus.CONFLICT,
                "Ya hay un cierre que se superpone con ese horario.", "startDate");
    }

    public static ScheduleBlockException hasAppointments(long count) {
        String turnos = count == 1 ? "turno" : "turnos";
        return new ScheduleBlockException("BLOCK_HAS_APPOINTMENTS", HttpStatus.CONFLICT,
                "Hay " + count + " " + turnos + " en ese horario. "
                        + "Cancelalos antes de cerrar la agenda.", "startDate");
    }

    public static ScheduleBlockException invalidRange(String message, String field) {
        return new ScheduleBlockException("INVALID_BLOCK", HttpStatus.UNPROCESSABLE_ENTITY,
                message, field);
    }

    public static ScheduleBlockException past() {
        return new ScheduleBlockException("BLOCK_IN_PAST", HttpStatus.UNPROCESSABLE_ENTITY,
                "Ese horario ya pasó. Elegí una fecha o una hora futura.", "startDate");
    }

    private ScheduleBlockException(String code, HttpStatus status, String message, String field) {
        super(code, status, message, field);
    }
}
