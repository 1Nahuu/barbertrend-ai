# FEATURES.md — Inventario de funcionalidades

Estado real de BarberTrend AI, actualizado al **2026-11-11**.

**Leyenda:** ✅ implementado y probado · 🟡 parcial o con limitaciones · ⬜ pendiente

---

## 1. Sitio público

| Funcionalidad | Estado | Detalle |
|---|---|---|
| Diseño responsive | ✅ | Móvil, tablet y computadora |
| Datos de la barbería centralizados | ✅ | Marca, contacto y galería en `site-config.js`; servicios y horario en la base |
| Servicios y precios | ✅ | Desde la base; se editan en el panel |
| Galería de fotos | ✅ | 4 imágenes en `static/assets/img/` |
| Horarios de atención | ✅ | Desde la base; se editan en el panel |
| Contacto y redes | ✅ | WhatsApp e Instagram reales |
| Botón flotante de WhatsApp | ✅ | Enlace `wa.me` |
| Navegación por secciones | ✅ | Sin dependencias de JavaScript externas |
| Aviso de sitio en construcción | ✅ | Se activa con `showSampleDataNotice`; hoy avisa que los textos son provisionales |
| Accesibilidad y contraste | 🟡 | Revisión básica; falta auditoría formal |

---

## 2. Reservas

| Funcionalidad | Estado | Detalle |
|---|---|---|
| Consulta de disponibilidad real | ✅ | `GET /api/public/availability` |
| Alta de turno | ✅ | `POST /api/public/appointments` |
| Validación en el backend | ✅ | No depende solo del navegador |
| Prevención de turnos duplicados | ✅ | Clave compuesta `(barber_id, slot_start)` + `Persistable` |
| Concurrencia | ✅ | 2 pruebas específicas de solicitudes simultáneas |
| Duración por servicio | ✅ | Bloques de 30 minutos |
| Anticipación mínima | ✅ | 120 minutos por defecto |
| Horizonte máximo | ✅ | 60 días por defecto |
| Validación de horarios de atención | ✅ | Fuera de horario se rechaza |
| Validación de cierres | ✅ | Respuesta `422 BLOCKED_TIME` |
| Código de turno | ✅ | Formato `BT-XXXXXX`, no secuencial |
| Datos básicos del cliente | ✅ | Nombre y teléfono |
| Límite de intentos (rate limiting) | ✅ | 5 intentos por IP cada 10 min; `429` con `Retry-After` |
| Cancelación o cambio por el cliente | ⬜ | Hoy se gestiona desde el panel |
| Política de cancelación publicada | ⬜ | Falta definir el dato |

---

## 3. Panel del barbero

| Funcionalidad | Estado | Detalle |
|---|---|---|
| Login con usuario y contraseña | ✅ | Spring Security, BCrypt |
| Sesión con cookie `HttpOnly` | ✅ | Cierre con `POST /admin/logout` |
| Protección CSRF | ✅ | Token en cookie y formulario |
| Protección de páginas y API | ✅ | `/admin` y `/api/admin/**` exigen sesión |
| Listado de turnos | ✅ | Pendientes, confirmados, completados y cancelados |
| Confirmar, completar y cancelar | ✅ | Con validación de estados |
| Cerrar días completos o rangos | ✅ | Con detección de turnos superpuestos |
| Cerrar franjas horarias | ✅ | Bloques de 30 minutos |
| Eliminar cierres | ✅ | `DELETE /api/admin/blocks/{id}` |
| Gestión de servicios desde el panel | ✅ | Crear, editar, ordenar, activar y desactivar |
| Gestión de horarios desde el panel | ✅ | Editar apertura/cierre y cerrar días |
| Edición de datos del negocio | ⬜ | Marca, contacto y galería se editan en `site-config.js` |

---

## 4. Notificaciones

| Funcionalidad | Estado | Detalle |
|---|---|---|
| Aviso por email al barbero | ✅ | Después del commit; nunca rompe la reserva |
| Modo dry-run de email | ✅ | `MAIL_ENABLED=false` escribe en el log |
| WhatsApp manual al cliente | ✅ | Enlace `wa.me` con mensaje armado |
| Normalización de teléfono argentino | ✅ | `WhatsAppLinkBuilder` |
| API oficial de WhatsApp Business | ⬜ | Etapa 4; requiere cuenta de Meta y plantillas |
| Recordatorios automáticos | ⬜ | Etapa 6 |
| Confirmación automática al cliente | ⬜ | Etapa 4 |

---

## 5. Asistente de marketing con IA

| Funcionalidad | Estado | Detalle |
|---|---|---|
| Detección de tendencias | ⬜ | Etapa 5 |
| Ideas para Reels e historias | ⬜ | Etapa 5 |
| Guiones, captions y hashtags | ⬜ | Etapa 5 |
| Historial de sugerencias | ⬜ | Etapa 5 |
| Recomendaciones por WhatsApp | ⬜ | Etapa 6 |
| Integración desacoplada del proveedor | ⬜ | Variables `AI_PROVIDER`, `AI_API_KEY`, `AI_MODEL` reservadas |

> El video de demostración de este módulo se grabará cuando la funcionalidad
> exista. No se va a presentar una maqueta como si estuviera implementada.

---

## 6. Calidad, seguridad y operación

| Funcionalidad | Estado | Detalle |
|---|---|---|
| Pruebas automatizadas | ✅ | **57 pruebas, 0 fallos** (verificado 2026-11-11) |
| Integración continua | ✅ | GitHub Actions ejecuta `./mvnw test` |
| Validación de datos en el servidor | ✅ | Bean Validation + reglas de negocio |
| Credenciales fuera del repositorio | ✅ | `.env` ignorado; `.env.example` sin datos reales |
| Contraseña de administrador con hash | ✅ | BCrypt |
| Teléfonos enmascarados en logs | ✅ | `22******45` |
| Migraciones de base de datos | ✅ | Flyway (`V1` a `V3`) |
| Perfiles dev / test / prod | ✅ | H2 en memoria o archivo; PostgreSQL en prod |
| Despliegue | ⬜ | Etapa 6 |
| Dominio y hosting | ⬜ | Etapa 6 |
| Licencia | ⬜ | Sin definir |

---

## 7. Deuda técnica conocida

- El *rate limiting* de reservas es en memoria y por instancia; el login no tiene límite de intentos.
- Los cierres recurrentes semanales no están implementados (solo fechas y franjas).
- Cambiar el horario semanal no cancela turnos ya reservados; el panel lo advierte y las reservas nuevas sí se validan contra el horario.
- Los datos de marca, contacto y galería se editan en `site-config.js`, no desde el panel.
- La API oficial de WhatsApp y la integración con IA están pendientes.
- No hay auditoría de accesibilidad ni pruebas end-to-end en navegador.

---

## 8. Decisiones descartadas

| Funcionalidad | Motivo |
|---|---|
| Extender turnos desde el panel (2026-11-08) | Si un corte se pasa de hora, se resuelve en el local. No forma parte de la hoja de ruta. |
