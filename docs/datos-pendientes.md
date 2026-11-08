# Datos que faltan — BarberTrend AI

Este proyecto **no inventa datos reales de la barbería**. Hasta que se
completen, el sitio funciona con **datos de ejemplo claramente marcados**.

Cada ítem indica dónde se usa hoy y en qué etapa hay que configurarlo.

> **Etapa 2 ya está hecha**: el sitio donde van estos datos es
> `backend/src/main/resources/static/assets/js/site-config.js`. Se puede
> cargar la información en cualquier momento, sin rehacer nada.

---

## Estado

| # | Dato | Estado | Dónde se usa | Etapa |
|---|---|---|---|---|
| 1 | Nombre de la barbería | ✅ REAL | Sitio, título, WhatsApp | 2 |
| 2 | Eslogan / bajada | ⚠️ DE EJEMPLO | Portada | 2 |
| 3 | Logo | ✅ REAL | Encabezado, pie, favicon | 2 |
| 4 | Colores de marca | ✅ DEFINIDO | CSS (`--gold`) | 2 |
| 5 | Teléfono / WhatsApp | ✅ REAL · confirmado | `549 2284 239845` | 2 |
| 6 | Email de contacto | ⬜ FALTA | Se oculta la fila | 2 |
| 7 | Instagram | ✅ REAL | `@Barberia_doble_s` | 2 |
| 8 | Facebook (u otra red) | ⬜ FALTA | Opcional | 2 |
| 9 | Dirección | ✅ REAL · oculta | Bolívar 3008 (sacada de la página) | 2 |
| 10 | Mapa / coordenadas | 🟡 SIN USO | Se sacó la tarjeta de Ubicación | 2 |
| 11 | Cómo llegar | ⬜ SIN USO | Se sacó la tarjeta de Ubicación | 2 |
| 12 | Horario por día | ✅ REAL | Lun a Sáb, dom. cerrado | 2 / 3 |
| 13 | Catálogo de servicios | ✅ REAL | 4 servicios con precio | 2 / 3 |
| 14 | Nombre del barbero | ⬜ FALTA | `barber` | 3 |
| 15 | Fotografías de la barbería | ✅ 4 FOTOS | Galería | 2 |
| 16 | Más fotos de cortes | ⬜ FALTAN | Galería | 2 |
| 17 | Política de cancelación | ⬜ FALTA | Formulario, panel | 3 |
| 18 | Zona horaria | 🟡 DEFINIDO | `America/Argentina/Buenos_Aires` | 1 |
| 19 | Moneda | 🟡 DEFINIDO | `ARS` | 1 |
| 20 | Duración de los bloques | 🟡 DEFINIDO | 30 minutos | 1 |
| 21 | Anticipación mínima | 🟡 PROPUESTO | 120 minutos | 1 |
| 22 | Horizonte máximo | 🟡 PROPUESTO | 60 días | 1 |
| 23 | Dominio y alojamiento | ⬜ FALTA | Despliegue | 6 |
| 24 | Credenciales de la Meta (WhatsApp) | ⬜ FALTA | `.env` | 4 |
| 25 | Plantilla de WhatsApp | ⬜ FALTA | `.env` | 4 |
| 26 | Proveedor de IA y credenciales | ⬜ FALTA | `.env` | 5 |

**Leyenda:** ✅ real y cargado · ⚠️ cargado con valor de ejemplo ·
🟡 definido o propuesto, confirmar · ⬜ falta

---

## Catálogo de servicios (precios reales)

| Servicio | Duración | Precio |
|---|---|---|
| Corte clásico | 30 min | $ 12.000 |
| Barba completa | 30 min | $ 12.000 |
| Corte + barba | 60 min | $ 15.000 |
| Corte infantil | 30 min | $ 9.000 |

**Las duraciones no se muestran al cliente, a propósito.** Un corte no dura
siempre lo mismo: depende del pelo, de cuánto hace que el cliente no se corta
y de lo que pida. Poner "30 min" en la tarjeta sería una cifra que el propio
barbero no puede garantizar.

