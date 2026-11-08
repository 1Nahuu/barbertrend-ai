package com.barbertrend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Datos que envía el cliente para reservar.
 *
 * <p>Las anotaciones de validación son la primera barrera. No reemplazan la
 * validación del servicio de negocio: estas verifican el FORMATO (que el
 * nombre no esté vacío, que el teléfono tenga dígitos), mientras que el
 * servicio verifica las REGLAS (que el horario esté libre, que la barbería
 * esté abierta).
 */
public record CreateAppointmentRequest(

        @NotNull(message = "Elegí un servicio.")
        Long serviceId,

        @NotNull(message = "Elegí un horario.")
        LocalDateTime startsAt,

        @NotBlank(message = "Escribí tu nombre.")
        @Size(min = 2, max = 120, message = "El nombre tiene que tener entre 2 y 120 letras.")
        String customerName,

        @NotBlank(message = "Escribí tu teléfono.")
        @Pattern(regexp = "\\d{7,15}", message = "Revisá el teléfono: debe tener entre 7 y 15 dígitos.")
        String phone,

        @Size(max = 500, message = "Las notas son demasiado largas.")
        String notes
) {

    /**
     * Normaliza el teléfono dejándolo solo con dígitos.
     *
     * <p>El cliente puede escribir "11 2233-4455", "+54 9 11 2233 4455" o
     * directamente los dígitos. Guardar siempre la misma forma permite
     * comparar sin sorpresas.
     */
    public String phoneDigits() {
        return phone == null ? null : phone.replaceAll("\\D", "");
    }
}
