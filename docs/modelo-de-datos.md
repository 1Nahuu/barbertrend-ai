# Modelo de datos — BarberTrend AI

Definición de las tablas. Todas las migraciones de Flyway (`backend/src/main/resources/db/migration/`)
se escriben en **SQL estándar portable**, para que funcionen igual en H2
(desarrollo y pruebas) y en PostgreSQL (producción).

---

## Diagrama de relaciones

```
        ┌───────────┐
        │  service  │◄──────────┐
        └───────────┘           │
                                │
   ┌───────────┐          ┌─────┴──────┐        ┌────────────────┐
   │  barber   │◄─────────│ appointment │───────►│   customer     │
   └───────────┘          └─────┬──────┘        └────────────────┘
        │                       │
        │  ┌────────────────────┴───────────────┐
        │  │                                    │
        ▼  ▼                                    ▼
┌────────────────────┐              ┌──────────────────┐
│ appointment_slot   │              │     time_off     │
│ (garantía de no    │              │ (vacaciones,     │
│  superposición)    │              │  feriados)       │
└────────────────────┘              └──────────────────┘

        ┌────────────────┐
        │ business_hours │   (independiente: horario semanal)
        └────────────────┘
```

---

## 1. `service` — Servicios del catálogo

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | `BIGINT` | PK, identidad | Identificador |
| `name` | `VARCHAR(80)` | NOT NULL, único | Nombre del servicio |
| `description` | `VARCHAR(500)` | | Descripción para el cliente |
| `price` | `NUMERIC(10,2)` | NOT NULL, >= 0 | Precio |
| `duration_minutes` | `INTEGER` | NOT NULL, > 0 | Duración real |
| `active` | `BOOLEAN` | NOT NULL, por defecto `true` | Si aparece en la web |
| `sort_order` | `INTEGER` | NOT NULL, por defecto 0 | Orden en el catálogo |

**Regla de negocio:** `duration_minutes` se redondea **hacia arriba** al
múltiplo de `APP_SLOT_MINUTES` (30) más cercano. Un servicio de 45 minutos
ocupa 2 bloques = 60 minutos de silla. La validación vive en el servicio, no
en la base, porque depende de una configuración.

`active = false` desactiva el servicio **sin borrarlo**: los turnos ya
reservados siguen apuntando a él y el historial permanece intacto. Borrar
servicios que ya se reservaron rompería la trazabilidad.

---

## 2. `barber` — Barberos

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | `BIGINT` | PK, identidad | Identificador |
| `name` | `VARCHAR(120)` | NOT NULL | Nombre |
| `active` | `BOOLEAN` | NOT NULL, por defecto `true` | Si atiende |

**Hoy hay un solo barbero** (decisión de la Etapa 1). La tabla existe igual, y
la columna `barber_id` aparece en los turnos y en los slots, porque:

- Agregar un segundo barbero después sería una migración de datos sobre
  información ya guardada por los clientes.
- La lógica de disponibilidad y la garantía de no superposición funcionan
  **exactamente igual** con uno o con diez.

El flujo público de la versión 1 **no** muestra un selector de barbero: el
servicio de reservas resuelve el único barbero activo. Si se activara un
segundo barbero sin actualizar la interfaz, el sistema lo detectaría y
devolvería un error explícito en lugar de comportarse mal.

---

## 3. `business_hours` — Horario de atención semanal

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | `BIGINT` | PK, identidad | Identificador |
| `day_of_week` | `INTEGER` | NOT NULL, único, 1..7 | 1 = lunes … 7 = domingo |
| `open_time` | `TIME` | | Hora de apertura |
| `close_time` | `TIME` | | Hora de cierre |
| `is_closed` | `BOOLEAN` | NOT NULL, por defecto `false` |Día cerrado |

Siempre hay **7 filas**, una por día. Los días cerrados conservan su fila con
`is_closed = true` para que el dueño vea el horario completo y pueda
cambiarlo desde el panel.

---

