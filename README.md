# BarberTrend AI

Sistema de reservas online para una barbería masculina, con un asistente de
marketing basado en IA como segunda etapa.

**Estado actual: Etapa 3 cerrada.** La web pública está construida, el
formulario **crea reservas reales** contra la API y el panel del barbero ya
muestra los turnos: se pueden confirmar, completar o cancelar. También permite
**cerrar días completos o franjas horarias** por vacaciones, feriados o
descansos, y **administrar el catálogo de servicios y el horario semanal** sin
tocar archivos: esos datos viven en la base y el sitio público los carga desde
la API. Cuando entra un turno nuevo, el barbero recibe un **aviso por email**;
al confirmarlo, el panel le da un enlace de **WhatsApp manual** para avisarle
al cliente (gratis, sin API de Meta). La automatización oficial de WhatsApp
queda para la Etapa 4.

**Doble S Barbería** — nombre, logo, contactos, Instagram, precios de los
4 servicios y horario ya son datos reales y confirmados. Lo único que sigue
siendo de ejemplo son los textos de la portada. Ver
[`docs/datos-pendientes.md`](docs/datos-pendientes.md).

---

## Documentación

| Documento | Qué contiene |
|---|---|
| [`docs/arquitectura.md`](docs/arquitectura.md) | Decisiones técnicas, con los motivos y las fuentes |
| [`docs/modelo-de-datos.md`](docs/modelo-de-datos.md) | Tablas, reglas de negocio y estados |
| [`docs/api.md`](docs/api.md) | Contrato de los endpoints REST |
| [`docs/datos-pendientes.md`](docs/datos-pendientes.md) | Qué datos del negocio falta completar |
| [`FEATURES.md`](FEATURES.md) | Inventario de funcionalidades y su estado real |
| [`Documentacion-BarberTrend.pdf`](Documentacion-BarberTrend.pdf) | **Resumen completo del proyecto**: lo hecho y la hoja de ruta, en PDF (fuente editable: [`docs/documentacion.html`](docs/documentacion.html)) |

---

## Stack

| Componente | Versión |
|---|---|
| Java | 21 LTS |
| Spring Boot | 4.1.1 |
| Spring Framework | 7.0.9+ |
| Maven | 3.9.16 |
| Base de datos | H2 (desarrollo/pruebas) · PostgreSQL 18 (producción) |
| Migraciones | Flyway |
| Frontend | HTML, CSS y JavaScript sin dependencias |
| Seguridad | Spring Security, sesiones, BCrypt |

