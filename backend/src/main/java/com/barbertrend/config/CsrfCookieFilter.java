package com.barbertrend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Fuerza la generación del token CSRF para que Spring Security escriba la
 * cookie {@code XSRF-TOKEN}.
 *
 * <h2>Por qué hace falta</h2>
 *
 * <p>La página de login es un archivo estático: nadie lee el token desde el
 * servidor para imprimirlo en el HTML. Spring Security 6/7 genera el token de
 * forma "diferida": si nadie lo pide, la cookie no se escribe y el formulario
 * quedaría sin token.
 *
 * <p>Este filtro corre después del {@code CsrfFilter} y pide el token. Con
 * eso la cookie viaja en la respuesta y el JavaScript del login puede leerla.
 *
 * <p>Solo actúa en las rutas del panel: el sitio público no necesita la
 * cookie.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Se usa el requestURI (y no getServletPath) porque MockMvc deja el
        // servletPath vacío. Con el context path descontado funciona igual
        // en pruebas y en producción.
        String path = request.getRequestURI()
                .substring(request.getContextPath().length());

        return !path.startsWith("/admin");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        if (csrfToken != null) {
            // Pedir el token es lo que dispara el guardado de la cookie.
            csrfToken.getToken();
        }

        filterChain.doFilter(request, response);
    }
}
