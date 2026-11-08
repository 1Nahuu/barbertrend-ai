-- --------------------------------------------------------------------------
-- Horario de atención actualizado
--
-- Lunes a sábado: 09:00 a 21:00.
-- Domingo: cerrado.
--
-- El cierre a las 21:00 es la hora en que baja la persiana: el último turno
-- empieza antes, según la duración del servicio (por ejemplo, 20:30 para un
-- corte de 30 minutos y 20:00 para corte + barba de 60 minutos).
--
-- No se edita V2 porque Flyway ya la aplicó en las bases existentes: los
-- cambios de datos se hacen con una migración nueva.
-- --------------------------------------------------------------------------

UPDATE business_hours
SET open_time = '09:00:00',
    close_time = '21:00:00',
    is_closed = FALSE
WHERE day_of_week BETWEEN 1 AND 6;

UPDATE business_hours
SET open_time = NULL,
    close_time = NULL,
    is_closed = TRUE
WHERE day_of_week = 7;
