package com.barbertrend.web;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Páginas HTML del panel del barbero.
 *
 * <p>La página de login es pública, pero si el barbero ya tiene una sesión
 * abierta no tiene sentido mostrarle el formulario otra vez: se lo redirige
 * al panel. Así el enlace "Acceso del barbero" funciona tanto con la sesión
 * abierta como sin ella.
 *
 * <p>Cuando no hay sesión, se hace un forward interno a
 * {@code /admin/login.html} para que la URL visible siga siendo
 * {@code /admin/login}.
 */
@Controller
public class AdminPageController {

    @GetMapping("/admin/login")
    public String login(Authentication authentication) {
        if (estaAutenticado(authentication)) {
            return "redirect:/admin";
        }

        return "forward:/admin/login.html";
    }

    /**
     * Spring Security representa al visitante sin sesión con un
     * {@link AnonymousAuthenticationToken}, que también está "autenticado"
     * a nivel técnico. Por eso hay que excluirlo explícitamente.
     */
    private boolean estaAutenticado(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
