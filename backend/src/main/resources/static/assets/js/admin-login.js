/* ==========================================================================
 * admin-login.js — Formulario de login del panel del barbero
 * ==========================================================================
 *
 *  Qué hace:
 *    - Lee la cookie CSRF que escribe Spring Security (XSRF-TOKEN) y la
 *      copia al campo oculto _csrf del formulario.
 *    - Muestra el mensaje de error que llega por la URL (?error) o el aviso
 *      de sesión cerrada (?logout).
 *    - Valida que los dos campos estén completos antes de enviar.
 *
 *  El envío es un POST nativo del formulario a /login. No se usa fetch:
 *  Spring Security responde con una redirección y el navegador la sigue.
 *  Si las credenciales están mal, vuelve a /admin/login?error.
 * ========================================================================== */

'use strict';

(function () {

    var TOKEN_COOKIE = 'XSRF-TOKEN';

    /**
     * Lee una cookie del documento.
     * Devuelve '' si no existe.
     */
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
     * Muestra un aviso arriba del formulario.
     * El contenedor tiene role="status" y aria-live, así que el lector de
     * pantalla lo anuncia solo.
     */
    function showStatus(type, title, message) {
        var container = document.getElementById('login-status');
        if (!container) return;

        container.innerHTML = '';

        var alert = document.createElement('div');
        alert.className = 'alert alert-' + type;

        var icon = document.createElement('span');
        icon.className = 'alert-icon';
        icon.setAttribute('aria-hidden', 'true');
        icon.textContent = type === 'error' ? '!' : 'i';
        alert.appendChild(icon);

        var body = document.createElement('div');

        var strong = document.createElement('strong');
        strong.textContent = title;
        body.appendChild(strong);

        body.appendChild(document.createTextNode(' ' + message));
        alert.appendChild(body);

        container.appendChild(alert);
    }

    function clearFieldErrors() {
        ['username-error', 'password-error'].forEach(function (id) {
            var node = document.getElementById(id);
            if (node) node.textContent = '';
        });

        ['username', 'password'].forEach(function (id) {
            var input = document.getElementById(id);
            if (input) input.removeAttribute('aria-invalid');
        });
    }

    function clearStatus() {
        var container = document.getElementById('login-status');
        if (container) container.innerHTML = '';
    }

    function init() {
        var form = document.getElementById('login-form');
        var csrfField = document.getElementById('csrf-token');
        var submit = document.getElementById('login-submit');
        var username = document.getElementById('username');
        var password = document.getElementById('password');

        if (!form || !csrfField || !submit || !username || !password) {
            return;
        }

        // --- Mensajes que deja Spring Security en la URL ---
        var params = new URLSearchParams(window.location.search);

        if (params.has('error')) {
            showStatus(
                'error',
                'No pudimos iniciar sesión.',
                'Revisá el usuario y la contraseña.'
            );
            username.focus();
        } else if (params.has('logout')) {
            showStatus(
                'success',
                'Sesión cerrada.',
                'Podés volver a entrar cuando quieras.'
            );
        }

        // --- Token CSRF ---
        // Sin token, el POST daría 403. Mejor avisar antes.
        var token = getCookie(TOKEN_COOKIE);

        if (!token) {
            submit.disabled = true;
            showStatus(
                'error',
                'No se pudo preparar el formulario.',
                'Recargá la página e intentá de nuevo.'
            );
            return;
        }

        csrfField.value = token;

        // --- Envío ---
        form.addEventListener('submit', function (event) {
            clearStatus();
            clearFieldErrors();

            var missingUsername = username.value.trim() === '';
            var missingPassword = password.value === '';

            if (missingUsername || missingPassword) {
                event.preventDefault();

                if (missingUsername) {
                    username.setAttribute('aria-invalid', 'true');
                    document.getElementById('username-error').textContent =
                        'Escribí tu usuario.';
                }

                if (missingPassword) {
                    password.setAttribute('aria-invalid', 'true');
                    document.getElementById('password-error').textContent =
                        'Escribí tu contraseña.';
                }

                (missingUsername ? username : password).focus();
                return;
            }

            // El token puede haber cambiado: se refresca por las dudas.
            csrfField.value = getCookie(TOKEN_COOKIE) || csrfField.value;

            // Evita doble clic mientras el navegador espera la respuesta.
            submit.disabled = true;
            submit.classList.add('is-loading');
        });

        // Al volver con el botón "Atrás" el botón puede quedar deshabilitado.
        window.addEventListener('pageshow', function () {
            submit.disabled = false;
            submit.classList.remove('is-loading');
        });
    }

    init();

})();
