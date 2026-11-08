# API REST — BarberTrend AI

Contrato de la API. **Los endpoints públicos de reserva y los del panel de
turnos ya están implementados**; los que figuran como "previsto" se agregan en
los próximos pasos. Si cambia algo, se actualiza este documento en el mismo
commit.

Todas las rutas de la API responden y consumen `application/json`. La única
excepción son las páginas HTML del panel, que se describen más abajo.

---

## Formato de errores

Todos los errores usan la misma estructura, para que el frontend pueda
tratarlos de una sola forma:

```json
{
  "code": "SLOT_NOT_AVAILABLE",
  "message": "Ese horario acaba de ser reservado. Elegí otro.",
  "field": null,
  "timestamp": "2026-10-18T22:30:00"
}
```

| Código HTTP | Cuándo |
|---|---|
| `400` | Faltan campos o tienen un formato inválido |
| `401` | Se pidió una ruta protegida sin sesión |
| `403` | Sin permisos |
| `404` | El recurso no existe |
| `409` | Conflicto: el horario ya está ocupado |
| `422` | Los datos son válidos en formato pero no tienen sentido (ej. una fecha en el pasado) |
| `429` | Demasiados intentos |
| `500` | Error inesperado (el detalle nunca se muestra al cliente) |

---

## Endpoints públicos

Sin autenticación. Sirven el sitio web.

### `GET /api/public/services`

Servicios activos, ordenados.

```json
[
  {
    "id": 1,
    "name": "Corte clásico",
    "description": "Corte a tijera y máquina, incluye lavado y peinado.",
    "price": 12000.00,
    "currency": "ARS",
    "durationMinutes": 30
  }
]
```

### `GET /api/public/availability`

Horarios libres para un servicio en una fecha.

**Parámetros:** `serviceId` (obligatorio), `date` (obligatorio, `YYYY-MM-DD`)

```json
{
  "date": "2026-10-20",
  "serviceId": 1,
  "serviceName": "Corte clásico",
  "durationMinutes": 30,
  "timezone": "America/Argentina/Buenos_Aires",
  "closed": false,
  "slots": [
    { "startsAt": "2026-10-20T09:00:00", "available": true },
    { "startsAt": "2026-10-20T09:30:00", "available": false }
  ]
}
```

Un día cerrado devuelve `"closed": true` y `"slots": []`, **no un error**: es un
resultado válido que la web debe saber mostrar. Esto incluye tanto los días
sin horario de atención como los días cerrados desde el panel con un cierre
de día completo.

### `POST /api/public/appointments`

Crea una reserva. Estado inicial: `PENDING`.

```json
{
  "serviceId": 1,
  "startsAt": "2026-10-20T09:00:00",
  "customerName": "Juan Pérez",
  "phone": "5491122334455",
  "notes": "Corto los lados más cortos"
}
```

**Respuesta `201 Created`:**

```json
{
  "reference": "BT-7K2M9Q",
  "status": "PENDING",
  "serviceName": "Corte clásico",
  "startsAt": "2026-10-20T09:00:00",
  "endsAt": "2026-10-20T09:30:00",
  "message": "Recibimos tu pedido. Te confirmamos por WhatsApp a la brevedad."
}
```

**Errores frecuentes:**

| Código | Causa |
|---|---|
| `409 SLOT_NOT_AVAILABLE` | Otro cliente tomó ese horario mientras tanto |
| `422 OUTSIDE_SCHEDULE` | Fuera del horario de atención |
| `422 TOO_SOON` | Dentro de la anticipación mínima |
| `422 TOO_FAR_AHEAD` | Más allá del horizonte máximo de reserva |
| `422 PAST_DATE` | Horario en el pasado |
| `422 BLOCKED_TIME` | El horario está dentro de un cierre cargado desde el panel |
| `429 RATE_LIMIT_EXCEEDED` | Demasiados intentos desde la misma IP |
| `400 VALIDATION_ERROR` | Nombre o teléfono con formato inválido |

El `429` incluye el encabezado `Retry-After` con los segundos que conviene
esperar antes de reintentar. Por defecto se permiten 5 intentos cada 10
minutos por IP (`RATE_LIMIT_*`). Se cuentan todos los intentos, incluso los
que fallan por una regla de negocio.

