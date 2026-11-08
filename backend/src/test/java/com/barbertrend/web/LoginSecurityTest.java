package com.barbertrend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Pruebas del acceso al panel del barbero.
 *
 * <h2>Qué se prueba</h2>
 * <ul>
 *   <li>La página de login es pública y entrega el token CSRF.</li>
 *   <li>El panel y la API privada no se pueden usar sin sesión.</li>
 *   <li>Con las credenciales correctas se entra y se llega al panel.</li>
 *   <li>Si ya hay sesión, el login redirige al panel en vez de mostrar el
 *       formulario otra vez.</li>
 *   <li>Con credenciales incorrectas se vuelve al login con el aviso.</li>
 *   <li>Un POST de login sin token CSRF se rechaza.</li>
 *   <li>Cerrar sesión redirige al login.</li>
 * </ul>
 *
 * <h2>Cómo se manda el token CSRF</h2>
 *
 * <p>Igual que el navegador: se pide la página de login, se lee la cookie
 * {@code XSRF-TOKEN} y se manda su valor en el campo {@code _csrf} junto con
 * la cookie. No se usa el post-procesador {@code csrf()} de
 * {@code spring-security-test} porque reemplaza el repositorio CSRF del
 * filtro compartido y las pruebas siguientes dejarían de ver la cookie.
 *
 * <h2>Credenciales de prueba</h2>
 *
 * <p>Se definen con {@code @SpringBootTest(properties = ...)}: son datos
 * descartables que solo existen mientras corre esta prueba. No hay ninguna
 * contraseña real en el código.
 */
@SpringBootTest(properties = {
        "ADMIN_USERNAME=barbero-de-prueba",
        "ADMIN_PASSWORD=clave-de-prueba",
        // Se limpia el hash que pueda venir del .env: si queda, SecurityConfig
        // lo prefiere y la contraseña de prueba no sirve.
        "ADMIN_PASSWORD_HASH="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginSecurityTest {

    private static final String USUARIO = "barbero-de-prueba";
    private static final String CLAVE = "clave-de-prueba";
    private static final String COOKIE_CSRF = "XSRF-TOKEN";

    @Autowired
    private MockMvc mvc;

    @Test
    void laPaginaDeLoginEsPublicaYEntregaElTokenCsrf() throws Exception {
        mvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/admin/login.html"))
                .andExpect(cookie().exists(COOKIE_CSRF));
    }

    @Test
    void elPanelSinSesionRedirigeAlLogin() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));
    }

    @Test
    void laApiPrivadaSinSesionDevuelve401() throws Exception {
        mvc.perform(get("/api/admin/appointments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conCredencialesCorrectasSeEntraAlPanel() throws Exception {
        MockHttpSession session = iniciarSesion();

        assertThat(session).isNotNull();

        mvc.perform(get("/admin").session(session))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/admin/index.html"));

        mvc.perform(get("/api/admin/appointments").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void conSesionIniciadaElLoginRedirigeAlPanel() throws Exception {
        MockHttpSession session = iniciarSesion();

        mvc.perform(get("/admin/login").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void conCredencialesIncorrectasVuelveAlLoginConError() throws Exception {
        Cookie csrf = pedirTokenCsrf();

        mvc.perform(post("/login")
                        .cookie(csrf)
                        .param("_csrf", csrf.getValue())
                        .param("username", USUARIO)
                        .param("password", "clave-incorrecta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?error"));
    }

    @Test
    void elLoginSinTokenCsrfSeRechaza() throws Exception {
        mvc.perform(post("/login")
                        .param("username", USUARIO)
                        .param("password", CLAVE))
                .andExpect(status().isForbidden());
    }

    @Test
    void cerrarSesionVuelveAlLogin() throws Exception {
        MockHttpSession session = iniciarSesion();

        // El panel entrega una cookie CSRF nueva para el POST de salida.
        MvcResult panel = mvc.perform(get("/admin").session(session))
                .andExpect(status().isOk())
                .andReturn();

        Cookie csrf = panel.getResponse().getCookie(COOKIE_CSRF);
        assertThat(csrf).isNotNull();

        mvc.perform(post("/admin/logout")
                        .session(session)
                        .cookie(csrf)
                        .param("_csrf", csrf.getValue()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?logout"));
    }

    /**
     * Pide la página de login y devuelve la cookie CSRF que entrega Spring
     * Security, igual que haría el navegador.
     */
    private Cookie pedirTokenCsrf() throws Exception {
        MvcResult result = mvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie(COOKIE_CSRF);
        assertThat(cookie).as("cookie %s", COOKIE_CSRF).isNotNull();

        return cookie;
    }

    /**
     * Hace el POST de login con credenciales válidas y devuelve la sesión
     * creada, para reutilizarla en las peticiones siguientes.
     */
    private MockHttpSession iniciarSesion() throws Exception {
        Cookie csrf = pedirTokenCsrf();

        MvcResult result = mvc.perform(post("/login")
                        .cookie(csrf)
                        .param("_csrf", csrf.getValue())
                        .param("username", USUARIO)
                        .param("password", CLAVE))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
