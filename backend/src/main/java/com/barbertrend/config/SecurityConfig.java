package com.barbertrend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Seguridad de la aplicación.
 *
 * <h2>Qué está abierto y qué no</h2>
 * <ul>
 *   <li>{@code /api/public/**} y el sitio público: abiertos.</li>
 *   <li>{@code /admin/login} y su archivo estático: abiertos. La página de
 *       login tiene que poder verse sin sesión, si no la redirección al
 *       login entraría en un bucle.</li>
 *   <li>{@code /api/admin/**}: exige sesión iniciada.</li>
 *   <li>{@code /admin/**}: exige sesión iniciada. Es el panel del barbero.</li>
 * </ul>
 *
 * <h2>Cómo se inicia sesión</h2>
 *
 * <p>Se usa el <b>form login de Spring Security</b> con una página propia en
 * {@code /admin/login}. El formulario hace {@code POST /login} con el usuario,
 * la contraseña y el token CSRF. Si las credenciales son correctas, Spring
 * crea la sesión y redirige a {@code /admin}; si no, vuelve al login con
 * {@code ?error}. El cierre de sesión es {@code POST /admin/logout}.
 *
 * <h2>Por qué sesión y no JWT</h2>
 *
 * <p>Hay un solo panel y un solo usuario. Un JWT obliga a guardar y
 * renovar el token en el navegador, a manejar expiraciones en el cliente y
 * a complicar el cierre de sesión. Una cookie de sesión hace todo eso en el
 * servidor, que es donde ya vive la información. Para este caso, la opción
 * simple es la correcta.
 *
 * <h2>CSRF</h2>
 *
 * <p>Se deja activado. El panel usa cookies, así que sin CSRF un sitio
 * cualquiera podría mandar una petición "en nombre" del barbero.
 *
 * <p>La página de login es estática (no la arma Spring), así que el token
 * se guarda en una cookie legible por JavaScript ({@code XSRF-TOKEN}) y el
 * formulario lo manda en el campo {@code _csrf}. Para que Spring acepte el
 * valor tal cual se usa {@link CsrfTokenRequestAttributeHandler}, que evita
 * el enmascarado XOR por defecto (pensado para tokens renderizados en el
 * HTML, caso que acá no aplica).
 *
 * <p>El {@link CsrfCookieFilter} fuerza que la cookie se escriba aunque la
 * respuesta sea un archivo estático, en el que nadie "lee" el token.
 */
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    /** Rutas que puede usar cualquiera, sin iniciar sesión. */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/public/**",
            "/h2-console/**",
            "/actuator/health",
            "/",
            "/index.html",
            "/assets/**",
            "/favicon.ico",
            // Login del panel. El ".html" es el archivo al que se reenvía
            // /admin/login (ver WebConfig).
            "/admin/login",
            "/admin/login.html"
    };

    /** Rutas del panel: requieren sesión. */
    private static final String[] ADMIN_ENDPOINTS = {
            "/api/admin/**",
            "/admin/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Cada API y cada endpoint público define sus propias reglas.
            // Sin esto, Spring protege todo por defecto y hay que andar
            // abriendo excepciones de a una.
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                    .requestMatchers(ADMIN_ENDPOINTS).authenticated()
                    // Cualquier ruta que no esté en las listas anteriores
                    // queda cerrada. Es mejor olvidar abrir algo que
                    // exponerlo por error.
                    .anyRequest().denyAll()
            )

            // CSRF: token en cookie para que lo pueda leer el formulario
            // estático de login. La API pública queda exceptuada.
            .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                    .ignoringRequestMatchers(
                            PathPatternRequestMatcher.pathPattern("/api/public/**"))
            )

            // Login del panel. POST /login lo procesa Spring Security antes
            // de las reglas de autorización, así que no hace falta abrirlo.
            .formLogin(form -> form
                    .loginPage("/admin/login")
                    .loginProcessingUrl("/login")
                    .defaultSuccessUrl("/admin", true)
                    .failureUrl("/admin/login?error")
            )

            // Solo POST cierra la sesión: un GET /admin/logout podría
            // dispararse sin querer (por ejemplo, con una imagen en un
            // correo o un enlace externo).
            .logout(logout -> logout
                    .logoutUrl("/admin/logout")
                    .logoutSuccessUrl("/admin/login?logout")
            )

            // La cookie se escribe desde un filtro propio.
            .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)

            // La API no necesita navegación entre sitios.
            .sessionManagement(session -> session
                    .sessionFixation(fixation -> fixation.changeSessionId())
            )

            // Cabeceras de seguridad básicas.
            .headers(headers -> headers
                    .frameOptions(frame -> frame.sameOrigin())   // para el H2 console
                    .referrerPolicy(referrer -> referrer.policy(
                            ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
            )

            // Qué pasa cuando falta la sesión:
            //   - las páginas del panel redirigen al login;
            //   - la API privada devuelve 401 (así el JavaScript puede
            //     distinguir "sin sesión" de un error de negocio).
            //
            // Si se declara un solo entry point, Spring lo aplica a TODAS
            // las rutas; por eso se declaran los dos.
            .exceptionHandling(ex -> ex
                    .defaultAuthenticationEntryPointFor(
                            new LoginUrlAuthenticationEntryPoint("/admin/login"),
                            PathPatternRequestMatcher.pathPattern("/admin/**"))
                    .defaultAuthenticationEntryPointFor(
                            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                            PathPatternRequestMatcher.pathPattern("/api/**"))
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Usuario del panel.
     *
     * <p>Se lee del entorno, nunca del código. Se prefieren las variables:
     *
     * <ul>
     *   <li>{@code ADMIN_PASSWORD_HASH}: hash BCrypt. Es la que hay que usar
     *       en producción.</li>
     *   <li>{@code ADMIN_PASSWORD}: texto plano. Solo para desarrollo; si se
     *       usa, la aplicación escribe un aviso en el log.</li>
     * </ul>
     */
    @Bean
    public UserDetailsService userDetailsService(AppProperties properties,
                                                 PasswordEncoder encoder,
                                                 Environment environment) {

        String username = properties.getAdminUsername();
        String hash = environment.getProperty("ADMIN_PASSWORD_HASH");
        String plain = environment.getProperty("ADMIN_PASSWORD");

        if (hash != null && !hash.isBlank()) {
            // El hash ya viene calculado: se usa tal cual.
            return new InMemoryUserDetailsManager(
                    User.withUsername(username).password(hash).roles("ADMIN").build());
        }

        if (plain != null && !plain.isBlank()) {
            log.warn("=".repeat(70));
            log.warn("ADMIN_PASSWORD está en texto plano.");
            log.warn("En producción usá ADMIN_PASSWORD_HASH (hash BCrypt) en su lugar.");
            log.warn("=".repeat(70));

            return new InMemoryUserDetailsManager(
                    User.withUsername(username)
                            .password(encoder.encode(plain))
                            .roles("ADMIN")
                            .build());
        }

        // Sin ninguna de las dos: no se inventa una contraseña.
        // Spring Boot, sin esto, genera una al azar y la imprime por log.
        log.warn("No hay ADMIN_PASSWORD_HASH ni ADMIN_PASSWORD en el entorno.");
        log.warn("El panel no se puede usar hasta que se defina una contraseña.");

        return new InMemoryUserDetailsManager();
    }
}