### `GET /api/public/appointments/{reference}`

Consulta el estado de un turno con su código, **sin iniciar sesión**.

```json
{
  "reference": "BT-7K2M9Q",
  "status": "PENDING",
  "serviceName": "Corte clásico",
  "startsAt": "2026-10-20T09:00:00",
  "customerFirstName": "Juan"
}
```

Devuelve **solo** lo necesario: no expone el teléfono completo ni datos de
otros clientes.

---

## Acceso al panel (páginas HTML)

El panel no se autentica con JSON: usa el form login de Spring Security y una
cookie de sesión.

| Método | Ruta | Qué hace |
|---|---|---|
| `GET` | `/admin/login` | Página de ingreso (pública). Si ya hay sesión → `302 /admin` |
| `POST` | `/login` | Valida `username`, `password` y `_csrf`. Correcto → `302 /admin`; incorrecto → `302 /admin/login?error` |
| `GET` | `/admin` | Panel. Sin sesión → `302 /admin/login` |
| `POST` | `/admin/logout` | Cierra la sesión. Requiere `_csrf` → `302 /admin/login?logout` |

El token CSRF se lee de la cookie `XSRF-TOKEN`, que el servidor escribe al
pedir `/admin/login`, y se envía en el campo `_csrf`. Sin token, el `POST`
responde `403`. La cookie de sesión es `HttpOnly`; la de CSRF no, porque el
JavaScript necesita leerla.

---

## Endpoints del panel del dueño

Requieren sesión iniciada (la misma del panel). Sin sesión: `401` en JSON.

| Método | Ruta | Qué hace | Estado |
|---|---|---|---|
| `GET` | `/api/admin/appointments?from=&to=&status=` | Listado (por defecto: hoy + 7 días). Filtra por `status` (`PENDING`, `CONFIRMED`, `COMPLETED`, `CANCELLED`) y por rango de fechas | ✅ |
| `GET` | `/api/admin/appointments/{id}` | Detalle: teléfono completo y `clientWhatsAppUrl` | ✅ |
| `PATCH` | `/api/admin/appointments/{id}/confirm` | Pasa de `PENDING` a `CONFIRMED` | ✅ |
| `PATCH` | `/api/admin/appointments/{id}/complete` | Pasa de `CONFIRMED` a `COMPLETED` y libera el horario | ✅ |
| `POST` | `/api/admin/appointments/{id}/cancel` | Cancela con motivo (`{"reason": "..."}`) y libera el horario | ✅ |
| `GET` | `/api/admin/services` | Servicios, incluidos los inactivos | ✅ |
| `POST` | `/api/admin/services` | Crear servicio | ✅ |
| `PUT` | `/api/admin/services/{id}` | Modificar servicio | ✅ |
| `PATCH` | `/api/admin/services/{id}/active` | Activar o desactivar | ✅ |
| `GET` | `/api/admin/business-hours` | Horario semanal | ✅ |
| `PUT` | `/api/admin/business-hours/{dayOfWeek}` | Actualizar un día | ✅ |
| `GET` | `/api/admin/blocks` | Cierres vigentes o futuros | ✅ |
| `POST` | `/api/admin/blocks` | Crear un cierre de día completo o por horas | ✅ |
| `DELETE` | `/api/admin/blocks/{id}` | Quitar un cierre y liberar la agenda | ✅ |

Las operaciones de escritura usan **peticiones idempotentes donde tiene
sentido**: por ejemplo, desactivar un servicio ya desactivado devuelve éxito
en lugar de un error.

### Detalle de un turno

```json
{
  "id": 42,
  "reference": "BT-7K2M9Q",
  "status": "CONFIRMED",
  "serviceName": "Corte clásico",
  "startsAt": "2026-10-20T09:00:00",
  "endsAt": "2026-10-20T09:30:00",
  "customerName": "Juan Pérez",
  "customerPhone": "2284239845",
  "notes": "Corto los lados más cortos",
  "cancellationReason": null,
  "clientWhatsAppUrl": "https://wa.me/5492284239845?text=Hola%20Juan..."
}
```