## 4. `customer` — Clientes

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | `BIGINT` | PK, identidad | Identificador |
| `name` | `VARCHAR(120)` | NOT NULL | Nombre y apellido |
| `phone` | `VARCHAR(32)` | NOT NULL, indexado | Teléfono **normalizado a dígitos** |
| `created_at` | `TIMESTAMP` | NOT NULL | Alta |

**Decisión de privacidad:** se guarda únicamente lo indispensable para poder
contactar al cliente y prestarle el servicio. No se piden documentos, ni
dirección, ni fecha de nacimiento, ni email.

El teléfono se guarda en dos formatos:

- `phone`: solo dígitos (`5491122334455`), para comparar y detectar duplicados
  sin depender de cómo lo escribió el cliente.
- El formato visible se reconstruye al mostrarlo, y **se enmascara en los logs**
  (`54911*****45`).

No hay restricción de unicidad: un mismo teléfono puede reservar varias
veces, que es lo normal.

---

## 5. `appointment` — Turnos

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | `BIGINT` | PK, identidad | Identificador interno |
| `reference` | `VARCHAR(16)` | NOT NULL, **único** | Código público (`BT-7K2M9Q`) |
| `customer_id` | `BIGINT` | FK → `customer`, NOT NULL | Cliente |
| `barber_id` | `BIGINT` | FK → `barber`, NOT NULL | Barbero |
| `service_id` | `BIGINT` | FK → `service`, NOT NULL | Servicio |
| `starts_at` | `TIMESTAMP` | NOT NULL, indexado | Inicio |
| `ends_at` | `TIMESTAMP` | NOT NULL | Fin |
| `status` | `VARCHAR(20)` | NOT NULL | Estado (ver abajo) |
| `notes` | `VARCHAR(500)` | | Notas del cliente |
| `cancellation_reason` | `VARCHAR(200)` | | Motivo de la cancelación |
| `created_at` | `TIMESTAMP` | NOT NULL | Alta |
| `updated_at` | `TIMESTAMP` | NOT NULL | Última modificación |

Índices: `starts_at`, `(barber_id, starts_at)`, `status`.

### 5.1 El campo `reference`

Cada turno recibe un código corto y aleatorio, por ejemplo `BT-7K2M9Q`. Sirve
para dos cosas:

1. El cliente puede consultar su turno sin iniciar sesión y **sin exponer los
   datos de otros clientes**.
2. Se puede dar por teléfono sin decir "el turno de tal persona".

El código **no** se genera de forma secuencial (`BT-000001`), porque eso
permitiría probarlos y ver los ajenos.

### 5.2 Estados

```
                 ┌──────────────┐
   reserva ────► │   PENDING    │ (el dueño tiene que confirmar)
                 └──┬────────┬──┘
        confirmar   │        │   cancelar
                    ▼        ▼
             ┌────────────┐  ┌───────────┐
             │ CONFIRMED  │  │ CANCELLED │
             └─────┬──────┘  └───────────┘
                   │   cancelar
                   ▼
             ┌────────────┐
             │ COMPLETED  │
             └────────────┘
```

| Estado | Significado | Ocupa el horario |
|---|---|---|
| `PENDING` | Reserva recibida, esperando confirmación del dueño | **Sí** |
| `CONFIRMED` | Confirmada por el dueño | **Sí** |
| `COMPLETED` | El servicio se prestó | Ya no (fecha pasada) |
| `CANCELLED` | Cancelada, con motivo | **No** (los slots se liberan) |

**Por qué la reserva nace `PENDING`.** Decisión de la Etapa 1: el dueño
confirma. Evita que se acumulen horarios ocupados por gente que nunca se
presentó, que es el problema clásico de la reserva online sin filtro.

Un turno `PENDING` **sí bloquea el horario**: si dos personas piden las 10:00,
la segunda recibe 409 aunque la primera todavía no haya sido confirmada. Sin
esa protección, ambos turnos entrarían en la lista de pendientes.

### 5.3 Cancelación

Al cancelar un turno se hace, **en la misma transacción**:

1. El turno pasa a `CANCELLED` con su motivo.
2. Se borran sus filas de `appointment_slot`.
3. El horario queda inmediatamente disponible para otro cliente.

