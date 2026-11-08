# Arquitectura — BarberTrend AI

Documento de referencia de la Etapa 1 (análisis y planificación).
Cada decisión de acá está registrada con el motivo por el que se tomó.

---

## 1. Resumen

Un **monolito modular**: una sola aplicación Spring Boot que expone la API REST
y además sirve el sitio web público y el panel del dueño como archivos
estáticos. Sin microservicios, sin Node, sin paso de build de frontend.

```
                    Navegador
                        │
        ┌───────────────┴────────────────┐
        │                                │
   /            (web pública)     /admin/  (panel del dueño)
        │                                │
        └───────────────┬────────────────┘
                        │  mismo origen, sin CORS
        ┌───────────────▼────────────────┐
        │        Spring Boot 4.1.1        │
        │                                 │
        │   web/ ──► service/ ──► repository/   ◄── domain/  (entidades)
        │     │         │             │           dto/      (records)
        │   common/  config/                      common/   (errores)
        └───────────────┬───────────────────────────────┘
                        │
          ┌─────────────┴─────────────┐
          │                           │
   H2 (dev/test)              PostgreSQL (prod)
          └─────────────┬─────────────┘
                        │
                    Flyway
              (migraciones .sql)
```

---

## 2. Decisiones de versión

Todas verificadas contra fuentes oficiales el 18/10/2026, no de memoria.

| Componente | Versión | Fuente de la verificación |
|---|---|---|
| Spring Boot | **4.1.1** | `docs.spring.io/spring-boot/system-requirements.html` |
| Spring Framework | 7.0.9 o superior | requisito declarado por Boot 4.1.1 |
| Spring Security | 7.1.1 | resuelto por el BOM de Boot 4.1.1 |
| Java | **21 LTS** (Temurin 21.0.12.1) | `api.adoptium.net/v3/info/available_releases` |
| Maven | **3.9.16** | última versión de la línea 3.9 en `dlcdn.apache.org` |
| Jakarta EE / Servlet | 11 / 6.1 | Boot 4.1.1 embebido con Tomcat 11 |
| PostgreSQL | **18** (o 17) | `endoflife.date`: 18 → EOL 2030-11-14 |
| H2 | la gestionada por Boot 4.1.1 | BOM de Spring Boot |

### 2.1 Versiones descartadas y por qué

| Versión tempting | Motivo del descarte |
|---|---|
| Spring Boot 3.5.16 | Su soporte oficial (OSS) **terminó el 2026-06-30**. Empezar un proyecto nuevo en una línea sin soporte es una deuda innecesaria. |
| Spring Boot 4.0.8 | Fin de soporte el **2026-12-31**, dentro de dos meses. |
| Spring Boot 4.2.0-M2 | Es un *milestone*, no una versión estable. |
| Java 27 | Es la más reciente, pero **Boot 4.1.1 soporta Java 17–26 solamente**. Java 27 no es una opción válida acá. |
| Java 17 | Funciona (es el mínimo), pero Java 21 agrega *records*, *pattern matching* y *sealed classes*, que se usan en el código. |
| Java 25 LTS | **Sí es válido** y está soportado. Se elige 21 sólo por máxima compatibilidad con IDEs. Si ya tenés 25 instalado, no cambia nada. |

### 2.2 Advertencia: Spring Boot 4 renombró los starters

Descubierto al generar un proyecto real con `start.spring.io`, no por suposición:

| En Boot 3.x | En Boot 4.x |
|---|---|
| `spring-boot-starter-web` | **`spring-boot-starter-webmvc`** |
| `spring-boot-starter-test` | starters de test por módulo: `-webmvc-test`, `-data-jpa-test`, `-security-test`, ... |

Comprobado en Maven Central: **ambos nombres antiguos siguen existiendo** en
4.1.1, así que no es una eliminación, es un renombre. Pero la enorme mayoría
del material en internet está escrito para 3.x. Cuando busques soluciones,
verificá que el artículo sea de la versión 4.

### 2.3 Prueba de humo ejecutada

Se generó un proyecto con Initializr (Boot 4.1.1, Java 21, con Web + JPA +
Security + Validation + Flyway + H2 + PostgreSQL + Actuator) y se ejecutó
`mvn test`:

