# MEMORY.md — BarberTrend AI

## 1. Descripción

BarberTrend AI es un proyecto personal para desarrollar una solución digital para una barbería masculina de un familiar.

El objetivo es construir un producto útil para el negocio real y, potencialmente, convertirlo en un proyecto de portfolio o en un servicio adaptable a otras barberías.

## 2. Visión del producto

El proyecto combinará dos módulos.

### Sistema de reservas

- Web pública.
- Catálogo de servicios y precios.
- Duración de cada servicio.
- Selección de fechas y horarios disponibles.
- Reserva de turnos.
- Registro de datos básicos del cliente.
- Confirmaciones por WhatsApp.
- Cancelación y modificación de reservas.
- Panel administrativo privado.
- Gestión de horarios, servicios y turnos.

### Asistente de marketing con IA

- Detección de tendencias relacionadas con barberías masculinas.
- Referencias a videos y publicaciones reales.
- Generación de ideas para Reels e historias.
- Guiones breves para grabar.
- Captions y hashtags.
- Recomendaciones periódicas por WhatsApp.
- Historial de sugerencias.
- Registro de publicaciones y resultados.
- Ideas orientadas a generar consultas y reservas.

La detección de tendencias debe depender de fuentes accesibles y verificables. No asumir que todas las plataformas ofrecen APIs públicas para buscar contenido viral.

## 3. Estrategia de desarrollo

Desarrollar y probar primero en la computadora local.

Orden previsto:

1. Web pública.
2. Backend y reservas.
3. Panel administrativo.
4. Integración de WhatsApp.
5. Servicio de IA.
6. Fuentes de tendencias.
7. Automatización.
8. Despliegue y evaluación de costos.

No integrar todos los componentes desde el primer día.

## 4. Preferencias técnicas iniciales

- HTML, CSS y JavaScript para el frontend.
- Java y Spring Boot para el backend.
- PostgreSQL como opción para producción.
- SQLite u otra alternativa sencilla para pruebas, si resulta conveniente.
- API REST.
- WhatsApp Business Platform Cloud API oficial.
- Integración de IA desacoplada de un proveedor.
- Git para control de versiones.

Las tecnologías son decisiones iniciales, no requisitos inamovibles. Deben revisarse según la complejidad, los costos y las necesidades reales.

## 5. Restricciones económicas

- Priorizar pruebas locales.
- Evitar contratar hosting antes de tener una versión funcional.
- No depender de servicios pagos sin evaluar alternativas.
- Investigar los costos actuales de WhatsApp, IA, alojamiento y bases de datos antes del despliegue.
- No realizar pagos ni activar servicios pagos sin autorización.

## 6. Datos todavía pendientes

No se deben inventar los siguientes datos:

- Nombre comercial de la barbería.
- Dirección y ubicación exacta.
- Número de WhatsApp.
- Cuenta de Instagram.
- Servicios y precios reales.
- Duración de cada servicio.
- Días y horarios de atención.
- Tiempo de descanso entre turnos.
- Política de cancelaciones.
- Identidad visual y fotografías autorizadas.
- Configuración actual de WhatsApp Business.

## 7. Supuestos operativos por validar

- El negocio es una barbería masculina.
- El sistema debe ser cómodo de usar desde el celular.
- El dueño necesita consultar y gestionar su agenda.
- Los clientes deben poder reservar sin contactar primero al dueño.
- Las confirmaciones automáticas son una funcionalidad deseada.
- La agenda debe impedir las reservas duplicadas.
- Las recomendaciones de marketing deben ser concretas y fáciles de ejecutar.

No asumir todavía si trabaja un solo barbero o varios.

## 8. Criterios de éxito

El primer objetivo no es tener una plataforma compleja.

Se busca conseguir una versión que permita:

1. Consultar los servicios.
2. Ver horarios disponibles.
3. Reservar un turno correctamente.
4. Consultar la agenda desde un panel protegido.
5. Probar las notificaciones sin enviar mensajes reales.
6. Incorporar posteriormente confirmaciones por WhatsApp.
7. Generar ideas de contenido útiles para el negocio.

El valor del proyecto debe evaluarse según su utilidad para la barbería, no por la cantidad de tecnologías incorporadas.

## 9. Decisiones confirmadas (registro)

### 9.1 Acceso al panel del barbero (2026-10-23)

- Un solo usuario administrador, definido por variables de entorno
  (`ADMIN_USERNAME` + `ADMIN_PASSWORD_HASH`, o `ADMIN_PASSWORD` solo en
  desarrollo). No hay registro de usuarios.
- Autenticación con **cookie de sesión** y el form login de Spring Security.
  Se descartó JWT: para un panel único agrega complejidad sin beneficio.
- Página de login propia y estática en `/admin/login`; el panel en `/admin`.
  Ambos se sirven con URLs limpias mediante un *forward* interno. Si ya hay
  sesión iniciada, `/admin/login` redirige a `/admin`; el enlace del pie
  apunta directo a `/admin` para no mostrar el formulario de nuevo.
- **CSRF activado**. El token viaja en la cookie `XSRF-TOKEN` (legible por
  JavaScript) y en el campo `_csrf` del formulario.
- El cierre de sesión es `POST /admin/logout`; un GET no debería cerrar sesión.
- El sitio público no anuncia el panel, pero tiene un enlace discreto
  **Acceso del barbero** en el pie. La ruta sigue protegida igual.
- El panel ya no es solo la estructura: muestra los turnos, permite
  confirmarlos, completarlos o cancelarlos, y arma el WhatsApp manual. El resto
  de los datos de la barbería se agrega después.

### 9.2 Reservas reales y notificaciones (2026-10-23)

