package com.barbertrend.common;

import org.springframework.http.HttpStatus;

/**
 * El horario de atención enviado desde el panel no es válido.
 *
 * <p>Son datos bien formados pero incoherentes: un día fuera de rango, un
 * cierre anterior a la apertura o un día abierto sin horas.
 */
public class BusinessHoursException extends ApiException {

    public static BusinessHoursException invalidDay() {
        return new BusinessHoursException(
                "El día tiene que estar entre 1 (lunes) y 7 (domingo).");
    }

    public static BusinessHoursException invalidRange() {
        return new BusinessHoursException(
                "Si el día no está cerrado, tiene que tener hora de apertura "
                        + "y de cierre, y el cierre tiene que ser posterior.");
    }

    public static BusinessHoursException invalidGrid(int slotMinutes) {
        return new BusinessHoursException(
                "La apertura y el cierre tienen que caer en bloques de "
                        + slotMinutes + " minutos.");
    }

    private BusinessHoursException(String message) {
        super("INVALID_BUSINESS_HOURS", HttpStatus.BAD_REQUEST, message, null);
    }
}
