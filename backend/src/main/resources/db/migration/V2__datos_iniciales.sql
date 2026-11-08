-- ==========================================================================
-- V2 — Datos iniciales de Doble S Barbería
-- ==========================================================================
--
-- Estos datos NO son inventados: los confirmó el dueño.
--   - Nombre, logo, dirección, teléfonos e Instagram: salen del logo
--     que está en "barberia doble s/".
--   - Precios, horarios y servicios: los confirmó el dueño.
--
-- Las "duration_minutes" NO son reales. Un corte no dura siempre lo mismo:
-- depende del pelo y de lo que pida el cliente. El valor de acá es una
-- duración NOMINAL, que es lo que la agenda usa por defecto para saber
-- cuántos bloques de 30 minutos ocupa el turno. El dueño puede extender un
-- turno desde el panel cuando haga falta.
--
-- Esta migración corre una sola vez. Para cambiar algo después se escribe
-- una migración nueva (V3, V4, ...), nunca se edita una que ya corrió.
-- ==========================================================================


-- --------------------------------------------------------------------------
-- Barbero
-- --------------------------------------------------------------------------
-- El dueño todavía no confirmó el nombre del barbero, así que queda un
-- nombre de referencia que se cambia desde el panel.
-- --------------------------------------------------------------------------
INSERT INTO barber (name, active, sort_order)
VALUES ('Barbero', TRUE, 1);


-- --------------------------------------------------------------------------
-- Servicios — precios reales
-- --------------------------------------------------------------------------
INSERT INTO service (name, description, price, duration_minutes, active, sort_order) VALUES
    ('Corte clásico',
     'Corte a tijera y máquina, incluye lavado y peinado.',
     12000.00, 30, TRUE, 1),

    ('Barba completa',
     'Afilado con toalla, navaja y aceite.',
     12000.00, 30, TRUE, 2),

    ('Corte + barba',
     'Corte y barba completos.',
     15000.00, 60, TRUE, 3),

    ('Corte infantil',
     'Para los más chicos, con paciencia incluida.',
     9000.00, 30, TRUE, 4);


-- --------------------------------------------------------------------------
-- Horario de atención — real
--
-- 1 = lunes, 2 = martes, ... 7 = domingo.
-- --------------------------------------------------------------------------
INSERT INTO business_hours (day_of_week, open_time, close_time, is_closed) VALUES
    (1, '09:00:00', '19:00:00', FALSE),   -- Lunes
    (2, '09:00:00', '19:00:00', FALSE),   -- Martes
    (3, '09:00:00', '19:00:00', FALSE),   -- Miércoles
    (4, '09:00:00', '19:00:00', FALSE),   -- Jueves
    (5, '09:00:00', '20:00:00', FALSE),   -- Viernes
    (6, '09:00:00', '14:00:00', FALSE),   -- Sábado
    (7, NULL,          NULL,          TRUE);  -- Domingo: cerrado