A diferencia del listado, acá el teléfono va **completo** y se incluye
`clientWhatsAppUrl`: es la pantalla donde el barbero le avisa al cliente por
WhatsApp. El enlace puede ser `null` si el turno está cancelado o si el
teléfono no se pudo interpretar.

### Cierres y bloqueos

El panel puede cerrar la agenda para un día completo, varios días seguidos o
una franja de un solo día. Los cierres se guardan en la misma tabla
`time_off` que ya usaba la disponibilidad, así que no requieren una migración
nueva.

**Cierre de día completo:**

```json
{
  "startDate": "2026-11-12",
  "endDate": "2026-11-14",
  "allDay": true,
  "startTime": null,
  "endTime": null,
  "reason": "Vacaciones"
}
```

`endDate` es opcional: si se omite, se cierra solo `startDate`. El rango es
inclusivo (`12`, `13` y `14` quedan cerrados), pero internamente el final se
guarda como el `15/11 00:00`, porque los rangos de la agenda son exclusivos.

**Cierre por horas:**

```json
{
  "startDate": "2026-11-12",
  "allDay": false,
  "startTime": "13:00:00",
  "endTime": "15:00:00",
  "reason": "Almuerzo"
}
```

Un cierre por horas tiene que ser de un solo día y sus horas deben caer en
bloques de 30 minutos, igual que los turnos.

**Respuesta `201 Created`:**

```json
{
  "id": 7,
  "startsAt": "2026-11-12T00:00:00",
  "endsAt": "2026-11-15T00:00:00",
  "allDay": true,
  "reason": "Vacaciones"
}
```

**Errores frecuentes:**

| Código | Causa |
|---|---|
| `409 BLOCK_OVERLAP` | Ya hay otro cierre que se superpone con el rango |
| `409 BLOCK_HAS_APPOINTMENTS` | Hay turnos `PENDING` o `CONFIRMED` dentro del rango |
| `422 INVALID_BLOCK` | Rango inválido, horas desalineadas o datos incompletos |
| `422 BLOCK_IN_PAST` | El cierre termina antes de la hora actual |
| `404 BLOCK_NOT_FOUND` | Se intentó eliminar un cierre inexistente |

Reglas importantes:

- **No se cancelan turnos automáticamente.** Si hay turnos activos, el cierre
  se rechaza y el panel indica cuántos hay. Primero se cancelan y después se
  crea el cierre.
- Un cierre de día completo hace que `GET /api/public/availability` devuelva
  `"closed": true`.
- Un cierre parcial oculta los horarios afectados, pero deja el resto del día
  disponible.
- `POST /api/public/appointments` también valida los cierres. Aunque alguien
  llame directo a la API, no puede reservar dentro de un horario bloqueado.
- `DELETE` libera el horario inmediatamente. No se envía ningún aviso al
  cliente.

---

## Notificaciones

El sistema **no envía WhatsApp automáticamente** en esta etapa: el barbero
dispara el mensaje desde el panel con el enlace `wa.me`. Lo que sí es
automático es el **aviso por email al barbero** cuando entra un turno nuevo.

- Se dispara **después** de que la reserva queda guardada
  (`@TransactionalEventListener(AFTER_COMMIT)`). Si la reserva falla, no se
  avisa.
- Un error de correo **no rompe la reserva**: se registra y sigue.
- Con `MAIL_ENABLED=false` (por defecto) el aviso solo queda en el log, con el
  teléfono enmascarado (modo *dry-run*).
- Con `MAIL_ENABLED=true` se envía al `BARBER_EMAIL` configurado.

---

## Datos personales

El panel devuelve los datos de los clientes porque el dueño los necesita para
prestarlos el servicio. Las respuestas **no incluyen** datos que no hagan
falta: no se exponen documentos, direcciones ni emails porque no se guardan.

En los **listados** el teléfono va enmascarado (`54911*****45`) y se muestra
completo solo en el detalle de un turno, que es donde hace falta para llamar.

---

## Endpoints de operación

| Método | Ruta | Acceso |
|---|---|---|
| `GET` | `/actuator/health` | Público |
| resto de `/actuator/**` | — | Deshabilitado en producción |