Se conserva el registro del turno cancelado en lugar de borrarlo: permite
saber qué pasó y evita que alguien reutilice el código de referencia.

---

## 6. `appointment_slot` — Bloqueo de la agenda

**La tabla que garantiza que no se superpongan las reservas.**

| Columna | Tipo | Restricciones |
|---|---|---|
| `barber_id` | `BIGINT` | **parte de la PK**, FK → `barber` |
| `slot_start` | `TIMESTAMP` | **parte de la PK** |
| `appointment_id` | `BIGINT` | NOT NULL, FK → `appointment` |

### 6.1 Cómo funciona

La jornada se divide en bloques de 30 minutos. Una reserva **ocupa bloques, no
un instante**: un corte de 60 minutos a las 10:00 inserta dos filas,
`(barber, 10:00)` y `(barber, 10:30)`.

Cuando alguien intenta tomar un bloque ya ocupado, el `INSERT` viola la clave
primaria compuesta y **la base de datos rechaza la operación**. La aplicación
traduce ese error a `409 Conflict`.

### 6.2 Por qué no un `UNIQUE` sobre `starts_at`

Porque dos turnos **pueden empezar en horarios distintos y pisarse**:

```
10:00 → 11:00   y   10:30 → 11:30
```

Un `UNIQUE(barber_id, starts_at)` no detectaría el choque, porque `starts_at`
es distinto en cada caso. Occupar bloques hace que el conflicto se vea siempre.

### 6.3 Por qué no un índice parcial de PostgreSQL

PostgreSQL admite `CREATE UNIQUE INDEX ... WHERE status = 'PENDING'`, que sería
más compacto. Pero **H2 no soporta índices parciales**: el comportamiento en
desarrollo y en las pruebas sería distinto del de producción, que es justo el
tipo de diferencia que produce errores días antes de desplegar.

Con esta tabla, la lógica es idéntica en ambas bases y la garantía la impone la
base de datos, no el código de aplicación.

### 6.4 Invariante

Una reserva y sus filas de slots se crean **siempre juntas**, dentro de la
misma transacción. Nunca puede existir un turno sin sus bloques bloqueados,
ni bloques bloqueados sin un turno que los justifique.

---

## 7. `time_off` — Bloqueos de agenda

Vacaciones, feriados, un almuerzo largo, un curso.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | `BIGINT` | PK, identidad | Identificador |
| `barber_id` | `BIGINT` | FK, nullable | `NULL` = todo el local |
| `starts_at` | `TIMESTAMP` | NOT NULL | Inicio del bloqueo |
| `ends_at` | `TIMESTAMP` | NOT NULL | Fin del bloqueo |
| `reason` | `VARCHAR(200)` | | Motivo visible |

Al calcular la disponibilidad, cualquier bloque que se superponga con un
`time_off` se descarta. Un bloque que cubre el día completo se informa como
día cerrado.

Desde el panel se crean cierres de día completo o por horas. Antes de crearlos
se verifica que no haya turnos `PENDING` o `CONFIRMED` dentro del rango: si los
hay, el cierre se rechaza y el barbero tiene que cancelarlos primero. El
sistema **no cancela turnos automáticamente**.

---

## 8. Tablas previstas para etapas posteriores

Se documentan ahora para que quede claro el camino, **sin crearlas todavía**:

| Tabla | Etapa | Para qué |
|---|---|---|
| `marketing_source` | 5 | Fuentes de tendencias consultadas y su estado |
| `marketing_idea` | 5 | Ideas generadas, con su origen verificable |
| `marketing_delivery` | 5 | Envíos de recomendaciones y su resultado |
| `post_result` | 5+ | Resultados de las publicaciones, para relacionarlos con reservas |

---

## 9. Migraciones

- Un solo juego de migraciones para H2 y PostgreSQL.
- `spring.jpa.hibernate.ddl-auto = none` en todos los perfiles: **Hibernate
  nunca modifica el esquema**. El esquema cambia únicamente por una migración
  de Flyway, versionada y revisable.
- Convenciones de nombres: tablas en `snake_case`, claves foráneas
  `id`, índices `idx_<tabla>_<columna>`.