Decisiones justificadas en [`docs/arquitectura.md`](docs/arquitectura.md#2-decisiones-de-versión).

---

## Estructura

```
BarberTrend/
├── docs/                      documentación
├── backend/
│   └── src/
│       ├── main/
│       │   ├── java/com/barbertrend/
│       │   │   ├── config/     configuración y seguridad
│       │   │   ├── domain/     entidades
│       │   │   ├── dto/        records de la API
│       │   │   ├── repository/ acceso a datos
│       │   │   ├── service/    lógica de negocio
│       │   │   ├── web/        controladores REST
│       │   │   └── common/     manejo de errores
│       │   └── resources/
│       │       ├── application*.yml
│       │       ├── db/migration/   migraciones Flyway
│       │       └── static/          web pública y panel
│       └── test/
├── .env.example
└── .gitignore
```

---

## Cómo ejecutarlo

### Requisitos

- **JDK 21** ([Adoptium Temurin](https://adoptium.net/temurin/releases/?version=21))
- Maven 3.9 o superior — o usar el wrapper `./mvnw`, que lo descarga solo

### Pasos

```bash
# 1) Configurar (copiar el ejemplo dentro de backend y completar)
cp .env.example backend/.env

# 2) Arrancar
cd backend
./mvnw spring-boot:run
```

> En Windows, usá `mvnw.cmd` en lugar de `./mvnw`.

| Dirección | Qué hay |
|---|---|
| <http://localhost:8080> | Sitio público |
| <http://localhost:8080/admin/login> | Ingreso al panel del barbero |
| <http://localhost:8080/admin> | Panel del barbero (pide sesión) |

Con el perfil `dev` **no hace falta instalar una base de datos**: usa H2 en
archivo.

### Ejecutar las pruebas

```bash
cd backend
./mvnw test
```

Estado actual: pasan **las 57 pruebas**. Incluyen concurrencia de reservas,
el circuito completo de una reserva (disponibilidad, alta, rechazo del horario
duplicado y confirmación desde el panel), cierres de agenda, administración de
servicios y horarios, el límite de intentos, el login y los avisos.

La interfaz de **Servicios** y **Horario de atención** también se verificó en
un navegador headless: crear, editar, desactivar y volver a mostrar un servicio,
cambiar el horario de un día y cerrar otro. La prueba manual no reemplaza a la
suite; sirve para comprobar que el HTML y el JavaScript del panel funcionan
juntos.

### Verificar en perfiles

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=test   # H2 en memoria
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod   # PostgreSQL
```

---

## Seguridad

- Las credenciales **nunca** van en el código. Están en `.env`, que está en
  `.gitignore`.
- `.env.example` contiene **solo valores de ejemplo**, rotulados como tales.
- La contraseña del administrador se guarda como **hash BCrypt**.
- El panel (`/admin`) y la API privada (`/api/admin/**`) exigen sesión.
  Sin sesión: las páginas redirigen a `/admin/login` y la API responde `401`.
- El login está en `/admin/login`. La sesión vive en una cookie `HttpOnly`
  y se cierra con `POST /admin/logout` (no con un enlace GET).
- CSRF está activado: el formulario del panel envía el token `_csrf`, que lee
  de la cookie `XSRF-TOKEN`.
- Los teléfonos de los clientes se enmascaran en los logs.
- El formulario público limita los intentos de reserva por IP (5 cada 10
  minutos por defecto). Al superarlo responde `429` con `Retry-After`. El
  conteo es en memoria: con varias instancias habría que compartirlo.

Si en algún momento se sube un `.env` por error a Git, **no alcanza con
borrarlo**: hay que rotar las credenciales, porque siguen en el historial.

---

## Dónde cambiar los datos de la barbería

Hay dos lugares, según el tipo de dato:

### Servicios y horario de atención

Se administran desde el panel, en las secciones **Servicios** y
**Horario de atención**:

```
http://localhost:8080/admin
```

Los cambios se guardan en la base de datos y el sitio público los carga desde
la API. No hace falta reiniciar la aplicación ni editar archivos.

- **Servicios:** nombre, descripción, precio, duración, orden y si está activo.
  Desactivar un servicio lo saca de la web, pero conserva los turnos ya
  reservados.
- **Horario:** apertura y cierre de cada día, o el día marcado como cerrado.
  Las horas tienen que caer en bloques de 30 minutos.

### Marca, contacto y galería

Los textos de portada, el logo, los contactos y las fotos siguen en:

```
backend/src/main/resources/static/assets/js/site-config.js
```

Ese archivo también conserva una **lista de respaldo** de servicios y horarios:
se usa solo si la API no responde, para que el sitio no quede vacío.

El checklist de lo que falta está en
[`docs/datos-pendientes.md`](docs/datos-pendientes.md).

---

## Ver la web sin instalar nada

El sitio es estático y no usa módulos ES, así que también podés abrirlo
directo en el navegador con doble clic:

```
backend/src/main/resources/static/index.html
```

La forma recomendada es levantar el servidor, porque ahí las rutas relativas
y los enlaces funcionan igual que en producción.

---

## Qué funciona y qué no todavía

| Parte | Estado |
|---|---|
| Diseño responsive (móvil, tablet, computadora) | ✅ Listo |
| Servicios, galería, horarios y contacto | ✅ Listo, con datos reales |
| Servicios y horario cargados desde la base | ✅ Listo, con respaldo si la API falla |
| Formulario de reserva con sus estados | ✅ Listo: crea reservas reales |
| Login y acceso al panel del barbero | ✅ Listo y probado |
| Backend de reservas (API) | ✅ Listo, con garantía anti-superposición |
| Límite de intentos de reserva por IP | ✅ Listo (5 cada 10 minutos, configurable) |
| Panel: listar, confirmar, completar y cancelar turnos | ✅ Listo |
| Panel: cerrar días completos o franjas horarias | ✅ Listo |
| Panel: crear, editar y desactivar servicios | ✅ Listo |
| Panel: editar el horario semanal | ✅ Listo |
| Aviso por email al barbero | ✅ Listo (modo dry-run por defecto) |
| WhatsApp al cliente | 🟡 Manual con enlace `wa.me`; la API oficial es Etapa 4 |
| Asistente de marketing con IA | ❌ Etapa 5 |

### Aviso de turno nuevo por email

Cuando un cliente reserva, el backend avisa al barbero por email **siempre
después de que la reserva quedó guardada**. Un problema de correo no rompe el
turno.

Por defecto **no se envía nada**: `MAIL_ENABLED=false` deja el sistema en
modo *dry-run* y el aviso aparece en el log. Para enviar de verdad, se usa el
SMTP gratuito de Gmail:

1. Activá la verificación en dos pasos de tu cuenta de Google.
2. Creá una **contraseña de aplicación** (Cuenta de Google → Seguridad).
3. En `backend/.env`, completá `MAIL_USERNAME`, `MAIL_PASSWORD` (la de 16
   caracteres), `MAIL_FROM` y `BARBER_EMAIL`.
4. Poné `MAIL_ENABLED=true` y reiniciá la aplicación.

No hace falta crear ninguna cuenta nueva ni pagar nada. Si preferís otro
proveedor, alcanza con cambiar `MAIL_HOST` y `MAIL_PORT`.

### WhatsApp manual (gratis)

En esta etapa el mensaje al cliente se manda **a mano** para no depender de la
cuenta de Meta ni de plantillas aprobadas:

1. El barbero entra a `/admin` y ve los turnos pendientes.
2. Toca **Confirmar**.
3. Toca **WhatsApp**: se abre una conversación con el cliente y el mensaje de
   confirmación ya escrito. Solo hay que tocar "enviar".

El enlace se arma en el backend (`WhatsAppLinkBuilder`), que normaliza el
teléfono argentino al formato internacional. Cuando el proyecto justifique la
API oficial de WhatsApp Business, el enlace manual se reemplaza sin tocar el
resto del panel.

### Cierres y bloqueos

Desde la sección **Cierres y bloqueos** del panel se puede cerrar:

- un día completo (opcionalmente, varios días seguidos);
- una franja de horas de un mismo día, en bloques de 30 minutos.

Los cierres ocultan esos horarios en la web pública y también se validan en el
backend, así que una petición directa a la API no puede reservar dentro de un
horario bloqueado. **No se cancelan turnos automáticamente**: si ya hay turnos
pendientes o confirmados en el rango, el sistema rechaza el cierre e indica
cuántos hay. Primero se cancelan esos turnos y después se crea el cierre.

### Servicios y horario desde el panel

La sección **Servicios** permite crear, editar, ordenar, activar y desactivar
servicios. Un servicio desactivado deja de aparecer en la web, pero **no se
borra**: los turnos que ya lo tenían reservado conservan su historial.

La sección **Horario de atención** muestra los siete días de la semana. Para
cada uno se puede cambiar la apertura y el cierre, o marcarlo como cerrado. Las
horas deben caer en bloques de 30 minutos. El cambio se refleja de inmediato en
la disponibilidad y en la tabla de horarios del sitio público.

> Cambiar el horario semanal **no cancela turnos ya reservados**. Si un día
> queda fuera del nuevo horario, esos turnos siguen en la agenda: revisalos y
> cancelalos manualmente si hace falta.

### Acceso al panel

1. Abrir <http://localhost:8080/admin/login>. También hay un enlace discreto
   **Acceso del barbero** en el pie del sitio público. Si la sesión ya está
   abierta, tanto el enlace como la página de login llevan directo al panel.
2. Ingresar con el usuario y la contraseña definidos en `backend/.env`
   (`ADMIN_USERNAME` + `ADMIN_PASSWORD_HASH`, o `ADMIN_PASSWORD` en desarrollo).
3. Al entrar se llega a `/admin`. El botón **Salir** cierra la sesión.

---

## Estado por etapas

| Etapa | Contenido | Estado |
|---|---|---|
| 1 | Análisis, arquitectura, modelo de datos | ✅ Hecho |
| 2 | Web pública responsive | ✅ Hecho |
| 3 | Backend, reservas, panel, seguridad | ✅ Hecho (reservas, panel, servicios, horario y aviso por email) |
| 4 | Integración con WhatsApp | 🟡 Hecho el envío manual; falta la API oficial |
| 5 | Asistente de marketing | ⬜ Pendiente |
| 6 | Automatización y despliegue | ⬜ Pendiente |

---

## Licencia

Proyecto académico. Sin licencia definida todavía.