**Pero la duración no se borró del modelo de datos.** El sistema la necesita
para saber **cuántos bloques de 30 minutos ocupa el turno en la agenda**. Sin
eso no se puede calcular la disponibilidad ni evitar superposiciones.

Consecuencia práctica: si a alguien con pelo abundante le reservaron 30
minutos y el corte se lleva 50, el turno se pasa de hora. Esa situación se
maneja en el local. **Extender turnos desde el panel fue descartado** y no
forma parte de la hoja de ruta.

**Ninguna tarjeta dice "Más pedido".** Eso sería afirmar cuál es el más
pedido, y no lo sabemos. Si querés marcarlo, agregá `popular: true` al
servicio correspondiente en `site-config.js`.

## Horario de atención (real)

| Día | Horario |
|---|---|
| Lunes a Sábado | 09:00 – 21:00 |
| Domingo | Cerrado |

## Sección de Ubicación: sacada de la página

Por pedido, la tarjeta de "Ubicación" (dirección y link al mapa) se eliminó
del bloque de Contacto. Quedaron dos tarjetas: Horarios y Escribinos.

Los datos **no se borraron**: siguen en `SITE_CONFIG.contact.address`
("Bolívar 3008") por si más adelante se usan en otro lugar, por ejemplo para
mandarle la dirección al cliente por WhatsApp.

Para volver a mostrarla, alcanza con reponer el `<div class="contact-card">`
en `index.html` y las líneas de `renderContact()` en `main.js` que escriben
sobre `#contact-address` y `#map-link`.

---

## Datos reales cargados (Etapa 2)

Del logo que aportaste en `barberia doble s/` y de lo que confirmaste:

| Dato | Valor |
|---|---|
| Nombre | Doble S Barbería |
| Instagram | `@Barberia_doble_s` |
| Dirección | Bolívar 3008 (guardada, no mostrada) |
| Teléfonos | 2284 239845 · 2284 245945 |
| WhatsApp | `+54 9 2284 239845` — confirmado por el dueño |
| Horario | Lun–Jue 9–19, Vie 9–20, Sáb 9–14, Dom cerrado |
| Servicios | Corte clásico 12.000 · Barba 12.000 · Corte+barba 15.000 · Infantil 9.000 |
| Logo | `assets/img/logo-doble-s.png` |
| Fotos de la galería | 4 fotos de cortes reales |

### Lo único que todavía es de ejemplo

Los **textos de la portada** ("Corte con carácter, barba con detalle") y el
**eslogan** que aparece junto al logo. Son redaccionables: pasame los tuyos y
los cambio.

Por eso el aviso del pie dice lo que dice: *"Las reservas online todavía no
están habilitadas"*, que es la única limitación real que le queda al sitio.

---

## Cómo se van a reemplazar los datos de ejemplo

### Datos que viven en el sitio (Etapa 2)

Están **centralizados en un solo archivo**:

```
backend/src/main/resources/static/assets/js/site-config.js
```

Ese archivo tiene un bloque `SITE_CONFIG` con todos los textos, precios,
horarios y enlaces. Para cambiar la barbería real se edita **solo ese
archivo**, sin tocar el HTML ni el CSS.

Mientras los valores sean de ejemplo, la web muestra un aviso visible en el
pie: *"Sitio en construcción — se muestran datos de ejemplo"*.

### Datos que pasan a la base de datos (Etapa 3)

El horario de atención, el catálogo de servicios y los bloqueos de agenda
**no** van a quedar en el código: se cargan desde el panel del dueño y se
guardan en la base. Esos datos se administran en `/admin`.

Los servicios y el horario se cargarán por primera vez mediante migraciones de
Flyway con los datos definitivos.

### Datos que van en variables de entorno (nunca en el código)

- Credenciales de la base de datos
- Credenciales de la Meta para WhatsApp
- Credenciales del proveedor de IA
- Contraseña del administrador

Todas están en `.env.example` con valores de ejemplo. El `.env` real está en
`.gitignore` y **nunca** se sube al repositorio.

---