```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

El stack propuesto **compila y arranca**. No es una recomendación teórica.

---

## 3. Por qué el frontend vive dentro de Spring Boot

El sitio se sirve desde `backend/src/main/resources/static/`.

| Opción | A favor | En contra | Decisión |
|---|---|---|---|
| HTML/CSS/JS servido por Spring Boot | un solo comando, mismo origen, cero `npm`, cero build | el frontend no se despliega por separado | **Elegida** |
| Frontend separado con Node/Vite | herramientas modernas, HMR | agrega Node, npm, bundler y CI | Descartada |
| React | componentes reutilizables | el pedido fue explícito de no agregarlo | Descartada |

Para una barbería con un único servidor, servir todo desde el mismo proceso es
la opción simple y sin sorpresas. Si algún día el frontend crece tanto que
necesita su propio despliegue, la separación es un cambio acotado: mover los
archivos de `static/` y configurar CORS.

---

## 4. Capas y responsabilidad

El flujo de una petición es siempre el mismo, y cada capa tiene **una sola**
responsabilidad:

| Capa | Carpeta | Responsabilidad | Lo que **no** hace |
|---|---|---|---|
| Controlador | `web/` | HTTP: valida el formato, responde códigos | Nada de lógica de negocio |
| Servicio | `service/` | Toda la lógica: disponibilidad, reserva, agenda | Nada de HTTP |
| Repositorio | `repository/` | Acceso a datos | Nada de reglas |
| Entidad | `domain/` | Datos y comportamiento propio | Nada de anotaciones de persistencia ajenas |
| DTO | `dto/` | `record`s de entrada y salida | — |

**Regla clave:** las entidades JPA **nunca** se devuelven directamente en la
API. Siempre se convierten a `record`s de `dto/`. Así se puede cambiar el
modelo de datos sin romper a los clientes de la API.

---

## 5. La agenda: cómo se calculan los horarios

### 5.1 La grilla

La jornada se divide en bloques fijos de **30 minutos** (`APP_SLOT_MINUTES`).

- Cada servicio ocupa varios bloques: un corte de 45 minutos ocupa **2 bloques**
  (60 minutos de silla).
- La duración de un servicio se redondea **hacia arriba** al múltiplo de 30
  más cercano. Si el barbero corta desde las 10:00 hasta las 10:45, la silla
  queda libre recién a las 11:00.

### 5.2 Cómo se determina la disponibilidad

Para una fecha pedida, el sistema arma la lista así:

1. Se busca el horario de atención de ese día de la semana (`business_hours`).
   Si está cerrado, no hay horarios: se responde con una lista vacía.
2. Se genera la grilla de bloques desde la apertura hasta el cierre.
3. Se descartan los bloques que:
   - están en el pasado,
   - están dentro del período de anticipación mínima,
   - están fuera del horizonte máximo de reserva,
   - están ocupados (tabla `appointment_slot`),
   - están bloqueados por vacaciones, feriados o cierres del panel
     (`time_off`),
   - **no alcanzan** para que el servicio completo termine antes del cierre.
4. Lo que sobra es la respuesta.

Si un cierre de `time_off` cubre el día completo, la respuesta usa
`"closed": true` y no arma la grilla. Un cierre parcial solo descarta los
bloques que toca.

El mismo cálculo se usa para mostrar los horarios y para crear la reserva.
Además, `BookingService` vuelve a consultar `time_off` antes de guardar un
turno: si el horario está bloqueado, responde `422 BLOCKED_TIME` aunque la
petición se haga directo a la API. Si un horario aparece en pantalla pero al
reservar da conflicto, es porque alguien lo tomó en el medio — y en ese caso
se responde **409** con un mensaje claro, no un error genérico.

### 5.3 Zonas horarias

Todos los horarios se calculan en la zona configurada en `APP_TIMEZONE`
(por defecto `America/Argentina/Buenos_Aires`) usando la clase `java.time`.
Las fechas y horas se guardan como `TIMESTAMP` en la base, que representa un
momento local de la barbería.

> Decisión consciente: guardar `TIMESTAMP WITH TIME ZONE` sería más elegante
> en el papel, pero complica los cálculos de "el bloque de las 10:00" y las
> comparaciones entre H2 y PostgreSQL. Para un sistema de una única sede con
> una única zona horaria, el costo no compensa.

---

## 6. Reservas duplicadas: el problema y la solución

### 6.1 Por qué el enfoque obvio no alcanza

El código "pregunto si está libre y, si está, lo grabo" **tiene una condición
de carrera**. Entre el `SELECT` y el `INSERT` entra la otra petición, que
también ve el horario libre, y las dos graban. No es un caso raro: dos personas
reservando el mismo horario a la vez es justamente lo que va a pasar en la
práctica.

### 6.2 Por qué un `UNIQUE` tampoco alcanza

La restricción obvia sería `UNIQUE(barber_id, starts_at)`, pero **no cubre el
caso real**:

```
Barbero reserva   10:00 → 11:00   (corte de 60 min)
Otro cliente      10:30 → 11:30   (corte de 60 min)

