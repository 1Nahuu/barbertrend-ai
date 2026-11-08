/* ==========================================================================
 * main.js — Armado del sitio público
 * ==========================================================================
 *
 * Qué hace este archivo:
 *   - Herramientas compartidas (formato de precio, fechas, etc.)
 *   - Menu hamburguesa en celular
 *   - Efecto del encabezado al hacer scroll
 *   - Dibuja servicios, galería, horarios y datos de contacto
 *   - Arma el selector de servicio del formulario de reservas
 *
 * Todos los datos salen de SITE_CONFIG (assets/js/site-config.js).
 * Para cambiar la barbería se edita ese archivo, no este.
 *
 * Se ejecuta cuando el DOM ya está listo, porque los scripts están al final
 * del <body>. Por eso se puede buscar cualquier elemento directamente.
 * ========================================================================== */

'use strict';

(function () {

    // ==========================================================================
    // Herramientas compartidas
    // También las usa booking.js: se exponen en window.BT para evitar
    // duplicar el código de formato.
    // ==========================================================================

    const BT = {};

    /** Formatea un precio: 12000 -> "$ 12.000" */
    BT.formatPrice = function (amount) {
        try {
            return new Intl.NumberFormat('es-AR', {
                style: 'currency',
                currency: SITE_CONFIG.currency || 'ARS',
                minimumFractionDigits: 0,
                maximumFractionDigits: 0
            }).format(amount);
        } catch (e) {
            // Si la moneda no está reconocida, se muestra el número solo
            return '$ ' + amount;
        }
    };

    /** Convierte 'YYYY-MM-DD' en un Date local (sin líos de zona horaria) */
    BT.parseISODate = function (isoString) {
        const parts = isoString.split('-');
        return new Date(
            Number(parts[0]),
            Number(parts[1]) - 1,
            Number(parts[2])
        );
    };

    /** Fecha de hoy en formato 'YYYY-MM-DD', en la zona local del navegador */
    BT.todayISO = function () {
        const now = new Date();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        return now.getFullYear() + '-' + month + '-' + day;
    };

    /** Agrega días a un Date y devuelve 'YYYY-MM-DD' */
    BT.addDaysISO = function (isoString, days) {
        const date = BT.parseISODate(isoString);
        date.setDate(date.getDate() + days);
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return date.getFullYear() + '-' + month + '-' + day;
    };

    /** '1' = lunes ... '7' = domingo, igual que en la base de datos */
    BT.isoDayOfWeek = function (isoString) {
        const day = BT.parseISODate(isoString).getDay();   // 0 = domingo
        return day === 0 ? 7 : day;
    };

    /** Busca el horario de atención de un día de la semana */
    BT.getHoursForDay = function (isoString) {
        // SITE_CONFIG.hours está ordenado de lunes a domingo, así que el
        // índice es exactamente el número del día menos uno.
        const dayOfWeek = BT.isoDayOfWeek(isoString);   // 1 = lunes
        return SITE_CONFIG.hours[dayOfWeek - 1] || null;
    };

    /**
     * Convierte minutos en 'X min' o 'X h'.
     * No se muestra al cliente (la duración de un corte varía), pero el
     * panel del dueño sí la va a necesitar para mostrar los turnos del día.
     */
    BT.formatDuration = function (minutes) {
        if (minutes < 60) {
            return minutes + ' min';
        }
        const hours = Math.floor(minutes / 60);
        const rest = minutes % 60;
        return rest === 0 ? hours + ' h' : hours + ' h ' + rest + ' min';
    };

    /** '2026-10-20' -> 'martes 20 de octubre' */
    BT.formatLongDate = function (isoString) {
        const date = BT.parseISODate(isoString);
        const dayName = date.toLocaleDateString('es-AR', { weekday: 'long' });
        const day = date.getDate();
        const month = date.toLocaleDateString('es-AR', { month: 'long' });
        return dayName + ' ' + day + ' de ' + month;
    };

    window.BT = BT;

    // ==========================================================================
    // Atajos
    // ==========================================================================

    const $ = function (selector) {
        return document.querySelector(selector);
    };

    /**
     * Crea un elemento con clase y contenido.
     * Evita ensuciar el código con.innerHTML cuando se insertan datos,
     * porque innerHTML interpreta HTML: si un dato viniera con etiquetas,
     * se ejecutaría. Con textContent eso nunca pasa.
     */
    function el(tag, className, textContent) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (textContent !== undefined && textContent !== null) {
            node.textContent = textContent;
        }
        return node;
    }

    // ==========================================================================
    // 1. Encabezado y navegación
    // ==========================================================================

    function initHeader() {
        const header = $('#site-header');

        // Al hacer scroll, el encabezado se vuelve opaco.
        // Se usa un listener pasivo para no frenar el scroll.
        const onScroll = function () {
            header.classList.toggle('is-scrolled', window.scrollY > 20);
        };

        window.addEventListener('scroll', onScroll, { passive: true });
        onScroll();
    }

    function initNav() {
        const toggle = $('#nav-toggle');
        const nav = $('#main-nav');

        if (!toggle || !nav) return;

        const setOpen = function (isOpen) {
            toggle.setAttribute('aria-expanded', String(isOpen));
            toggle.setAttribute(
                'aria-label',
                isOpen ? 'Cerrar menú de navegación' : 'Abrir menú de navegación'
            );
            nav.classList.toggle('is-open', isOpen);
        };

        toggle.addEventListener('click', function () {
            const isOpen = toggle.getAttribute('aria-expanded') === 'true';
            setOpen(!isOpen);
        });

        // Al tocar un enlace del menú en celular, se cierra solo.
        nav.querySelectorAll('a').forEach(function (link) {
            link.addEventListener('click', function () {
                setOpen(false);
            });
        });

        // Con la tecla Escape se cierra el menú.
        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape' && toggle.getAttribute('aria-expanded') === 'true') {
                setOpen(false);
                toggle.focus();
            }
        });

        // Si la ventana se agranda y pasa el ancho del breakpoint, el menú
        // tiene que volver a su estado de escritorio.
        window.addEventListener('resize', function () {
            if (window.innerWidth > 860) {
                setOpen(false);
            }
        });
    }

    // ==========================================================================
    // 2. Datos del sitio (marca, contacto, pie)
    // ==========================================================================

    function renderBrand() {
        const brand = SITE_CONFIG.brand;

        document.title = brand.name + ' — Reserva tu turno';

        const meta = $('#meta-description');
        if (meta) meta.setAttribute('content', brand.metaDescription);

        ['#brand-name', '#footer-brand-name'].forEach(function (selector) {
            const node = $(selector);
            if (node) node.textContent = brand.name;
        });

        ['#brand-tagline', '#footer-tagline'].forEach(function (selector) {
            const node = $(selector);
            if (node) node.textContent = brand.tagline || '';
        });
    }

    /**
     * Portada: los textos vienen de la configuración para que se puedan
     * cambiar sin tocar el HTML.
     */
    function renderHero() {
        const hero = SITE_CONFIG.hero;
        if (!hero) return;

        const targets = {
            '#hero-eyebrow': hero.eyebrow,
            '#hero-title-1': hero.titleLine1,
            '#hero-title-2': hero.titleLine2,
            '#hero-text': hero.text
        };

        Object.keys(targets).forEach(function (selector) {
            const node = $(selector);
            if (node && targets[selector]) {
                node.textContent = targets[selector];
            }
        });
    }

    function renderContact() {
        const contact = SITE_CONFIG.contact;

        // Nota: la tarjeta de "Ubicación" (dirección y link al mapa) se sacó
        // de la página por pedido. Los datos siguen guardados en
        // SITE_CONFIG.contact.address por si hay que usarlos más adelante.

        // --- WhatsApp ---
        // wa.me abre el chat con ese número. Se arma la URL con el número
        // en formato internacional y sin signos, como lo exige WhatsApp.
        const whatsappUrl = 'https://wa.me/' + contact.whatsappNumber;

        const whatsappLink = $('#whatsapp-link');
        if (whatsappLink) whatsappLink.href = whatsappUrl;

        const footerWhatsapp = $('#footer-whatsapp');
        if (footerWhatsapp) footerWhatsapp.href = whatsappUrl;

        const phone = $('#contact-phone');
        if (phone) phone.textContent = contact.phoneDisplay;

        // --- Instagram ---
        const instagramUrl = contact.instagram
            ? 'https://instagram.com/' + contact.instagram.replace('@', '')
            : '';

        const instagramLink = $('#instagram-link');
        if (instagramLink) {
            if (contact.instagram) {
                instagramLink.href = instagramUrl;
                instagramLink.removeAttribute('aria-hidden');
            } else {
                // Sin Instagram configurado: se oculta el enlace en vez de
                // dejar uno roto.
                instagramLink.closest('li').hidden = true;
            }
        }

        const footerInstagram = $('#footer-instagram');
        if (footerInstagram) {
            if (contact.instagram) {
                footerInstagram.href = instagramUrl;
            } else {
                footerInstagram.hidden = true;
            }
        }

        const instagramText = $('#contact-instagram');
        if (instagramText) {
            instagramText.textContent = contact.instagram
                ? '@' + contact.instagram.replace('@', '')
                : '';
        }

        // --- Email ---
        const emailLink = $('#email-link');
        if (emailLink && contact.email) {
            // mailto: abre el programa de correo del visitante
            emailLink.href = 'mailto:' + contact.email;
        } else if (emailLink) {
            emailLink.closest('li').hidden = true;
        }

        const emailText = $('#contact-email');
        if (emailText) emailText.textContent = contact.email || '';
    }

    function renderHours() {
        const body = $('#hours-body');
        if (!body) return;

        // El array de SITE_CONFIG.hours arranca en lunes, pero getDay()
        // devuelve 0 para domingo. Se rota para comparar bien.
        const todayISO = BT.todayISO();
        const todayIndex = BT.isoDayOfWeek(todayISO);   // 1 = lunes

        body.innerHTML = '';

        SITE_CONFIG.hours.forEach(function (entry, index) {
            const row = el('tr');
            row.className = 'hours-row';

            // Resalta el día de hoy para que el visitante lo vea de una
            const isToday = (index + 1) === todayIndex;
            if (isToday) row.classList.add('is-today');

            const dayCell = el('td', null, entry.day);
            row.appendChild(dayCell);

            const timeCell = el('td', null, entry.closed ? 'Cerrado' : entry.open + ' – ' + entry.close);
            if (entry.closed) timeCell.className = 'is-closed';
            row.appendChild(timeCell);

            body.appendChild(row);
        });
    }

    function renderFooter() {
        const copy = $('#footer-copy');
        if (copy) {
            const year = new Date().getFullYear();
            copy.textContent = '© ' + year + ' ' + SITE_CONFIG.brand.name +
                '. Todos los derechos reservados.';
        }

        // Aviso de datos de ejemplo.
        // Mientras esté en true, el visitante sabe que lo que ve no es real.
        const notice = $('#sample-data-notice');
        if (notice) {
            notice.hidden = !SITE_CONFIG.flags.showSampleDataNotice;
        }
    }

    // ==========================================================================
    // 3. Servicios
    // ==========================================================================

    function renderServices() {
        const grid = $('#services-grid');
        if (!grid) return;

        grid.innerHTML = '';

        SITE_CONFIG.services.forEach(function (service) {
            const card = el('article', 'service-card');

            if (service.popular) {
                card.appendChild(el('span', 'badge', 'Más pedido'));
            }

            // --- Encabezado: nombre y precio ---
            const head = el('div', 'service-card__head');

            const name = el('h3', 'service-card__name', service.name);
            head.appendChild(name);

            const price = el('span', 'service-card__price', BT.formatPrice(service.price));
            head.appendChild(price);

            card.appendChild(head);

            // --- Descripción ---
            card.appendChild(el('p', 'service-card__desc', service.description));

            // --- Pie: botón de reserva ---
            // No se muestra la duración: un corte no dura siempre lo mismo
            // (depende del pelo y de lo que pida el cliente), así que
            // poner un número fijo sería mentir. El sistema igual la usa
            // por dentro para calcular la grilla de horarios.
            const foot = el('div', 'service-card__foot');

            // type="button" porque está dentro de un <div>, no de un formulario.
            // Si no se indica, el botón se trataría como "enviar".
            const cta = el('button', 'service-card__cta', 'Reservar');
            cta.type = 'button';
            cta.setAttribute('aria-label', 'Reservar ' + service.name);

            cta.addEventListener('click', function () {
                selectService(service.id);
                scrollToBooking();
            });

            foot.appendChild(cta);
            card.appendChild(foot);

            grid.appendChild(card);
        });
    }

    /**
     * Dibuja el selector de servicio del formulario.
     * Son <input type="radio"> ocultos con un <label> visible: así se
     * comportan como un grupo de opciones real, se recorren con las flechas
     * del teclado y los lectores de pantalla los anuncian correctamente.
     * Un grupo de <div> con click no daría ninguna de esas garantías.
     */
    function renderServicePicker() {
        const picker = $('#service-picker');
        if (!picker) return;

        picker.innerHTML = '';

        SITE_CONFIG.services.forEach(function (service) {
            const option = el('div', 'service-option');

            const input = el('input', 'service-option__input');
            input.type = 'radio';
            input.name = 'servicio';
            input.id = 'servicio-' + service.id;
            input.value = String(service.id);

            // Avisa al resto de la página que cambió la selección
            input.addEventListener('change', function () {
                window.dispatchEvent(new CustomEvent('bt:service-selected', {
                    detail: { serviceId: service.id }
                }));
            });

            option.appendChild(input);

            const label = el('label', 'service-option__label');
            label.htmlFor = input.id;

            const text = el('span', 'service-option__text');
            text.appendChild(el('span', 'service-option__name', service.name));
            label.appendChild(text);

            label.appendChild(el(
                'span',
                'service-option__price',
                BT.formatPrice(service.price)
            ));

            option.appendChild(label);
            picker.appendChild(option);
        });
    }

    /** Marca un servicio como elegido (usado por los botones "Reservar") */
    function selectService(serviceId) {
        const input = document.getElementById('servicio-' + serviceId);
        if (!input) return;

        input.checked = true;

        // Disparar 'change' a mano: si no, el resumen no se entera
        input.dispatchEvent(new Event('change', { bubbles: true }));
    }

    function scrollToBooking() {
        const target = document.getElementById('reservar');
        if (!target) return;

        // scrollIntoView con 'smooth' y un pequeño margen para que el
        // encabezado fijo no tape el título.
        target.scrollIntoView({
            behavior: 'smooth',
            block: 'start'
        });
    }

    // ==========================================================================
    // 4. Galería
    // ==========================================================================

    function renderGallery() {
        const grid = $('#gallery-grid');
        if (!grid) return;

        grid.innerHTML = '';

        SITE_CONFIG.gallery.forEach(function (item) {
            const figure = el('figure', 'gallery-item');

            if (item.src) {
                // --- Foto real ---
                // loading="lazy": el navegador no descarga la imagen hasta
                // que está por entrar en pantalla. Como la galería está al
                // final de la página, eso ahorra ancho de banda de entrada.
                const image = el('img', 'gallery-item__photo');
                image.src = item.src;
                image.alt = item.title;          // el texto ya describe la foto
                image.loading = 'lazy';
                image.decoding = 'async';
                figure.appendChild(image);

            } else {
                // --- Marcador de posición ---
                // Fondo generado con CSS: no hay fotos falsas que parezcan reales.
                const background = el('div', 'gallery-item__bg ' + (item.tone || 'tone-2'));
                figure.appendChild(background);

                const overlay = el('div', 'gallery-item__overlay');
                figure.appendChild(overlay);

                // Ícono de tijeras, dibujado con SVG dentro del documento.
                const iconWrap = el('span', 'gallery-item__icon');
                iconWrap.innerHTML =
                    '<svg viewBox="0 0 48 48" width="72" height="72" fill="none" ' +
                    'stroke="currentColor" stroke-width="1.5" aria-hidden="true" focusable="false">' +
                    '<circle cx="13" cy="36" r="7"/><circle cx="35" cy="36" r="7"/>' +
                    '<path d="M17 31 L36 8 M31 31 L12 8"/>' +
                    '</svg>';
                figure.appendChild(iconWrap);

                // Etiqueta que aclara que es un marcador de posición
                figure.appendChild(el('span', 'gallery-item__badge', 'Próximamente'));
            }

            // En los dos casos, el degradado inferior hace legible el texto.
            if (item.src) {
                figure.appendChild(el('div', 'gallery-item__overlay'));
            }

            const caption = el('figcaption', 'gallery-item__caption', item.title);
            figure.appendChild(caption);

            grid.appendChild(figure);
        });
    }

    // ==========================================================================
    // Arranque
    // ==========================================================================

    function init() {
        initHeader();
        initNav();

        renderBrand();
        renderHero();
        renderContact();
        renderHours();
        renderFooter();
        renderServices();
        renderServicePicker();
        renderGallery();
    }

    init();

})();
