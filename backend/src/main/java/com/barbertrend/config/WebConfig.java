package com.barbertrend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Rutas de las páginas del panel.
 *
 * <h2>Por qué hace falta</h2>
 *
 * <p>Las páginas del panel son archivos estáticos, pero Spring MVC solo
 * resuelve automáticamente el {@code index.html} de la raíz. Un pedido a
 * {@code /admin} no encuentra {@code static/admin/index.html} por sí solo.
 * Esta regla hace el reenvío interno (forward) al archivo real, para que la
 * URL que ve el barbero quede limpia.
 *
 * <p>{@code /admin/login} no se maneja acá: lo resuelve
 * {@link com.barbertrend.web.AdminPageController}, que además redirige al
 * panel cuando ya hay una sesión iniciada.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/admin")
                .setViewName("forward:/admin/index.html");
    }
}
