/* ==========================================================================
 * booking.js — Formulario de reserva de turnos
 * ==========================================================================
 *
 * Estado actual: ETAPA 3 (reservas reales).
 *
 * El formulario arma la grilla consultando la disponibilidad real del
 * backend (GET /api/public/availability) y crea el turno
 * (POST /api/public/appointments). El backend es el que decide si un
 * horario está libre; el navegador solo muestra lo que le devuelve.
 *
 * Depende de las utilidades de main.js (window.BT).
 * ========================================================================== */

'use strict';

(function () {

    const $ = function (selector) {
        return document.querySelector(selector);
    };

    // ==========================================================================
    // Estado del formulario
    //
    // Se mantiene todo en un solo objeto en vez de leer los valores del DOM
    // cada vez: así hay una única fuente de verdad y no quedan casos donde
    // la pantalla diga una cosa y el estado otra.
    // ==========================================================================

    const state = {
        serviceId: null,
        date: null,
        time: null
    };

    let refs = {};

    // ==========================================================================
    // Utilidades
    // ==========================================================================

    function el(tag, className, textContent) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (textContent !== undefined && textContent !== null) {
            node.textContent = textContent;
        }
        return node;
    }

    /** Busca un servicio por id dentro de la configuración */
    function getService(serviceId) {
        return SITE_CONFIG.services.find(function (service) {
            return service.id === serviceId;
        }) || null;
    }

    /**
     * Redondea la duración hacia arriba al múltiplo del bloque más cercano.
     *
     * Ejemplo con bloques de 30 minutos:
     *   30 min -> 30      (1 bloque)
     *   45 min -> 60      (2 bloques)
     *   60 min -> 60      (2 bloques)
     *
     * Es la misma regla que aplica el backend (docs/arquitectura.md, §5.1).
     */
    function blocksNeeded(durationMinutes) {
        const slot = SITE_CONFIG.booking.slotMinutes;
        return Math.ceil(durationMinutes / slot);
    }

    /** Convierte 'HH:MM' en minutos desde medianoche */
    function timeToMinutes(time) {
        const parts = time.split(':');
        return Number(parts[0]) * 60 + Number(parts[1]);
    }

    /** Convierte minutos desde medianoche en 'HH:MM' */
    function minutesToTime(minutes) {
        const hour = String(Math.floor(minutes / 60)).padStart(2, '0');
        const minute = String(minutes % 60).padStart(2, '0');
        return hour + ':' + minute;
    }

    /** Pide texto al usuario y lo convierte a minúscula sin acentos */
    function normalizeText(value) {
        return value.trim().toLowerCase();
    }

    // ==========================================================================
    // Mostrar y borrar mensajes
    // ==========================================================================

    /**
     * Muestra un aviso arriba del botón de enviar.
     * El contenedor tiene role="status" y aria-live, así que los lectores
     * de pantalla anuncian el cambio sin necesidad de nada más.
     */
    function showStatus(type, title, message) {
        const container = refs.status;
        if (!container) return;

        container.innerHTML = '';

        const alert = el('div', 'alert alert-' + type);
        alert.appendChild(el('span', 'alert-icon', type === 'error' ? '!' : 'i'));

        const body = el('div');
        body.appendChild(el('strong', null, title));
        body.appendChild(document.createTextNode(message));

        alert.appendChild(body);
        container.appendChild(alert);
    }

    function clearStatus() {
        if (refs.status) refs.status.innerHTML = '';
    }

    /** Pone o saca el mensaje de error de un campo */
    function setFieldError(fieldId, errorId, message) {
        const input = document.getElementById(fieldId);
        const errorNode = document.getElementById(errorId);

        // aria-invalid solo tiene sentido en un control de formulario.
        // El selector de servicio es un <div> que contiene radios, y la
        // grilla de horarios es un <p>: ahí no se toca el atributo.
        if (input && input.matches('input, select, textarea')) {
            if (message) {
                input.setAttribute('aria-invalid', 'true');
            } else {
                input.removeAttribute('aria-invalid');
            }
        }

        if (errorNode) {
            errorNode.textContent = message || '';
        }
    }

    // ==========================================================================
    // Fecha:input y días rápidos
    // ==========================================================================

    function initDateField() {
        const input = refs.fecha;
        if (!input) return;

        const today = BT.todayISO();
        const maxDate = BT.addDaysISO(today, SITE_CONFIG.booking.maxAdvanceDays);

        // El navegador no deja elegir antes de hoy ni después del horizonte.
        // Los atributos min/max son la primera barrera; la validación de
        // JavaScript viene después, porque los atributos se pueden saltar.
        input.min = today;
        input.max = maxDate;

        input.addEventListener('change', function () {
            state.date = input.value || null;
            state.time = null;
            clearFieldErrors();
            renderQuickDates();
            renderSlots();
            updateSummary();
        });
    }

    /** Muestra los próximos días como botones rápidos */
    function renderQuickDates() {
        const container = refs.quickDates;
        if (!container) return;

        container.innerHTML = '';

        const today = BT.todayISO();
        const daysToShow = 7;

        for (let offset = 0; offset < daysToShow; offset++) {
            const iso = BT.addDaysISO(today, offset);
            const hours = BT.getHoursForDay(iso);
            const isClosed = !hours || hours.closed;

            const button = el('button', 'quick-date');
            button.type = 'button';

            const date = BT.parseISODate(iso);
            const dayName = date
                .toLocaleDateString('es-AR', { weekday: 'short' })
                .replace('.', '');

            button.appendChild(el('span', 'quick-date__day', offset === 0 ? 'Hoy' : dayName));
            button.appendChild(el('span', 'quick-date__num', String(date.getDate())));

            if (state.date === iso) {
                button.classList.add('is-selected');
                button.setAttribute('aria-pressed', 'true');
            } else {
                button.setAttribute('aria-pressed', 'false');
            }

            if (isClosed) {
                // Cerrado: se muestra, pero sin poder seleccionarlo.
                // Ocultarlo haría creer que el día no existe.
                button.classList.add('is-closed');
                button.disabled = true;
                button.title = 'Cerrado';
            } else {
                button.addEventListener('click', function () {
                    state.date = iso;
                    state.time = null;
                    refs.fecha.value = iso;
                    renderQuickDates();
                    renderSlots();
                    updateSummary();
                    setFieldError('fecha', 'fecha-error', '');
                });
            }

            container.appendChild(button);
        }
    }

    // ==========================================================================
    // Horarios
    // ==========================================================================

    /**
     * Arma la grilla de horarios para el servicio y la fecha elegidos.
     *
     * Los horarios los decide el backend (GET /api/public/availability):
     * ahí se aplican el horario de atención, la anticipación mínima y,
     * sobre todo, los turnos que ya están ocupados. Armar la grilla en el
     * navegador con datos fijos mostraría como libres horarios que no lo
     * están.
     */
    let slotsRequestId = 0;

    async function renderSlots() {
        const grid = refs.slotsGrid;
        const placeholder = refs.slotsPlaceholder;
        if (!grid) return;

        // Cada llamada invalida la anterior: si el usuario cambia de fecha
        // rápido, la respuesta vieja no pisa la nueva.
        const requestId = ++slotsRequestId;

        grid.innerHTML = '';
        grid.hidden = false;

        const service = state.serviceId ? getService(state.serviceId) : null;

        // --- Sin servicio todavía ---
        if (!service) {
            placeholder.textContent = 'Elegí un servicio y una fecha para ver ' +
                                      'los horarios disponibles.';
            placeholder.classList.remove('is-info');
            placeholder.hidden = false;
            return;
        }

        // --- Sin fecha todavía ---
        if (!state.date) {
            placeholder.textContent = 'Ahora elegí el día para ver los horarios.';
            placeholder.classList.remove('is-info');
            placeholder.hidden = false;
            return;
        }

        placeholder.textContent = 'Buscando horarios libres…';
        placeholder.classList.add('is-info');
        placeholder.hidden = false;
        grid.hidden = true;

        try {
            const response = await fetch(
                '/api/public/availability?serviceId=' +
                encodeURIComponent(service.id) +
                '&date=' + encodeURIComponent(state.date)
            );

            if (requestId !== slotsRequestId) return;

            if (!response.ok) {
                throw new Error('No pudimos consultar la disponibilidad.');
            }

            const data = await response.json();
            if (requestId !== slotsRequestId) return;

            // --- Día cerrado ---
            if (data.closed) {
                placeholder.textContent = 'Ese día estamos cerrados. Probá con otro día.';
                placeholder.classList.add('is-info');
                placeholder.hidden = false;
                return;
            }

            const slots = (data.slots || []).map(function (slot) {
                return slot.startsAt.substring(11, 16);
            });

            // --- Sin horarios libres ---
            if (slots.length === 0) {
                placeholder.textContent = 'No quedan horarios libres para ese día. ' +
                                          'Probá con otra fecha.';
                placeholder.classList.add('is-info');
                placeholder.hidden = false;
                return;
            }

            // Si el horario elegido dejó de estar libre, se deselecciona.
            if (state.time && slots.indexOf(state.time) === -1) {
                state.time = null;
                updateSummary();
            }

            const slot = SITE_CONFIG.booking.slotMinutes;
            const needed = blocksNeeded(service.duration);

            slots.forEach(function (timeText) {
                const endText = minutesToTime(timeToMinutes(timeText) + needed * slot);

                const button = el('button', 'slot-btn', timeText);
                button.type = 'button';
                button.dataset.time = timeText;
                button.dataset.end = endText;

                if (state.time === timeText) {
                    button.classList.add('is-selected');
                    button.setAttribute('aria-checked', 'true');
                }

                button.addEventListener('click', function () {
                    selectTime(timeText);
                });

                grid.appendChild(button);
            });

            placeholder.hidden = true;
            grid.hidden = false;
        } catch (error) {
            if (requestId !== slotsRequestId) return;

            grid.innerHTML = '';
            placeholder.textContent = 'No pudimos cargar los horarios. ' +
                                      'Revisá tu conexión y probá de nuevo.';
            placeholder.classList.remove('is-info');
            placeholder.hidden = false;
        }
    }

    function selectTime(timeText) {
        state.time = timeText;

        // Marca visualmente el botón elegido
        refs.slotsGrid.querySelectorAll('.slot-btn').forEach(function (button) {
            const selected = button.dataset.time === timeText;
            button.classList.toggle('is-selected', selected);
            button.setAttribute('aria-checked', String(selected));
        });

        setFieldError('slots-error', 'slots-error', '');
        updateSummary();
    }

    // ==========================================================================
    // Resumen
    // ==========================================================================

    function updateSummary() {
        const service = state.serviceId ? getService(state.serviceId) : null;

        refs.summaryServicio.textContent = service ? service.name : '—';

        refs.summaryPrecio.textContent = service
            ? BT.formatPrice(service.price)
            : '—';

        refs.summaryFecha.textContent = state.date
            ? BT.formatLongDate(state.date)
            : '—';

        refs.summaryHorario.textContent = state.time
            ? state.time + ' hs'
            : '—';
    }

    // ==========================================================================
    // Validación
    // ==========================================================================

    function clearFieldErrors() {
        setFieldError('fecha', 'fecha-error', '');
        setFieldError('nombre', 'nombre-error', '');
        setFieldError('telefono', 'telefono-error', '');
        setFieldError('notas', 'notas-error', '');
        setFieldError('service-picker', 'service-error', '');
        setFieldError('slots-grid', 'slots-error', '');
    }

    /**
     * Valida todos los campos. Devuelve un array con los problemas
     * encontrados, o vacío si todo está bien.
     *
     * La validación del backend es la que manda: esto es solo para dar
     * respuestas rápidas y claras sin volver al servidor.
     *
     * Cada error trae dos datos: dónde está el campo (para poder enfocarlo)
     * y dónde va el mensaje.
     */
    function validate() {
        const errors = [];

        // --- Servicio ---
        if (!state.serviceId) {
            errors.push({
                field: 'service-picker',
                errorId: 'service-error',
                message: 'Elegí un servicio.'
            });
        }

        // --- Fecha ---
        if (!state.date) {
            errors.push({
                field: 'fecha',
                errorId: 'fecha-error',
                message: 'Elegí una fecha.'
            });
        } else {
            const today = BT.todayISO();
            const maxDate = BT.addDaysISO(today, SITE_CONFIG.booking.maxAdvanceDays);

            if (state.date < today) {
                errors.push({
                    field: 'fecha',
                    errorId: 'fecha-error',
                    message: 'Esa fecha ya pasó.'
                });
            } else if (state.date > maxDate) {
                errors.push({
                    field: 'fecha',
                    errorId: 'fecha-error',
                    message: 'Se puede reservar hasta ' +
                             BT.formatLongDate(maxDate) + '.'
                });
            }
        }

        // --- Horario ---
        if (!state.time) {
            errors.push({
                field: 'slots-grid',
                errorId: 'slots-error',
                message: 'Elegí un horario.'
            });
        }

        // --- Nombre ---
        const name = refs.nombre.value.trim();
        if (name.length < 2) {
            errors.push({
                field: 'nombre',
                errorId: 'nombre-error',
                message: 'Escribí tu nombre.'
            });
        } else if (name.length > 120) {
            errors.push({
                field: 'nombre',
                errorId: 'nombre-error',
                message: 'El nombre es demasiado largo.'
            });
        }

        // --- Teléfono ---
        const phoneRaw = refs.telefono.value.trim();
        const phoneDigits = phoneRaw.replace(/\D/g, '');

        if (!phoneDigits) {
            errors.push({
                field: 'telefono',
                errorId: 'telefono-error',
                message: 'Escribí tu teléfono.'
            });
        } else if (phoneDigits.length < 7 || phoneDigits.length > 15) {
            errors.push({
                field: 'telefono',
                errorId: 'telefono-error',
                message: 'Revisá el teléfono: debe tener entre 7 y 15 dígitos.'
            });
        }

        // --- Notas (opcionales, pero con un tope) ---
        if (refs.notas.value.length > 500) {
            errors.push({
                field: 'notas',
                errorId: 'notas-error',
                message: 'Las notas son demasiado largas.'
            });
        }

        // Se pintan los errores en los campos
        clearFieldErrors();
        errors.forEach(function (error) {
            setFieldError(error.field, error.errorId, error.message);
        });

        return errors;
    }

    /** Lleva el foco al primer campo con problema */
    function focusFirstError(errors) {
        if (!errors.length) return;

        const first = errors[0].field;

        // El selector de servicio y la grilla de horarios no son inputs:
        // el foco va al primer control de cada uno.
        if (first === 'service-picker') {
            const input = refs.servicePicker.querySelector('input');
            if (input) input.focus();
            return;
        }

        if (first === 'slots-grid') {
            const button = refs.slotsGrid.querySelector('.slot-btn:not(:disabled)');
            if (button) button.focus();
            return;
        }

        const node = document.getElementById(first);
        if (node) node.focus();
    }

    // ==========================================================================
    // Envío
    // ==========================================================================

    /**
     * Llama a la API real de reservas.
     *
     * Contrato ya definido en docs/api.md:
     *   POST /api/public/appointments
     *   {
     *     serviceId: number,
     *     startsAt: "2026-10-20T09:00:00",
     *     customerName: string,
     *     phone: string,        (solo dígitos, con código de país)
     *     notes: string
     *   }
     *
     * Respuesta 201:
     *   { reference, status, serviceName, startsAt, endsAt, message }
     *
     * Errores a manejar:
     *   409 SLOT_NOT_AVAILABLE -> "Ese horario acaba de ser reservado"
     *   422 OUTSIDE_SCHEDULE   -> "Fuera del horario de atención"
     *   422 TOO_SOON / TOO_FAR_AHEAD / PAST_DATE
     *   400 VALIDATION_ERROR
     */
    async function sendBookingRequest(payload) {
        const response = await fetch('/api/public/appointments', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const body = await response.json().catch(function () {
            return null;
        });

        if (!response.ok) {
            const error = new Error(
                (body && body.message) || 'No pudimos registrar la reserva.'
            );
            error.code = (body && body.code) || 'UNKNOWN';
            throw error;
        }

        return body;
    }

    /** Activa o desactiva el botón de enviar */
    function setSubmitting(isSubmitting) {
        refs.submitBtn.disabled = isSubmitting;
        refs.submitBtn.classList.toggle('is-loading', isSubmitting);
    }

    function handleSubmit(event) {
        event.preventDefault();

        clearStatus();
        const errors = validate();

        if (errors.length > 0) {
            showStatus(
                'error',
                'Faltan datos',
                'Revisá los campos marcados en rojo antes de reservar.'
            );
            focusFirstError(errors);
            return;
        }

        const payload = {
            serviceId: state.serviceId,
            startsAt: state.date + 'T' + state.time + ':00',
            customerName: refs.nombre.value.trim(),
            phone: refs.telefono.value.replace(/\D/g, ''),
            notes: refs.notas.value.trim()
        };

        // --- Reservas apagadas por mantenimiento ---
        if (!SITE_CONFIG.flags.bookingApiEnabled) {
            showStatus(
                'info',
                'Reservas momentáneamente deshabilitadas',
                'El sistema de turnos está en mantenimiento. ' +
                'Probá de nuevo en un rato o escribinos por WhatsApp.'
            );
            return;
        }

        // --- Envío real ---
        setSubmitting(true);

        sendBookingRequest(payload)
            .then(function (result) {
                const reference = result && result.reference
                    ? ' Guardá el código ' + result.reference +
                      ' para consultar tu turno.'
                    : '';

                showStatus(
                    'success',
                    '¡Reserva enviada!',
                    ((result && result.message) ||
                    'Te confirmamos el turno a la brevedad.') + reference
                );
                resetForm();
            })
            .catch(function (error) {
                // El 409 es el caso normal: alguien más tomó el horario
                // mientras el cliente completaba el formulario.
                if (error.code === 'SLOT_NOT_AVAILABLE') {
                    showStatus(
                        'error',
                        'Ese horario ya no está disponible',
                        'Otra persona lo tomó mientras completabas el formulario. ' +
                        'Elegí otro horario.'
                    );

                    // Se recarga la grilla para mostrar la disponibilidad real
                    state.time = null;
                    renderSlots();
                    updateSummary();
                } else {
                    showStatus(
                        'error',
                        'No pudimos registrar la reserva',
                        error.message
                    );
                }
            })
            .finally(function () {
                setSubmitting(false);
            });
    }

    /** Limpia el formulario después de una reserva exitosa */
    function resetForm() {
        state.serviceId = null;
        state.date = null;
        state.time = null;

        refs.form.reset();

        const firstRadio = refs.servicePicker.querySelector('input');
        if (firstRadio) firstRadio.checked = false;

        renderQuickDates();
        renderSlots();
        updateSummary();
        clearFieldErrors();
    }

    // ==========================================================================
    // Arranque
    // ==========================================================================

    function init() {
        refs = {
            form: $('#booking-form'),
            servicePicker: $('#service-picker'),
            fecha: $('#fecha'),
            quickDates: $('#quick-dates'),
            slotsGrid: $('#slots-grid'),
            slotsPlaceholder: $('#slots-placeholder'),
            nombre: $('#nombre'),
            telefono: $('#telefono'),
            notas: $('#notas'),
            status: $('#form-status'),
            submitBtn: $('#submit-btn'),

            summaryServicio: $('#summary-servicio'),
            summaryPrecio: $('#summary-precio'),
            summaryFecha: $('#summary-fecha'),
            summaryHorario: $('#summary-horario')
        };

        // Si el HTML cambió y falta algo, se avisa en consola en vez de
        // romper en silencio.
        const required = ['form', 'servicePicker', 'fecha', 'slotsGrid', 'submitBtn'];
        const missing = required.filter(function (key) {
            return !refs[key];
        });

        if (missing.length > 0) {
            console.error('booking.js: faltan elementos en el HTML:', missing);
            return;
        }

        initDateField();
        renderQuickDates();
        renderSlots();
        updateSummary();

        refs.form.addEventListener('submit', handleSubmit);

        // Cuando main.js cambia el servicio elegido (por ejemplo, desde el
        // botón "Reservar" de una tarjeta), se recalcula todo.
        window.addEventListener('bt:service-selected', function (event) {
            state.serviceId = event.detail.serviceId;
            state.time = null;
            renderSlots();
            updateSummary();
        });
    }

    init();

})();