starts_at es 10:00 en un caso y 10:30 en el otro
→ el UNIQUE no se activa
→ los dos turnos se pisan y hay dos personas en la misma silla
```

### 6.3 La solución: tabla de slots ocupados

Cada reserva ocupa bloques de la grilla, y se inserta **una fila por cada bloque
que ocupa**:

```
┌──────────────────────────────┬─────────────┐
│ barber_id (PK)               │ BIGINT      │
│ slot_start (PK)              │ TIMESTAMP   │
│ appointment_id (FK)          │ BIGINT      │
└──────────────────────────────┴─────────────┘
```

Ejemplo: un corte de 60 minutos reservado a las 10:00 inserta
`(barber, 10:00)` y `(barber, 10:30)`.

Ahora bien, si alguien intenta reservar las 10:30, el `INSERT` de esa fila
**choca con la clave primaria** y la base de datos rechaza la operación. La
aplicación traduce ese error a un **409 Conflict** con un mensaje legible.

**Por qué una tabla y no un índice parcial de PostgreSQL.** PostgreSQL permite
`CREATE UNIQUE INDEX ... WHERE status = 'PENDING'`, que parece más elegante.
Pero **H2 no soporta índices parciales**, así que el desarrollo local y las
pruebas automáticas se comportarían distinto de la producción. Con la tabla de
slots, la lógica es **exactamente la misma en H2 y en PostgreSQL**: la garantía
la da la base de datos, no el código de aplicación.

### 6.4 Capas de defensa

La garantía no depende de una sola cosa:

| Capa | Qué evita |
|---|---|
| Clave primaria compuesta de `appointment_slot` | Dos reservas sobre los mismos bloques, incluso simultáneas |
| `@Transactional` | Que quede un turno guardado sin sus slots, o al revés |
| Botón de reservar deshabilitado | Doble clic del usuario en el navegador |
| Mensaje 409 explícito | Que el usuario piense que falló la web |

### 6.5 Cómo se probó

Un test de concurrencia (`ConcurrentBookingTest`) lanza 20 peticiones **al
mismo tiempo** sobre el mismo horario y verifica que **exactamente una**
termina bien y las demás chocan con la base. Hay un segundo caso con servicios
que se superponen parcialmente (10:00–11:00 contra 10:30–11:30). Las dos
pruebas pasan.

### 6.6 Un bug real que apareció acá: `merge` en vez de `insert`

La primera versión del código usaba `slotRepository.saveAndFlush(...)` para
insertar los bloques. Las pruebas de concurrencia fallaban: en lugar de una
sola reserva exitosa, quedaban muchas.

La causa: con una **clave compuesta asignada a mano**, Spring Data no ve la
entidad como "nueva" (la clave no es nula) y usa `merge`. El `merge` primero
hace un `SELECT`; si el bloque no existe, inserta, pero si otro hilo ya lo
insertó, **actualiza la fila existente**. Resultado: el perdedor se quedaba con
el bloque del ganador en vez de recibir un error.

La solución es implementar `Persistable<AppointmentSlotId>` y marcar
explícitamente la entidad como nueva. Así Spring Data usa `persist` y el
`INSERT` choca con la clave primaria, que es lo que necesitamos. Queda
documentado en `AppointmentSlot` y cubierto por las pruebas de concurrencia.

---

## 7. Seguridad

| Tema | Decisión | Motivo |
|---|---|---|
| Autenticación | Cookie de sesión + form login propio en `/admin/login` | Un panel, un usuario. Un JWT sería complejidad sin beneficio. |
| Contraseña | BCrypt, leída de variable de entorno | Nunca en texto plano ni en el código. |
| Variables | `.env` + `.gitignore` desde el primer commit | Los secretos no entran al repositorio. |
| CSRF | Activado, con token en cookie `XSRF-TOKEN` y campo `_csrf` | La página de login es estática y el token lo necesita JavaScript. |
| Autorización | `/admin/**` y `/api/admin/**` exigen sesión | Separación total entre público y privado. |
| Sin sesión | Páginas → redirect a `/admin/login`; API → `401` | Cada cliente recibe lo que puede entender. |
| Cierre de sesión | `POST /admin/logout` + token CSRF | Un GET podría dispararse sin querer desde otro sitio. |
| Cabeceras | CSP, `X-Content-Type-Options`, `X-Frame-Options` | Endurecimiento básico del navegador. |
| Datos personales | Teléfono enmascarado en los logs | No se filtran datos de clientes a la consola. |
| Datos del cliente | Se guardan solo los imprescindibles | Principle of least data. |

### 7.1 Cómo se lee el `.env` sin agregar dependencias

Spring Boot puede importar archivos de propiedades de forma nativa:

```yaml
spring:
  config:
    import: "optional:file:.env[.properties]"
```

Por eso el `.env` usa formato `CLAVE=valor`. No hace falta la librería `dotenv`.
Si el archivo no existe, `optional:` hace que la aplicación arranque igual.
La ruta es relativa a la carpeta desde donde se ejecuta la aplicación, así que
el archivo va en `backend/.env` (no en la raíz del repositorio).

### 7.2 Contraseña del administrador

La variable preferida es `ADMIN_PASSWORD_HASH` (un hash BCrypt).

> El valor incluido en `.env.example` corresponde a la contraseña de ejemplo
> `cambiar-esto`. **Es un placeholder, no un secreto**, y está pensado para que
> la app funcione al instante en desarrollo.

Existe también `ADMIN_PASSWORD` en texto plano **solo para desarrollo**: si se
usa, la aplicación escribe un aviso en el log al arrancar. En producción hay que
usar el hash.

Para generar un hash propio se agregará en la Etapa 3 la utilidad:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--app.hash-password=miClave
```

### 7.3 Login del panel del barbero

**Flujo:**

```
GET  /admin/login   → página estática (pública)
                      └── si ya hay sesión → 302 a /admin
POST /login         → Spring Security valida usuario + contraseña + _csrf
                      ├── correcto → 302 a /admin
                      └── incorrecto → 302 a /admin/login?error
GET  /admin         → requiere sesión; si falta → 302 a /admin/login
POST /admin/logout  → cierra la sesión → 302 a /admin/login?logout
```

**Piezas:**

| Archivo | Para qué |
|---|---|
| `SecurityConfig` | Rutas públicas/privadas, form login, logout, CSRF y entry points |
| `WebConfig` | Reenvía `/admin` al `index.html` estático |
| `AdminPageController` | Sirve `/admin/login` y redirige al panel si ya hay sesión |
| `CsrfCookieFilter` | Fuerza la escritura de la cookie `XSRF-TOKEN` |
| `static/admin/login.html` | Formulario de ingreso |
| `static/admin/index.html` | Panel: turnos, cierres de agenda y diálogo de cancelación |
| `assets/js/admin-login.js` | Copia el token CSRF y valida los campos |
| `assets/js/admin-panel.js` | Lista y cambia turnos, gestiona cierres y prepara el cierre de sesión |

**Por qué el token CSRF va en una cookie y no en el HTML:** la página de login
es un archivo estático. Spring no la genera, así que no puede imprimir el token
dentro del formulario. El filtro propio pide el token y Spring lo escribe en la
cookie `XSRF-TOKEN`; el JavaScript la lee y completa el campo `_csrf`.

Se usa `CsrfTokenRequestAttributeHandler` (el handler "simple") porque el
formulario manda el valor de la cookie tal cual. El handler por defecto aplica
un enmascarado XOR pensado para tokens impresos en el HTML, que acá no aplica.

**URLs limpias:** Spring MVC no resuelve solo un pedido a `/admin` como
`static/admin/index.html`. Por eso `WebConfig` declara dos view controllers que
hacen un *forward* interno. La barra de direcciones no cambia.

**Qué muestra el panel:** el listado de turnos (hoy y los próximos 7 días, o
filtrado por estado). Desde ahí el barbero confirma, completa o cancela un
turno. Al confirmar, aparece el botón de WhatsApp con el mensaje ya armado.

### 7.4 Avisos al barbero: email automático y WhatsApp manual

El circuito de avisos es:

```
Cliente reserva
  └─► turno PENDING  (se guarda en la base)
        └─► evento AppointmentCreatedEvent
              └─► (AFTER_COMMIT) EmailAppointmentNotifier
                    ├─ MAIL_ENABLED=false → aviso en el log (dry-run)
                    └─ MAIL_ENABLED=true  → email al barbero

Barbero confirma desde el panel
  └─► turno CONFIRMED
        └─► el panel arma un enlace wa.me con el mensaje de confirmación
```

**Por qué el aviso se dispara recién después del commit.** El evento se publica
dentro de la transacción que crea el turno, pero el listener usa
`@TransactionalEventListener(AFTER_COMMIT)`. Si la reserva termina en rollback
(por ejemplo, porque otro cliente tomó el horario), no se avisa de nada. El
aviso usa un `record` con los datos copiados y no la entidad, para no tocar
relaciones perezosas con la sesión ya cerrada.

**Por qué un fallo de correo no rompe la reserva.** El listener y el notificador
capturan las excepciones y las registran. El turno ya está guardado cuando el
email intenta salir.

**Por qué WhatsApp es manual en esta etapa.** La API oficial de WhatsApp
Business exige una cuenta de Meta, un número dedicado y plantillas aprobadas;
además, los mensajes iniciados por el negocio pueden tener costo. El enlace
`wa.me` que arma `WhatsAppLinkBuilder` es **gratis**, funciona desde el primer
día y no depende de ningún tercero. El barbero toca "enviar". Cuando se
justifique la API oficial, se reemplaza el notificador sin tocar el panel.

**Modo dry-run.** Con `MAIL_ENABLED=false` (el valor por defecto) no se envía
ningún correo: el contenido queda en el log, con el teléfono enmascarado. Es la
forma de probar todo el circuito sin credenciales y sin mandar mensajes reales.

---

### 7.5 Límite de intentos de reserva

El formulario público acepta como máximo **5 intentos por IP cada 10 minutos**
(configurable con `RATE_LIMIT_*`). Se cuenta todo intento, incluso los que
después fallan por una regla de negocio, para que no se pueda eludir el límite
mandando datos inválidos. Al superarlo responde `429 RATE_LIMIT_EXCEEDED` con
el encabezado `Retry-After`.

El conteo vive en memoria (`ConcurrentHashMap`), porque el proyecto corre en
una sola instancia. Con varias instancias, cada una contaría por separado y
habría que moverlo a un almacenamiento compartido. La IP se toma de la
conexión (`getRemoteAddr()`); detrás de un proxy hay que configurar
`server.forward-headers-strategy`.

---

## 8. Base de datos

| Perfil | Base de datos | Para qué |
|---|---|---|
| `test` | H2 en memoria | Pruebas rápidas y aisladas |
| `dev` | H2 en archivo | Datos que sobreviven al reinicio, sin instalar nada |
| `prod` | PostgreSQL 18 | Datos reales, concurrentes y respaldables |

Un **único** juego de migraciones de Flyway para las tres, escrito en SQL
estándar portable. El esquema nunca se crea con `ddl-auto`: en producción
Hibernate **nunca** modifica el esquema por su cuenta.

---

## 9. Estrategia de pruebas

| Tipo | Qué cubre | Ejemplo concreto |
|---|---|---|
| Unitario | Reglas de negocio puras | Un servicio de 60 min desde las 20:00 necesita cerrar después de las 21:00 |
| Integración | Endpoints + base real (H2) | Reservar un horario ya ocupado devuelve 409 |
| Concurrencia | La garantía de no superposición | 20 peticiones simultáneas al mismo horario → 1 sola con 201 |
| Seguridad | El panel está protegido | `/api/admin/appointments` sin sesión devuelve 401 |
| Login | Acceso, credenciales y CSRF | `LoginSecurityTest`: página pública, redirect, 401, login válido, error, sin token → 403 y logout |

Cobertura mínima antes de dar por terminado un módulo: **crear turno válido,
turno duplicado, turno en horario cerrado, turno en el pasado, validación de
campos y acceso no autorizado al panel**.

**Estado de la suite (verificado el 2026-11-08):** pasan **las 46 pruebas**.

| Clase | Pruebas | Qué cubre |
|---|---|---|
| `ScheduleRulesTest` (Límites y Redondeo) | 9 | Reglas de la grilla y duración de servicios |
| `ConcurrentBookingTest` | 2 | 20 reservas simultáneas → gana una sola |
| `BookingFlowApiTest` | 3 | Disponibilidad, alta, 409, confirmación y WhatsApp |
| `AdminBlockApiTest` | 9 | Cierres completos, parciales, superposición y acceso |
| `LoginSecurityTest` | 8 | Acceso al panel, CSRF y logout |
| `WhatsAppLinkBuilderTest` | 6 | Normalización del teléfono argentino |
| `EmailAppointmentNotifierTest` | 3 | Dry-run y contenido del email |
| `BookingRateLimiterTest` | 5 | Límite, ventana, IPs distintas y reinicio |
| `RateLimitApiTest` | 1 | Respuesta 429 con `Retry-After` |

Las 2 fallas viejas de concurrencia quedaron resueltas con el cambio a
`Persistable` descrito en §6.6.

---

## 10. Límites conocidos de este entorno

Lo que **no** se pudo verificar acá, y cómo se cubre:

| No verificado | Causa | Cobertura |
|---|---|---|
| Comportamiento real en PostgreSQL | no hay PostgreSQL ni Docker disponibles | SQL portable + checklist para probar con un PostgreSQL local |
| Envío real de WhatsApp | requiere cuenta de Meta verificada | modo `dry-run`: **nunca** informa un envío exitoso que no ocurrió |
| Tendencias reales de redes | sin acceso autenticado a esas plataformas | verificación de fuentes antes de integrar nada (ver §11) |
| Alojamiento y costos | depende del país y el presupuesto | se evalúa en la Etapa 6, con precios del momento |

---

## 11. Nota anticipada sobre el asistente de marketing (Etapa 5)

Se verificó en la documentación oficial de Meta que la **Instagram API no
ofrece ningún endpoint de búsqueda de hashtags ni de tendencias**. Lo que sí
ofrece es: insights de las publicaciones propias, publicación de contenido,
 moderación de comentarios, menciones y mensajería.

Consecuencia: **el asistente no puede obtener tendencias de Instagram por API
oficial.** Habrá que usar fuentes reales y verificables (por ejemplo, YouTube
Data API, que sí permite búsqueda con cupos, o feeds RSS de publicaciones del
rubro). Por eso el primer paso de la Etapa 5 es documentar, fuente por fuente,
qué se puede consultar, con qué permisos y con qué límites — **antes** de
escribir código que dependa de esos datos.

---

## 12. Technology Debt Register

Deuda técnica asumida conscientemente, con su motivo:

| Deuda | Motivo | Se resuelve en |
|---|---|---|
| Un solo usuario administrador (en memoria) | Una barbería tiene un dueño | Si aparece un segundo usuario |
| Sin selector de barbero en el frontend | Hoy hay un solo barbero | Cuando haya más de uno |
| Sin rate limiting en el login | Un panel único, uso interno | Antes de exponer a internet |
| Sin paginación | Volumen bajo esperado | Cuando el listado crezca |
| Sin cache | Volumen bajo esperado | Si se detecta latencia |
