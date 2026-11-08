/* ==========================================================================
 * admin-panel.js — Interfaz privada del barbero
 * ==========================================================================
 *
 *  Hace dos cosas:
 *
 *    1. Prepara el cierre de sesión: copia el token CSRF de la cookie al
 *       formulario "Salir". El POST lo procesa Spring Security en
 *       /admin/logout.
 *
 *    2. Carga los turnos de /api/admin/appointments y permite confirmarlos,
 *       completarlos o cancelarlos. Para avisarle al cliente por WhatsApp,
 *       pide el detalle del turno (que trae el enlace wa.me ya armado) y lo
 *       abre en una pestaña nueva. El envío del mensaje es MANUAL: el
 *       barbero toca "enviar" en WhatsApp. Es la opción gratis y sin
 *       depender de la API de Meta.
 *
 *    3. Permite cerrar días completos o franjas horarias con
 *       /api/admin/blocks. Los cierres ocultan horarios en la web pública y
 *       el backend también los valida al reservar.
 *
 *  Todas las llamadas de escritura mandan el header X-XSRF-TOKEN, que es
 *  lo que espera el CookieCsrfTokenRepository de Spring Security.
 * ========================================================================== */

'use strict';

(function () {

    var TOKEN_COOKIE = 'XSRF-TOKEN';

    var STATUS_LABELS = {
        PENDING: 'Pendiente',
        CONFIRMED: 'Confirmado',
        COMPLETED: 'Completado',
        CANCELLED: 'Cancelado'
    };

    // ==========================================================================
    // Utilidades
    // ==========================================================================

    function el(tag, className, textContent) {
        var node = document.createElement(tag);
        if (className) node.className = className;
        if (textContent !== undefined && textContent !== null) {
            node.textContent = textContent;
        }
        return node;
    }

    function getCookie(name) {
        var prefix = name + '=';
        var parts = document.cookie ? document.cookie.split('; ') : [];

        for (var i = 0; i < parts.length; i++) {
            if (parts[i].indexOf(prefix) === 0) {
                return decodeURIComponent(parts[i].substring(prefix.length));
            }
        }

        return '';
    }

    /**
     * Pide datos a la API privada.
     * Manda la cookie de sesión y, en las escrituras, el token CSRF.
     */
    async function apiFetch(url, options) {
        var opts = Object.assign(
            { credentials: 'same-origin', headers: {} },
            options || {}
        );

        var method = (opts.method || 'GET').toUpperCase();

        if (method !== 'GET' && method !== 'HEAD') {
            var token = getCookie(TOKEN_COOKIE);
            if (token) {
                opts.headers['X-XSRF-TOKEN'] = token;
            }
            if (opts.body && !opts.headers['Content-Type']) {
                opts.headers['Content-Type'] = 'application/json';
            }
        }

        var response = await fetch(url, opts);

        // La sesión se venció: se vuelve al login.
        if (response.status === 401) {
            window.location.href = '/admin/login';
            throw new Error('Sesión vencida.');
        }

        var data = await response.json().catch(function () {
            return null;
        });

        if (!response.ok) {
            var message = (data && data.message) || 'No pudimos completar la operación.';
            var error = new Error(message);
            error.code = data && data.code;
            throw error;
        }

        return data;
    }

    /** Convierte "2026-10-24T09:00:00" en un Date local. */
    function parseLocalDateTime(value) {
        var parts = value.split('T');
        var date = parts[0].split('-');
        var time = (parts[1] || '00:00:00').split(':');
        return new Date(
            Number(date[0]),
            Number(date[1]) - 1,
            Number(date[2]),
            Number(time[0]),
            Number(time[1])
        );
    }

    function formatWhen(value) {
        var date = parseLocalDateTime(value);

        var day = date.toLocaleDateString('es-AR', {
            weekday: 'short',
            day: '2-digit',
            month: '2-digit'
        });

        var time = date.toLocaleTimeString('es-AR', {
            hour: '2-digit',
            minute: '2-digit'
        });

        return day + ' · ' + time + ' hs';
    }

    function todayISO() {
        var now = new Date();
        var month = String(now.getMonth() + 1).padStart(2, '0');
        var day = String(now.getDate()).padStart(2, '0');
        return now.getFullYear() + '-' + month + '-' + day;
    }

    function parseISODate(value) {
        var parts = value.split('-');
        return new Date(
            Number(parts[0]),
            Number(parts[1]) - 1,
            Number(parts[2])
        );
    }

    function formatISODate(value) {
        return parseISODate(value).toLocaleDateString('es-AR', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric'
        });
    }

    function addDaysISO(value, days) {
        var date = parseISODate(value);
        date.setDate(date.getDate() + days);
        var month = String(date.getMonth() + 1).padStart(2, '0');
        var day = String(date.getDate()).padStart(2, '0');
        return date.getFullYear() + '-' + month + '-' + day;
    }

    /** Texto legible de un cierre: "Todo el día" o "13:00 a 15:00". */
    function formatBlockWhen(block) {
        var startsAt = block.startsAt;
        var endsAt = block.endsAt;
        var startDate = startsAt.substring(0, 10);

        if (block.allDay) {
            // El final es exclusivo: un cierre del 12/11 termina el 13/11
            // a las 00:00, así que se muestra hasta el día anterior.
            var lastDate = addDaysISO(endsAt.substring(0, 10), -1);
            var range = startDate === lastDate
                ? formatISODate(startDate)
                : formatISODate(startDate) + ' al ' + formatISODate(lastDate);
            return 'Todo el día · ' + range;
        }

        return formatISODate(startDate)
            + ' · ' + startsAt.substring(11, 16)
            + ' a ' + endsAt.substring(11, 16) + ' hs';
    }

    // ==========================================================================
    // Mensajes
    // ==========================================================================

    var statusContainer = document.getElementById('appointments-status');

    function renderStatus(container, type, title, message) {
        if (!container) return;

        container.innerHTML = '';

        var alert = el('div', 'alert alert-' + type);
        alert.appendChild(el('span', 'alert-icon', type === 'error' ? '!' : 'i'));

        var body = el('div');
        body.appendChild(el('strong', null, title));
        body.appendChild(document.createTextNode(' ' + message));
        alert.appendChild(body);

        container.appendChild(alert);
    }

    function showStatus(type, title, message) {
        renderStatus(statusContainer, type, title, message);
    }

    function clearStatus() {
        if (statusContainer) statusContainer.innerHTML = '';
    }

    // ==========================================================================
    // Turnos
    // ==========================================================================

    var list = document.getElementById('appointments-list');
    var filter = document.getElementById('status-filter');
    var refreshButton = document.getElementById('refresh-appointments');
    var busy = false;

    function actionButton(label, action, id, extraClass) {
        var button = el('button', 'btn btn-sm ' + (extraClass || 'btn-outline'), label);
        button.type = 'button';
        button.dataset.action = action;
        button.dataset.id = String(id);
        return button;
    }

    function renderAppointment(appointment) {
        var card = el('article', 'appointment');
        card.dataset.id = String(appointment.id);
        card.dataset.status = appointment.status;

        // --- Columna principal ---
        var main = el('div', 'appointment-main');

        var head = el('div', 'appointment-head');
        head.appendChild(el('span', 'appointment-when',
            formatWhen(appointment.startsAt)));
        head.appendChild(el('span', 'status-badge is-' + appointment.status.toLowerCase(),
            STATUS_LABELS[appointment.status] || appointment.status));
        main.appendChild(head);

        main.appendChild(el('h3', 'appointment-name',
            appointment.customerName || '(sin nombre)'));

        var meta = el('p', 'appointment-meta');
        meta.textContent = appointment.serviceName +
            ' · Código ' + appointment.reference +
            ' · Tel. ' + (appointment.customerPhoneMasked || '—');
        main.appendChild(meta);

        if (appointment.notes) {
            main.appendChild(el('p', 'appointment-notes',
                'Notas: ' + appointment.notes));
        }

        card.appendChild(main);

        // --- Acciones ---
        var side = el('div', 'appointment-side');

        var actions = el('div', 'appointment-actions');

        if (appointment.status === 'PENDING') {
            actions.appendChild(actionButton('Confirmar', 'confirm', appointment.id,
                'btn-primary'));
        }

        if (appointment.status === 'PENDING'
                || appointment.status === 'CONFIRMED'
                || appointment.status === 'COMPLETED') {
            actions.appendChild(actionButton('WhatsApp', 'whatsapp', appointment.id,
                'btn-whatsapp'));
        }

        if (appointment.status === 'CONFIRMED') {
            actions.appendChild(actionButton('Completar', 'complete', appointment.id));
        }

        if (appointment.status === 'PENDING'
                || appointment.status === 'CONFIRMED') {
            actions.appendChild(actionButton('Cancelar', 'cancel', appointment.id,
                'btn-danger'));
        }

        side.appendChild(actions);
        card.appendChild(side);

        return card;
    }

    function renderAppointments(appointments) {
        if (!list) return;

        list.innerHTML = '';

        if (!appointments || appointments.length === 0) {
            list.appendChild(el('p', 'appointments-empty',
                'No hay turnos para mostrar con este filtro.'));
            return;
        }

        appointments.forEach(function (appointment) {
            list.appendChild(renderAppointment(appointment));
        });
    }

    async function loadAppointments() {
        if (busy || !list) return;

        busy = true;
        if (refreshButton) refreshButton.disabled = true;

        showStatus('info', 'Cargando turnos…', 'Un segundo.');

        try {
            var query = '?size=100';
            if (filter && filter.value) {
                query += '&status=' + encodeURIComponent(filter.value);
            }

            var appointments = await apiFetch('/api/admin/appointments' + query);
            clearStatus();
            renderAppointments(appointments);
        } catch (error) {
            showStatus('error', 'No pudimos cargar los turnos.', error.message);
        } finally {
            busy = false;
            if (refreshButton) refreshButton.disabled = false;
        }
    }

    async function confirmAppointment(id) {
        await apiFetch('/api/admin/appointments/' + id + '/confirm', {
            method: 'PATCH'
        });
        showStatus('success', 'Turno confirmado.',
            'Ya podés usar el botón WhatsApp para avisarle al cliente.');
        await loadAppointments();
    }

    async function completeAppointment(id) {
        await apiFetch('/api/admin/appointments/' + id + '/complete', {
            method: 'PATCH'
        });
        showStatus('success', 'Turno completado.', 'El horario quedó libre.');
        await loadAppointments();
    }

    async function openWhatsApp(id) {
        // Se abre la pestaña AHORA, dentro del clic, porque los navegadores
        // bloquean window.open() después de un await. La URL se completa
        // cuando llega la respuesta del servidor.
        var waWindow = window.open('', '_blank', 'noopener');

        try {
            var detail = await apiFetch('/api/admin/appointments/' + id);

            if (!detail.clientWhatsAppUrl) {
                if (waWindow) waWindow.close();
                showStatus('info', 'Sin enlace de WhatsApp.',
                    'Revisá el teléfono del cliente.');
                return;
            }

            if (waWindow) {
                waWindow.location.href = detail.clientWhatsAppUrl;
            } else {
                // Bloqueador de ventanas emergentes: se navega en la actual.
                window.location.href = detail.clientWhatsAppUrl;
            }
        } catch (error) {
            if (waWindow) waWindow.close();
            throw error;
        }
    }

    // ==========================================================================
    // Cancelación (con motivo obligatorio)
    // ==========================================================================

    var cancelDialog = document.getElementById('cancel-dialog');
    var cancelForm = document.getElementById('cancel-form');
    var cancelSummary = document.getElementById('cancel-dialog-summary');
    var cancelReason = document.getElementById('cancel-reason');
    var cancelError = document.getElementById('cancel-reason-error');
    var cancelCloseButton = document.getElementById('cancel-dialog-close');
    var cancelAppointmentId = null;

    function openCancelDialog(id, name) {
        if (!cancelDialog) return;

        cancelAppointmentId = id;
        if (cancelSummary) {
            cancelSummary.textContent = 'Turno de ' + (name || 'el cliente') + '.';
        }
        if (cancelReason) {
            cancelReason.value = '';
            cancelReason.removeAttribute('aria-invalid');
        }
        if (cancelError) cancelError.textContent = '';

        cancelDialog.showModal();
        if (cancelReason) cancelReason.focus();
    }

    function closeCancelDialog() {
        cancelAppointmentId = null;
        if (cancelDialog) cancelDialog.close();
    }

    function initCancelDialog() {
        if (!cancelDialog || !cancelForm) return;

        if (cancelCloseButton) {
            cancelCloseButton.addEventListener('click', closeCancelDialog);
        }

        // Clic afuera del formulario: se cierra.
        cancelDialog.addEventListener('click', function (event) {
            if (event.target === cancelDialog) closeCancelDialog();
        });

        cancelForm.addEventListener('submit', async function (event) {
            event.preventDefault();

            var reason = cancelReason ? cancelReason.value.trim() : '';

            if (!reason) {
                if (cancelError) {
                    cancelError.textContent = 'Escribí el motivo de la cancelación.';
                }
                if (cancelReason) {
                    cancelReason.setAttribute('aria-invalid', 'true');
                    cancelReason.focus();
                }
                return;
            }

            var id = cancelAppointmentId;
            closeCancelDialog();

            try {
                await apiFetch('/api/admin/appointments/' + id + '/cancel', {
                    method: 'POST',
                    body: JSON.stringify({ reason: reason })
                });
                showStatus('success', 'Turno cancelado.',
                    'El horario volvió a quedar libre.');
                await loadAppointments();
            } catch (error) {
                showStatus('error', 'No pudimos cancelar el turno.', error.message);
            }
        });
    }

    // ==========================================================================
    // Cierres y bloqueos
    // ==========================================================================

    var blocksStatus = document.getElementById('blocks-status');
    var blocksList = document.getElementById('blocks-list');
    var blockForm = document.getElementById('block-form');
    var blockAllDay = document.getElementById('block-all-day');
    var blockStartDate = document.getElementById('block-start-date');
    var blockEndDate = document.getElementById('block-end-date');
    var blockEndDateField = document.getElementById('block-end-date-field');
    var blockTimes = document.getElementById('block-times');
    var blockStartTime = document.getElementById('block-start-time');
    var blockEndTime = document.getElementById('block-end-time');
    var blockReason = document.getElementById('block-reason');
    var blockError = document.getElementById('block-error');
    var blockSubmit = document.getElementById('block-submit');
    var blocksBusy = false;

    function setBlockError(message) {
        if (blockError) blockError.textContent = message || '';
    }

    function showBlockStatus(type, title, message) {
        renderStatus(blocksStatus, type, title, message);
    }

    function clearBlockStatus() {
        if (blocksStatus) blocksStatus.innerHTML = '';
    }

    function renderBlocks(blocks) {
        if (!blocksList) return;

        blocksList.innerHTML = '';

        if (!blocks || blocks.length === 0) {
            blocksList.appendChild(el('p', 'appointments-empty',
                'No hay cierres programados.'));
            return;
        }

        blocks.forEach(function (block) {
            var item = el('article', 'block-item');
            item.dataset.id = String(block.id);

            var main = el('div', 'block-main');
            main.appendChild(el('p', 'block-when', formatBlockWhen(block)));

            if (block.reason) {
                main.appendChild(el('p', 'block-reason',
                    'Motivo: ' + block.reason));
            }

            item.appendChild(main);

            var actions = el('div', 'block-actions');
            var button = el('button', 'btn btn-outline btn-sm', 'Quitar');
            button.type = 'button';
            button.dataset.action = 'delete-block';
            button.dataset.id = String(block.id);
            actions.appendChild(button);
            item.appendChild(actions);

            blocksList.appendChild(item);
        });
    }

    async function loadBlocks() {
        if (blocksBusy || !blocksList) return;

        blocksBusy = true;
        if (blockSubmit) blockSubmit.disabled = true;

        showBlockStatus('info', 'Cargando cierres…', 'Un segundo.');

        try {
            var blocks = await apiFetch('/api/admin/blocks');
            clearBlockStatus();
            renderBlocks(blocks);
        } catch (error) {
            showBlockStatus('error', 'No pudimos cargar los cierres.', error.message);
        } finally {
            blocksBusy = false;
            if (blockSubmit) blockSubmit.disabled = false;
        }
    }

    function toggleBlockFields() {
        var allDay = !!(blockAllDay && blockAllDay.checked);

        if (blockEndDateField) blockEndDateField.hidden = !allDay;
        if (blockTimes) blockTimes.hidden = allDay;

        if (blockEndDate) blockEndDate.disabled = !allDay;
        if (blockStartTime) blockStartTime.disabled = allDay;
        if (blockEndTime) blockEndTime.disabled = allDay;
    }

    function syncEndDateMin() {
        if (!blockStartDate || !blockEndDate) return;

        var start = blockStartDate.value || todayISO();
        blockEndDate.min = start;

        if (blockEndDate.value && blockEndDate.value < start) {
            blockEndDate.value = start;
        }
    }

    async function createBlock(event) {
        event.preventDefault();

        setBlockError('');

        var startDate = blockStartDate ? blockStartDate.value : '';
        var allDay = !!(blockAllDay && blockAllDay.checked);
        var endDate = allDay && blockEndDate ? blockEndDate.value : '';
        var startTime = allDay || !blockStartTime ? '' : blockStartTime.value;
        var endTime = allDay || !blockEndTime ? '' : blockEndTime.value;
        var reason = blockReason ? blockReason.value.trim() : '';

        if (!startDate) {
            setBlockError('Elegí una fecha de inicio.');
            if (blockStartDate) blockStartDate.focus();
            return;
        }

        if (allDay && endDate && endDate < startDate) {
            setBlockError('La fecha final no puede ser anterior a la inicial.');
            if (blockEndDate) blockEndDate.focus();
            return;
        }

        if (!allDay && (!startTime || !endTime)) {
            setBlockError('Indicá la hora de inicio y la hora de fin.');
            if (!startTime && blockStartTime) blockStartTime.focus();
            else if (blockEndTime) blockEndTime.focus();
            return;
        }

        if (!allDay && endTime <= startTime) {
            setBlockError('La hora final tiene que ser posterior a la inicial.');
            if (blockEndTime) blockEndTime.focus();
            return;
        }

        var payload = {
            startDate: startDate,
            endDate: allDay && endDate ? endDate : null,
            allDay: allDay,
            startTime: allDay ? null : startTime + ':00',
            endTime: allDay ? null : endTime + ':00',
            reason: reason || null
        };

        if (blockSubmit) blockSubmit.disabled = true;
        showBlockStatus('info', 'Guardando el cierre…', 'Un segundo.');

        try {
            await apiFetch('/api/admin/blocks', {
                method: 'POST',
                body: JSON.stringify(payload)
            });

            blockForm.reset();
            toggleBlockFields();
            syncEndDateMin();
            clearBlockStatus();
            await loadBlocks();
            showBlockStatus('success', 'Cierre creado.',
                'La agenda quedó bloqueada en ese horario.');
        } catch (error) {
            var title = error.code === 'BLOCK_HAS_APPOINTMENTS'
                ? 'Hay turnos en ese horario.'
                : 'No pudimos crear el cierre.';
            showBlockStatus('error', title, error.message);
        } finally {
            if (blockSubmit) blockSubmit.disabled = false;
        }
    }

    async function deleteBlock(id) {
        if (!window.confirm('¿Quitar este cierre? La agenda volverá a estar '
                + 'disponible en ese horario.')) {
            return;
        }

        try {
            await apiFetch('/api/admin/blocks/' + id, { method: 'DELETE' });
            clearBlockStatus();
            await loadBlocks();
            showBlockStatus('success', 'Cierre eliminado.',
                'La agenda volvió a quedar disponible.');
        } catch (error) {
            showBlockStatus('error', 'No pudimos eliminar el cierre.', error.message);
        }
    }

    function onBlocksClick(event) {
        var button = event.target.closest('button[data-action="delete-block"]');
        if (!button) return;

        var id = Number(button.dataset.id);
        button.disabled = true;

        deleteBlock(id).finally(function () {
            if (button.isConnected) button.disabled = false;
        });
    }

    function initBlocks() {
        if (!blockForm) return;

        var today = todayISO();

        if (blockStartDate) blockStartDate.min = today;
        if (blockEndDate) blockEndDate.min = today;

        toggleBlockFields();
        syncEndDateMin();

        if (blockAllDay) {
            blockAllDay.addEventListener('change', toggleBlockFields);
        }

        if (blockStartDate) {
            blockStartDate.addEventListener('change', syncEndDateMin);
        }

        if (blocksList) {
            blocksList.addEventListener('click', onBlocksClick);
        }

        blockForm.addEventListener('submit', createBlock);
        loadBlocks();
    }

    // ==========================================================================
    // Logout
    // ==========================================================================

    function initLogout() {
        var form = document.getElementById('logout-form');
        var csrfField = document.getElementById('logout-csrf');

        if (!form || !csrfField) return;

        var token = getCookie(TOKEN_COOKIE);

        if (token) {
            csrfField.value = token;
        } else {
            form.addEventListener('submit', function (event) {
                event.preventDefault();
                window.location.reload();
            });
        }
    }

    // ==========================================================================
    // Arranque
    // ==========================================================================

    function onListClick(event) {
        var button = event.target.closest('button[data-action]');
        if (!button) return;

        var id = Number(button.dataset.id);
        var action = button.dataset.action;

        button.disabled = true;

        var actionPromise;

        if (action === 'confirm') {
            actionPromise = confirmAppointment(id);
        } else if (action === 'complete') {
            actionPromise = completeAppointment(id);
        } else if (action === 'whatsapp') {
            actionPromise = openWhatsApp(id);
        } else if (action === 'cancel') {
            var nameNode = button.closest('.appointment')
                .querySelector('.appointment-name');
            openCancelDialog(id, nameNode ? nameNode.textContent : '');
            button.disabled = false;
            return;
        } else {
            button.disabled = false;
            return;
        }

        actionPromise
            .catch(function (error) {
                showStatus('error', 'No pudimos completar la acción.', error.message);
            })
            .finally(function () {
                button.disabled = false;
            });
    }

    function init() {
        initLogout();
        initCancelDialog();
        initBlocks();

        if (list) {
            list.addEventListener('click', onListClick);
        }

        if (refreshButton) {
            refreshButton.addEventListener('click', loadAppointments);
        }

        if (filter) {
            filter.addEventListener('change', loadAppointments);
        }

        if (list) {
            loadAppointments();
        }
    }

    init();

})();