## El logo: cómo se le quitó el fondo blanco

El logo original venía en JPEG, que **no admite transparencia**, sobre un
fondo blanco. Al ponerlo tal cual en el encabezado oscuro quedaba un recuadro
blanco, que es lo que se veía mal.

**Por qué no se resolvió borrando el blanco directamente.** El logo tiene
partes blancas **en su interior**: la "S" de la izquierda es plateada y las
tijeras del centro son claras. Un borrado simple le habría hecho agujeros al
propio logo.

**Cómo se resolvió.** Con una inundación (*flood fill*) que arranca **solo
desde los bordes** de la imagen y se expande mientras encuentre blanco:

1. El fondo está en contacto con los bordes → se elimina entero.
2. El blanco del interior **no está conectado al borde** → sobrevive intacto.
3. Un segundo paso suaviza el contorno: los píxeles pegados al fondo se
   vuelven transparentes de a poco, en vez de cortarse de golpe. Si no,
   quedaría un borde duro alrededor del logo.

Después se recortó el espacio vacío sobrante y se escaló a 315×320 px.

> **Resultado:** el logo es un PNG con canal alfa (98 KB) y se apoya sobre el
> fondo oscuro sin ningún recuadro blanco. El interior circular queda blanco,
> y eso es intencional: se lee como un medallón clásico de barbería, con su
> aro dorado.

Si en algún momento se quiere el interior también transparente, se puede
intentar, pero hay que revisar que la "S" plateada no quede perforada.

## Las fotos de la galería

Las cuatro son **fotos reales**. Todas pesan poco porque se reescalaron y se
recomprimieron antes de subirlas:

| Archivo | Original | Optimizado | Qué muestra |
|---|---|---|---|
| `corte-degradado-barba.jpg` | 135 KB | **67 KB** | Degradado bajo con barba |
| `corte-fade-texturizado.jpg` | 207 KB | **102 KB** | Fade con textura |
| `corte-rizado-degradado.jpg` | 172 KB (PNG) | **19 KB** | Corte rizado con degradado |
| `corte-flequillo.jpg` | 133 KB (PNG) | **17 KB** | Corte con flequillo |

Las dos últimas venían como capturas de pantalla en PNG: pesaban muchísimo para
su tamaño porque PNG guarda cada píxel sin pérdida. Como son fotos y no logos,
no necesitan transparencia, así que convertirlas a JPEG las dejó en 19 KB y
17 KB: **un 89 % menos de peso**.

Las fotos cargan con `loading="lazy"`: el navegador no las descarga hasta que
están por entrar en pantalla. Como la galería está al final de la página, al
entrar no se descargan.

**Ya no hay marcadores de posición en la galería.** Las cuatro fichas tienen su
foto. El código de `main.js` igual conserva el Marcador por si algún día se
agrega una entrada sin `src`, pero hoy no se usa.

Para agregar más fotos: copiá el archivo a `static/assets/img/`, optimizalo si pesa mucho
y sumá un objeto con `title` y `src` al array `gallery` de `site-config.js`.

---

## Dónde se van los datos, según el tipo

| Dato | Va en | Por qué ahí |
|---|---|---|
| Nombre, dirección, teléfonos, Instagram | `site-config.js` | Se muestran en la web |
| Precios, horarios, eslogan | `site-config.js` (ahora) | Después pasan al panel |
| Logo y fotos | `static/assets/img/` | Son archivos, no datos |
| Contraseñas, tokens, claves de API | `.env` | Nunca en el repositorio |
| Horarios y servicios definitivos | Panel `/admin` → base de datos | Los cambia el dueño sin tocar código |

## Nada de esto se inventa

Mientras un dato esté marcado como ⬜ FALTA, el sitio muestra un marcador
visible en su lugar, o directamente oculta la sección si no corresponde (por
ejemplo, la fila del email). No se escriben precios inventados como si fueran
reales, ni direcciones, ni horarios, ni métricas de redes sociales.

Cuando me pases los datos, los implemento en el archivo o en la migración que
corresponda y te indico exactamente cuáles toqué.
