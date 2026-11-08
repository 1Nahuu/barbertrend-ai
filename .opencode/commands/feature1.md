# FEATURES.md — BarberTrend AI

## Estados

- [ ] Pendiente
- [x] Completado y verificado

Una funcionalidad solo se marca como completada después de implementarla y comprobarla.

## Fase 0 — Preparación

- [ ] Inspeccionar el entorno y las herramientas instaladas.
- [ ] Crear la estructura del proyecto.
- [ ] Definir versiones compatibles de Java, Spring Boot y Maven.
- [ ] Crear el repositorio Git.
- [ ] Preparar README.md.
- [ ] Configurar `.gitignore`.
- [ ] Crear `.env.example`.
- [ ] Documentar cómo ejecutar el proyecto localmente.

## Fase 1 — Web pública

- [ ] Diseñar la identidad visual inicial.
- [ ] Crear una página de inicio responsive.
- [ ] Mostrar los servicios y sus precios.
- [ ] Mostrar una galería de cortes.
- [ ] Añadir la información de contacto.
- [ ] Incorporar enlaces a Instagram y WhatsApp.
- [ ] Crear la sección de reservas.
- [ ] Añadir estados de carga, éxito y error.
- [ ] Comprobar el diseño en celulares y computadoras.

## Fase 2 — Backend y base de datos

- [ ] Crear la aplicación Spring Boot.
- [ ] Configurar la conexión a la base de datos.
- [ ] Definir entidades y relaciones.
- [ ] Implementar validaciones.
- [ ] Crear el modelo de servicios.
- [ ] Crear el modelo de clientes.
- [ ] Crear el modelo de turnos.
- [ ] Implementar endpoints REST.
- [ ] Añadir manejo de errores.
- [ ] Escribir pruebas unitarias y de integración pertinentes.

## Fase 3 — Agenda y reservas

- [x] Configurar días y horarios de atención.
- [x] Definir la duración de cada servicio.
- [x] Calcular horarios disponibles.
- [x] Permitir seleccionar fecha y servicio.
- [x] Recibir los datos del cliente.
- [x] Verificar disponibilidad en el backend.
- [x] Evitar reservas simultáneas para el mismo horario.
- [x] Registrar las reservas.
- [x] Permitir consultar el estado de una reserva.
- [x] Implementar cancelaciones.
- [ ] Implementar modificaciones de turnos.
- [x] Mostrar confirmación de la reserva.

## Fase 4 — Panel administrativo

- [x] Crear acceso privado para el dueño.
- [x] Implementar autenticación segura.
- [x] Proteger las rutas administrativas.
- [x] Mostrar la agenda diaria.
- [x] Consultar turnos futuros.
- [x] Filtrar turnos por fecha y estado.
- [ ] Crear y editar servicios.
- [ ] Administrar precios y duraciones.
- [ ] Administrar horarios de atención.
- [x] Cancelar y modificar reservas.
- [x] Mostrar estados de las notificaciones.

## Fase 5 — WhatsApp

- [x] Investigar requisitos y costos actuales de la API oficial.
- [ ] Configurar un entorno de prueba.
- [x] Implementar un servicio de notificaciones independiente (email, desacoplado).
- [x] Generar mensajes de confirmación.
- [x] Añadir un modo simulado sin envíos reales (dry-run del email).
- [x] Configurar credenciales mediante variables de entorno.
- [x] Implementar el envío real (manual con enlace `wa.me`; la API oficial queda pendiente).
- [ ] Registrar el estado de los envíos.
- [ ] Manejar errores y reintentos.
- [ ] Evitar mensajes duplicados.
- [ ] Revisar requisitos de plantillas y ventanas de mensajería.
- [ ] Implementar recordatorios, si resultan convenientes.
- [ ] Evaluar respuestas para cancelar o cambiar turnos.

## Fase 6 — Asistente de marketing con IA

- [ ] Definir el perfil de la barbería.
- [ ] Configurar servicios y estilos de corte.
- [ ] Crear el modelo de tendencias.
- [ ] Crear el modelo de ideas de contenido.
- [ ] Diseñar una interfaz intercambiable para proveedores de IA.
- [ ] Generar ideas a partir de datos de ejemplo.
- [ ] Generar guiones para Reels.
- [ ] Generar captions y llamadas a reservar.
- [ ] Proponer historias y publicaciones.
- [ ] Guardar las ideas generadas.
- [ ] Evitar sugerencias repetidas.
- [ ] Validar la salida de la IA.
- [ ] Identificar qué afirmaciones están verificadas y cuáles son sugerencias.

## Fase 7 — Fuentes de tendencias

- [ ] Investigar fuentes de tendencias disponibles.
- [ ] Verificar permisos y condiciones de uso.
- [ ] Comprobar si existe acceso programático permitido.
- [ ] Documentar límites, cobertura y costos.
- [ ] Implementar el primer adaptador de fuente.
- [ ] Guardar enlaces originales y fechas disponibles.
- [ ] Registrar las señales de popularidad accesibles.
- [ ] Filtrar tendencias relevantes para barberías.
- [ ] Implementar un modo alternativo cuando una fuente falle.
- [ ] Evitar inventar estadísticas o enlaces.
- [ ] Probar la calidad de las recomendaciones con ejemplos reales.

## Fase 8 — Automatización

- [ ] Definir la frecuencia de búsqueda.
- [ ] Configurar la zona horaria de Argentina.
- [ ] Ejecutar búsquedas programadas.
- [ ] Generar recomendaciones automáticamente.
- [ ] Preparar el resumen para WhatsApp.
- [ ] Configurar horarios de envío.
- [ ] Evitar múltiples ejecuciones simultáneas.
- [ ] Añadir registros de actividad y errores.
- [ ] Configurar reintentos controlados.
- [ ] Permitir pausar la automatización.

## Fase 9 — Seguimiento y métricas

- [ ] Registrar qué ideas fueron utilizadas.
- [ ] Registrar la fecha de publicación.
- [ ] Permitir cargar métricas manualmente.
- [ ] Registrar consultas y turnos asociados a publicaciones.
- [ ] Comparar formatos de contenido.
- [ ] Mostrar resultados básicos.
- [ ] Usar los resultados para orientar nuevas recomendaciones.

No afirmar que una publicación generó una reserva si no existe una forma razonable de relacionarlas.

## Fase 10 — Despliegue

- [ ] Comparar proveedores de alojamiento.
- [ ] Estimar costos mensuales.
- [ ] Configurar variables de entorno de producción.
- [ ] Configurar una base de datos persistente.
- [ ] Desplegar el backend.
- [ ] Publicar la web.
- [ ] Configurar HTTPS.
- [ ] Configurar webhooks públicos cuando corresponda.
- [ ] Verificar seguridad y persistencia.
- [ ] Probar reservas reales controladas.
- [ ] Comprobar el funcionamiento sin depender de la computadora local.
- [ ] Documentar mantenimiento y recuperación ante errores.

## Criterio general de finalización

Cada fase debe quedar implementada, probada y documentada antes de considerar que está terminada.

Las fases pueden ajustarse si aparecen limitaciones técnicas, costos inesperados o necesidades nuevas del negocio.

La prioridad es entregar un producto útil y confiable, evitando complejidad innecesaria.