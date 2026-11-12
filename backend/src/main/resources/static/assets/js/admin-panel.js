/* ==========================================================================
 * admin-panel.js — Interfaz privada del barbero
 * ==========================================================================
 *
 *  Hace cinco cosas:
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
 *    4. Administra el catálogo de servicios con /api/admin/services. Los
 *       servicios no se borran: se desactivan para no romper el historial de
 *       turnos.
 *
 *    5. Edita el horario semanal con /api/admin/business-hours. El sitio
 *       público carga ese horario desde la API, así que el cambio se ve sin
 *       tocar archivos.
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
    // Servicios
    // ==========================================================================

    var servicesStatus = document.getElementById('services-status');
    var servicesList = document.getElementById('services-list');
    var serviceForm = document.getElementById('service-form');
    var serviceFormTitle = document.getElementById('service-form-title');
    var serviceNewButton = document.getElementById('service-new');
    var serviceCancelButton = document.getElementById('service-cancel');
    var serviceIdField = document.getElementById('service-id');
    var serviceName = document.getElementById('service-name');
    var serviceDescription = document.getElementById('service-description');
    var servicePrice = document.getElementById('service-price');
    var serviceDuration = document.getElementById('service-duration');
    var serviceSort = document.getElementById('service-sort');
    var serviceActive = document.getElementById('service-active');
    var serviceError = document.getElementById('service-error');
    var serviceSubmit = document.getElementById('service-submit');
    var serviceCurrency = document.getElementById('service-currency');

    var services = [];
    var servicesBusy = false;

    function showServicesStatus(type, title, message) {
        renderStatus(servicesStatus, type, title, message);
    }

    function clearServicesStatus() {
        if (servicesStatus) servicesStatus.innerHTML = '';
    }

    function setServiceError(message) {
        if (serviceError) serviceError.textContent = message || '';
    }

    function formatPrice(value, currency) {
        var amount = Number(value);

        if (!isFinite(amount)) return '—';

        try {
            return new Intl.NumberFormat('es-AR', {
                style: 'currency',
                currency: currency || 'ARS',
                minimumFractionDigits: 0,
                maximumFractionDigits: 2
            }).format(amount);
        } catch (error) {
            return '$ ' + amount;
        }
    }

    function renderServices(items) {
        if (!servicesList) return;

        services = items || [];
        servicesList.innerHTML = '';

        if (services.length === 0) {
            servicesList.appendChild(el('p', 'panel-empty',
                'No hay servicios cargados. Creá el primero con el botón de arriba.'));
            return;
        }

        if (serviceCurrency && services[0].currency) {
            serviceCurrency.textContent = services[0].currency;
        }

        services.forEach(function (service) {
            var item = el('article', 'service-item');
            item.dataset.id = String(service.id);

            var main = el('div', 'service-main');

            var head = el('div', 'service-head');
            head.appendChild(el('h3', 'service-name', service.name));

            var badge = el('span', 'status-badge '
                    + (service.active ? 'is-active' : 'is-inactive'),
                service.active ? 'Activo' : 'Inactivo');
            head.appendChild(badge);
            main.appendChild(head);

            main.appendChild(el('p', 'service-meta',
                formatPrice(service.price, service.currency)
                + ' · ' + service.durationMinutes + ' min'
                + ' · Orden ' + service.sortOrder));

            if (service.description) {
                main.appendChild(el('p', 'service-description',
                    service.description));
            }

            item.appendChild(main);

            var actions = el('div', 'service-actions');

            var editButton = el('button', 'btn btn-outline btn-sm', 'Editar');
            editButton.type = 'button';
            editButton.dataset.action = 'edit-service';
            editButton.dataset.id = String(service.id);
            actions.appendChild(editButton);

            var toggleButton = el('button', 'btn btn-outline btn-sm',
                service.active ? 'Desactivar' : 'Activar');
            toggleButton.type = 'button';
            toggleButton.dataset.action = 'toggle-service';
            toggleButton.dataset.id = String(service.id);
            actions.appendChild(toggleButton);

            item.appendChild(actions);
            servicesList.appendChild(item);
        });
    }

    async function loadServices() {
        if (servicesBusy || !servicesList) return;

        servicesBusy = true;
        showServicesStatus('info', 'Cargando servicios…', 'Un segundo.');

        try {
            var items = await apiFetch('/api/admin/services');
            clearServicesStatus();
            renderServices(items);
        } catch (error) {
            showServicesStatus('error', 'No pudimos cargar los servicios.',
                error.message);
        } finally {
            servicesBusy = false;
        }
    }

    function openServiceForm(service) {
        if (!serviceForm) return;

        serviceForm.hidden = false;
        setServiceError('');

        if (service) {
            if (serviceFormTitle) serviceFormTitle.textContent = 'Editar servicio';
            if (serviceSubmit) serviceSubmit.textContent = 'Guardar cambios';
            if (serviceIdField) serviceIdField.value = String(service.id);
            if (serviceName) serviceName.value = service.name;
            if (serviceDescription) serviceDescription.value = service.description || '';
            if (servicePrice) servicePrice.value = service.price;
            if (serviceDuration) serviceDuration.value = service.durationMinutes;
            if (serviceSort) serviceSort.value = service.sortOrder;
            if (serviceActive) serviceActive.checked = service.active;
        } else {
            serviceForm.reset();
            if (serviceFormTitle) serviceFormTitle.textContent = 'Nuevo servicio';
            if (serviceSubmit) serviceSubmit.textContent = 'Crear servicio';
            if (serviceIdField) serviceIdField.value = '';
            if (serviceActive) serviceActive.checked = true;
            if (serviceDuration) serviceDuration.value = 30;
            if (serviceSort) serviceSort.value = 0;
        }

        serviceForm.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        if (serviceName) serviceName.focus();
    }

    function closeServiceForm() {
        if (!serviceForm) return;

        serviceForm.hidden = true;
        serviceForm.reset();
        setServiceError('');
    }

    async function saveService(event) {
        event.preventDefault();

        setServiceError('');

        var id = serviceIdField && serviceIdField.value
            ? Number(serviceIdField.value)
            : null;
        var name = serviceName ? serviceName.value.trim() : '';
        var description = serviceDescription ? serviceDescription.value.trim() : '';
        var price = servicePrice ? Number(servicePrice.value) : NaN;
        var duration = serviceDuration ? Number(serviceDuration.value) : NaN;
        var sort = serviceSort && serviceSort.value !== ''
            ? Number(serviceSort.value)
            : 0;
        var active = !!(serviceActive && serviceActive.checked);

        if (!name) {
            setServiceError('Escribí el nombre del servicio.');
            if (serviceName) serviceName.focus();
            return;
        }

        if (!isFinite(price) || price < 0) {
            setServiceError('El precio tiene que ser un número igual o mayor a cero.');
            if (servicePrice) servicePrice.focus();
            return;
        }

        if (!isFinite(duration) || duration < 1 || Math.floor(duration) !== duration) {
            setServiceError('La duración tiene que ser un número entero mayor a cero.');
            if (serviceDuration) serviceDuration.focus();
            return;
        }

        if (!isFinite(sort) || sort < 0 || Math.floor(sort) !== sort) {
            setServiceError('El orden tiene que ser un número entero igual o mayor a cero.');
            if (serviceSort) serviceSort.focus();
            return;
        }

        var payload = {
            name: name,
            description: description || null,
            price: price,
            durationMinutes: duration,
            active: active,
            sortOrder: sort
        };

        if (serviceSubmit) serviceSubmit.disabled = true;
        showServicesStatus('info', 'Guardando el servicio…', 'Un segundo.');

        try {
            await apiFetch(
                id ? '/api/admin/services/' + id : '/api/admin/services',
                {
                    method: id ? 'PUT' : 'POST',
                    body: JSON.stringify(payload)
                }
            );

            closeServiceForm();
            clearServicesStatus();
            await loadServices();
            showServicesStatus('success',
                id ? 'Servicio actualizado.' : 'Servicio creado.',
                'El catálogo público ya muestra los cambios.');
        } catch (error) {
            showServicesStatus('error', 'No pudimos guardar el servicio.',
                error.message);
            setServiceError(error.message);
        } finally {
            if (serviceSubmit) serviceSubmit.disabled = false;
        }
    }

    async function toggleService(service) {
        var activate = !service.active;

        try {
            await apiFetch('/api/admin/services/' + service.id
                    + '/active?active=' + activate, { method: 'PATCH' });

            clearServicesStatus();
            await loadServices();
            showServicesStatus('success',
                activate ? 'Servicio activado.' : 'Servicio desactivado.',
                activate
                    ? 'Ya se puede reservar de nuevo.'
                    : 'Dejó de ofrecerse en la web, pero sus turnos se conservan.');
        } catch (error) {
            showServicesStatus('error', 'No pudimos cambiar el servicio.',
                error.message);
        }
    }

    function findService(id) {
        for (var i = 0; i < services.length; i++) {
            if (services[i].id === id) return services[i];
        }
        return null;
    }

    function onServicesClick(event) {
        var button = event.target.closest('button[data-action]');
        if (!button) return;

        var id = Number(button.dataset.id);
        var service = findService(id);
        if (!service) return;

        if (button.dataset.action === 'edit-service') {
            openServiceForm(service);
            return;
        }

        if (button.dataset.action === 'toggle-service') {
            button.disabled = true;
            toggleService(service).finally(function () {
                if (button.isConnected) button.disabled = false;
            });
        }
    }

    function initServices() {
        if (!servicesList) return;

        servicesList.addEventListener('click', onServicesClick);

        if (serviceNewButton) {
            serviceNewButton.addEventListener('click', function () {
                openServiceForm(null);
            });
        }

        if (serviceCancelButton) {
            serviceCancelButton.addEventListener('click', closeServiceForm);
        }

        if (serviceForm) {
            serviceForm.addEventListener('submit', saveService);
        }

        loadServices();
    }

    // ==========================================================================
    // Horario de atención
    // ==========================================================================

    var hoursStatus = document.getElementById('hours-status');
    var hoursList = document.getElementById('hours-list');

    function showHoursStatus(type, title, message) {
        renderStatus(hoursStatus, type, title, message);
    }

    function clearHoursStatus() {
        if (hoursStatus) hoursStatus.innerHTML = '';
    }

    function capitalize(text) {
        return text ? text.charAt(0).toUpperCase() + text.slice(1) : '';
    }

    function hoursValue(value) {
        return value ? value.substring(0, 5) : '';
    }

    function syncHoursRow(form) {
        if (!form) return;

        var closedInput = form.querySelector('input[data-role="closed"]');
        var openInput = form.querySelector('input[data-role="open"]');
        var closeInput = form.querySelector('input[data-role="close"]');
        var closed = !!(closedInput && closedInput.checked);

        if (openInput) openInput.disabled = closed;
        if (closeInput) closeInput.disabled = closed;

        // Al reabrir un día que estaba cerrado no hay horas guardadas.
        // Se proponen las mismas que usa la barbería de lunes a sábado.
        if (!closed && openInput && !openInput.value) openInput.value = '09:00';
        if (!closed && closeInput && !closeInput.value) closeInput.value = '21:00';
    }

    function renderHours(days) {
        if (!hoursList) return;

        hoursList.innerHTML = '';

        if (!days || days.length === 0) {
            hoursList.appendChild(el('p', 'panel-empty',
                'No hay horario cargado. Revisá la migración de la base.'));
            return;
        }

        days.forEach(function (day) {
            var form = el('form', 'hours-item');
            form.dataset.day = String(day.dayOfWeek);
            form.setAttribute('novalidate', '');

            var openId = 'hours-open-' + day.dayOfWeek;
            var closeId = 'hours-close-' + day.dayOfWeek;
            var closedId = 'hours-closed-' + day.dayOfWeek;

            form.appendChild(el('div', 'hours-day', capitalize(day.dayName)));

            var closedLabel = el('label', 'checkbox-label');
            var closedInput = document.createElement('input');
            closedInput.type = 'checkbox';
            closedInput.id = closedId;
            closedInput.checked = !!day.closed;
            closedInput.dataset.role = 'closed';
            closedLabel.appendChild(closedInput);
            closedLabel.appendChild(document.createTextNode(' Cerrado'));
            form.appendChild(closedLabel);

            var openField = el('div', 'field');
            var openLabel = el('label', 'label', 'Apertura');
            openLabel.setAttribute('for', openId);
            var openInput = document.createElement('input');
            openInput.type = 'time';
            openInput.className = 'input';
            openInput.id = openId;
            openInput.step = '1800';
            openInput.value = hoursValue(day.openTime);
            openInput.dataset.role = 'open';
            openField.appendChild(openLabel);
            openField.appendChild(openInput);
            form.appendChild(openField);

            var closeField = el('div', 'field');
            var closeLabel = el('label', 'label', 'Cierre');
            closeLabel.setAttribute('for', closeId);
            var closeInput = document.createElement('input');
            closeInput.type = 'time';
            closeInput.className = 'input';
            closeInput.id = closeId;
            closeInput.step = '1800';
            closeInput.value = hoursValue(day.closeTime);
            closeInput.dataset.role = 'close';
            closeField.appendChild(closeLabel);
            closeField.appendChild(closeInput);
            form.appendChild(closeField);

            var actions = el('div', 'hours-actions');
            var saveButton = el('button', 'btn btn-primary btn-sm', 'Guardar');
            saveButton.type = 'submit';
            actions.appendChild(saveButton);
            form.appendChild(actions);

            syncHoursRow(form);
            hoursList.appendChild(form);
        });
    }

    async function loadHours() {
        if (!hoursList) return;

        showHoursStatus('info', 'Cargando horario…', 'Un segundo.');

        try {
            var data = await apiFetch('/api/admin/business-hours');
            clearHoursStatus();
            renderHours(data ? data.days : null);
        } catch (error) {
            showHoursStatus('error', 'No pudimos cargar el horario.',
                error.message);
        }
    }

    async function saveHours(event) {
        event.preventDefault();

        var form = event.target.closest('.hours-item');
        if (!form) return;

        var day = Number(form.dataset.day);
        var closedInput = form.querySelector('input[data-role="closed"]');
        var openInput = form.querySelector('input[data-role="open"]');
        var closeInput = form.querySelector('input[data-role="close"]');
        var closed = !!(closedInput && closedInput.checked);
        var open = openInput ? openInput.value : '';
        var close = closeInput ? closeInput.value : '';

        if (!closed) {
            if (!open || !close) {
                showHoursStatus('error', 'Faltan las horas.',
                    'Indicá la apertura y el cierre, o marcá el día como cerrado.');
                return;
            }

            if (close <= open) {
                showHoursStatus('error', 'El horario no es válido.',
                    'El cierre tiene que ser posterior a la apertura.');
                return;
            }
        }

        var payload = {
            openTime: closed ? null : open,
            closeTime: closed ? null : close,
            closed: closed
        };

        var button = form.querySelector('button[type="submit"]');
        if (button) button.disabled = true;

        showHoursStatus('info', 'Guardando el horario…', 'Un segundo.');

        try {
            await apiFetch('/api/admin/business-hours/' + day, {
                method: 'PUT',
                body: JSON.stringify(payload)
            });

            clearHoursStatus();
            await loadHours();
            showHoursStatus('success', 'Horario guardado.',
                'La web pública ya muestra el cambio.');
        } catch (error) {
            showHoursStatus('error', 'No pudimos guardar el horario.',
                error.message);
        } finally {
            if (button && button.isConnected) button.disabled = false;
        }
    }

    function initHours() {
        if (!hoursList) return;

        hoursList.addEventListener('change', function (event) {
            var closedInput = event.target.closest('input[data-role="closed"]');
            if (!closedInput) return;

            syncHoursRow(closedInput.closest('.hours-item'));
        });

        hoursList.addEventListener('submit', saveHours);
        loadHours();
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
        initServices();
        initHours();
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