- **Reservas activadas.** El formulario público consulta la disponibilidad
  real (`GET /api/public/availability`) y crea el turno
  (`POST /api/public/appointments`). Antes la bandera
  `bookingApiEnabled` estaba en `false` y el formulario no enviaba nada.
- **Se corrigió un bug de concurrencia.** Con `saveAndFlush` sobre una entidad
  de clave compuesta asignada a mano, Spring Data usa `merge`, que ante una
  fila ya insertada por otro hilo la **actualiza** en vez de fallar. Se
  implementó `Persistable` en `AppointmentSlot` para forzar el `INSERT`. Las 2
  pruebas de concurrencia pasan.
- **Aviso al barbero: email automático + WhatsApp manual.** El usuario eligió
  esta combinación por ser **100% gratis**. Se descartó, por ahora, la API
  oficial de WhatsApp Business (requiere cuenta de Meta, número dedicado y
  plantillas aprobadas).
- El email se dispara **después del commit** de la reserva
  (`@TransactionalEventListener(AFTER_COMMIT)`) y nunca rompe la reserva. Sin
  credenciales, funciona en **modo dry-run**: el aviso queda en el log.
- El WhatsApp al cliente es un **enlace `wa.me`** con el mensaje ya armado
  (`WhatsAppLinkBuilder`), que normaliza el teléfono argentino. El barbero toca
  "enviar". No usa librerías no oficiales.
- Los datos del barbero (`BARBER_EMAIL`, `BARBER_WHATSAPP`) se leen del `.env`.
  Editable desde el panel queda para más adelante.
- **Deuda pendiente:** no hay *rate limiting* en el formulario público. Está
  anotado en el registro de deuda técnica.

### 9.3 Horarios y zona horaria (2026-11-06)

- Los horarios de la agenda se guardan como **hora local** de la barbería.
  Por eso `hibernate.jdbc.time_zone` usa `APP_TIMEZONE` y no `UTC`: con UTC,
  Hibernate interpretaba 09:00 como UTC y en Argentina (UTC−3) la mostraba
  como 06:00.
- Horario de atención actual: **lunes a sábado de 09:00 a 21:00**, domingo
  cerrado. Se aplica con la migración `V3__actualizar_horario_atencion.sql`.
- El cierre a las 21:00 es la hora de cierre del local: el último turno
  empieza antes según la duración del servicio. Por ejemplo, un corte de
  30 minutos puede empezar 20:30 y uno de 60 minutos, 20:00.

### 9.4 Cierres y bloqueos de agenda (2026-11-08)

- El panel permite cerrar **fechas puntuales**, **rangos de días seguidos** y
  **franjas horarias** de un mismo día. No se implementaron cierres
  recurrentes semanales: quedan para una etapa posterior.
- Se reutiliza la tabla `time_off`, que ya existía para vacaciones y feriados.
  Un cierre de día completo se guarda desde las 00:00 del primer día hasta las
  00:00 del día siguiente al último (final exclusivo). Un cierre parcial debe
  caer en bloques de 30 minutos, igual que la agenda.
- Antes de crear un cierre se cuentan los turnos `PENDING` y `CONFIRMED` que
  se superponen. Si hay alguno, el cierre se rechaza con
  `BLOCK_HAS_APPOINTMENTS`; **no se cancelan turnos automáticamente** y no se
  envía ningún aviso al cliente. El barbero debe cancelarlos primero.
- Tampoco se permiten dos cierres superpuestos (`BLOCK_OVERLAP`). Un cierre
  terminado en el pasado se rechaza (`BLOCK_IN_PAST`).
- La disponibilidad pública marca un día con cierre completo como
  `"closed": true`; un cierre parcial solo oculta los horarios afectados.
- La reserva directa por API también consulta `time_off` y responde
  `422 BLOCKED_TIME` si el horario está bloqueado. No alcanza con ocultar los
  horarios en el navegador.
- API nueva: `GET /api/admin/blocks`, `POST /api/admin/blocks` y
  `DELETE /api/admin/blocks/{id}`. Quedan protegidos por la regla existente de
  `/api/admin/**` y por CSRF.
- La suite quedó en **40 pruebas, 0 fallos**, incluyendo cierres completos,
  parciales, superposición, turnos existentes, reserva directa bloqueada,
  eliminación y control de acceso.

### 9.5 Zona horaria del JVM y CI (2026-11-08)

- GitHub Actions ejecuta las pruebas en un JVM con zona **UTC**. Hibernate usa
  `hibernate.jdbc.time_zone=APP_TIMEZONE`, pero las columnas `TIME` se
  convierten con la zona por defecto del JVM: con UTC, `09:00` se leía como
  `12:00` y fallaban 9 pruebas. En Windows no se veía porque el JVM ya estaba
  en hora argentina.
- Se agregó `TimeZoneEnvironmentPostProcessor`, registrado en
  `META-INF/spring.factories`. Fija `TimeZone.setDefault(APP_TIMEZONE)` después
  de cargar `.env`/`application.yml` y antes de crear el contexto de Spring.
- Esto también corrige el despliegue en cualquier servidor Linux que use UTC;
  no era solo un problema del CI.
- Verificado: **40 pruebas, 0 fallos** en Linux sin `.env` y en Windows.

## 10. Mantenimiento de este archivo

Actualizar este documento cuando se tomen decisiones importantes.

No marcar como implementada una funcionalidad solo porque fue planificada.

Distinguir entre decisiones confirmadas, propuestas y cuestiones pendientes.

Las instrucciones de desarrollo están en AGENTS.md. El inventario y estado de las funcionalidades están en FEATURES.md.