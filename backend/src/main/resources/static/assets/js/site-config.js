/* ==========================================================================
 * site-config.js — CONFIGURACIÓN DE LA BARBERÍA
 * ==========================================================================
 *
 *  DATOS REALES + DATOS DE EJEMPLO
 *
 *  Lo que YA es real (sacado del logo de la barbería):
 *    - Nombre: Doble S Barbería
 *    - Instagram: @Barberia_doble_s
 *    - Dirección: Bolívar 3008
 *    - Teléfonos: 2284 239845 y 2284 245945
 *    - Logo y fotos de la galería
 *
 *  Lo que SIGUE SIENDO DE EJEMPLO (marcado con ⚠️ abajo):
 *    - El eslogan y los textos de la portada
 *
 *  Para cambiar cualquier dato del negocio se edita SOLO este archivo,
 *  sin tocar el HTML ni el CSS.
 *
 *  ── Cómo se lee ──────────────────────────────────────────────────────────
 *  El HTML ya está cargado cuando estas funciones se ejecutan (los scripts
 *  van al final del <body>), así que se puede buscar cualquier elemento
 *  directamente con document.getElementById.
 *  ─────────────────────────────────────────────────────────────────────────
 */

window.SITE_CONFIG = {

  // -----------------------------------------------------------------------
  // Banderas del sistema
  // -----------------------------------------------------------------------
  flags: {
    /**
     * Etapa 3: la API de reservas ya está activa.
     *
     * Con esto en true, booking.js consulta la disponibilidad real
     * (GET /api/public/availability) y crea el turno
     * (POST /api/public/appointments).
     *
     * Se deja la bandera para poder apagar las reservas sin borrar código
     * si hiciera falta hacer mantenimiento.
     */
    bookingApiEnabled: true,

    /**
     * Muestra el aviso del pie de página.
     *
     * Poner en false cuando el sitio esté listo para publicar.
     */
    showSampleDataNotice: true
  },

  // Moneda en la que se muestran los precios.
  currency: 'ARS',

  // -----------------------------------------------------------------------
  // Identidad — DATOS REALES
  // -----------------------------------------------------------------------
  brand: {
    name: 'Doble S Barbería',

    // ⚠️ DATO DE EJEMPLO — el eslogan real todavía no está definido.
    // Si no querés un eslogan, poné '' y el lugar queda vacío.
    tagline: 'Corte, barba y buen carácter',

    // Iniciales que se usan en el título de la pestaña y en el pie,
    // cuando no hay logo que mostrar.
    initials: 'DS',

    // Aparece en el <title> y en los metadatos de la página.
    metaDescription:
      'Doble S Barbería en Bolívar 3008. Cortes clásicos, fade y barbas. ' +
      'Reservá tu turno online por WhatsApp.'
  },

  // -----------------------------------------------------------------------
  // Portada — ⚠️ TEXTOS DE EJEMPLO
  // -----------------------------------------------------------------------
  // Son los títulos grandes de la página. Conviene reemplazarlos por los
  // textos reales de la barbería: son los primero que ve el visitante.
  hero: {
    eyebrow: 'Barbería masculina',
    titleLine1: 'Corte con carácter,',
    titleLine2: 'barba con detalle.',
    text: 'Turnos online, sin llamadas ni esperas. ' +
          'Elegí tu servicio, elegí tu horario y listo.',
    scrollLabel: 'Bajar a servicios'
  },

  // -----------------------------------------------------------------------
  // Contacto — DATOS REALES
  // -----------------------------------------------------------------------
  contact: {
    // ── WhatsApp ───────────────────────────────────────────────────────
    // wa.me necesita el número en formato internacional: código de país,
    // luego el 9, luego el número sin el 0 inicial.
    //
    // ✅ CONFIRMADO por el dueño: 2284 239845 es un celular de Bolívar
    // (Buenos Aires), así que el internacional es 54 9 2284 239845.
    whatsappNumber: '5492284239845',

    // Cómo se ve el teléfono en pantalla.
    // El logo muestra dos números; acá van los dos.
    phoneDisplay: '2284 239845 · 2284 245945',

    // ── Instagram ──────────────────────────────────────────────────────
    instagram: 'Barberia_doble_s',

    // ── Ubicación ──────────────────────────────────────────────────────
    // La dirección NO se muestra en la página: se sacó la tarjeta de
    // "Ubicación" por pedido. El dato queda guardado acá por si más
    // adelante se usa en otro lugar (por ejemplo, en el pie, o para que
    // el sistema le mande la dirección al cliente por WhatsApp).
    address: 'Bolívar 3008',

    // Referencia para llegar (entre calles, barrio).
    addressExtra: '',

    // Link de búsqueda en el mapa por la dirección.
    mapUrl: 'https://www.google.com/maps/search/?api=1&query=' +
            encodeURIComponent('Bolívar 3008'),

    // ── Email ──────────────────────────────────────────────────────────
    // Todavía no está confirmado. Si queda vacío, el sitio oculta la
    // fila del email en lugar de mostrar un enlace roto.
    email: '',

    // ── WhatsApp como texto flotante ──────────────────────────────────
    whatsappFloat: true
  },

  // -----------------------------------------------------------------------
  // Horario de atención — ✅ DATO REAL, confirmado por el dueño
  // -----------------------------------------------------------------------
  // El panel del dueño todavía no administra los horarios. Por ahora se
  // editan acá, en un solo lugar.
  //
  // Para cambiar un horario: modificá open/close. Para cerrar un día,
  // poné closed: true.
  hours: [
    { day: 'Lunes',     short: 'Lun', open: '09:00', close: '21:00', closed: false },
    { day: 'Martes',    short: 'Mar', open: '09:00', close: '21:00', closed: false },
    { day: 'Miércoles', short: 'Mié', open: '09:00', close: '21:00', closed: false },
    { day: 'Jueves',    short: 'Jue', open: '09:00', close: '21:00', closed: false },
    { day: 'Viernes',   short: 'Vie', open: '09:00', close: '21:00', closed: false },
    { day: 'Sábado',    short: 'Sáb', open: '09:00', close: '21:00', closed: false },
    { day: 'Domingo',   short: 'Dom', open: null,   close: null,    closed: true  }
  ],

  // -----------------------------------------------------------------------
  // Servicios — datos REALES
  // -----------------------------------------------------------------------
  // El panel del dueño todavía no administra el catálogo. Por ahora se
  // edita acá, en un solo lugar.
  //
  //   price -> precio real
  //   duration -> duración NOMINAL. NO se muestra al cliente, porque un
  //     corte no dura siempre lo mismo: depende del pelo, de cuánto hace
  //     que el cliente no se corta y de lo que pida.
  //
  //     Pero el sistema la necesita igual: es lo que le dice qué cantidad
  //     de bloques de 30 minutos ocupa el turno en la agenda. Si un corte
  //     "nominalmente" dura 30, el sistema reserva 1 bloque de 30.
  //
  //     Ojo con esto: si el pelo exige 50 minutos y el turno era de 30,
  //     el barbero se va a pasar de hora. La agenda no extiende el turno
  //     automáticamente; esa situación se maneja en el local.
  //
  // Para marcar un servicio como "Más pedido": agregale
  //   popular: true
  services: [
    {
      id: 1,
      name: 'Corte clásico',
      description: 'Corte a tijera y máquina, incluye lavado y peinado.',
      duration: 30,
      price: 12000
    },
    {
      id: 2,
      name: 'Barba completa',
      description: 'Afilado con toalla, navaja y aceite.',
      duration: 30,
      price: 12000
    },
    {
      id: 3,
      name: 'Corte + barba',
      description: 'Corte y barba completos.',
      duration: 60,
      price: 15000
    },
    {
      id: 4,
      name: 'Corte infantil',
      description: 'Para los más chicos, con paciencia incluida.',
      duration: 30,
      price: 9000
    }
  ],

  // -----------------------------------------------------------------------
  // Galería — 4 FOTOS REALES
  // -----------------------------------------------------------------------
  // Para agregar más: copiá la imagen a static/assets/img/ y sumá un objeto.
  //
  //   title -> lo que se escribe sobre la foto
  //   src   -> ruta de la imagen
  //
  // Si a una entrada le falta el "src", el sitio dibuja un marcador de
  // posición en su lugar. Ahora no hay ninguno: las cuatro entradas tienen
  // su foto.
  gallery: [
    {
      title: 'Degradado bajo con barba',
      src: 'assets/img/corte-degradado-barba.jpg'
    },
    {
      title: 'Fade con textura',
      src: 'assets/img/corte-fade-texturizado.jpg'
    },
    {
      title: 'Corte rizado con degradado',
      src: 'assets/img/corte-rizado-degradado.jpg'
    },
    {
      title: 'Corte con flequillo',
      src: 'assets/img/corte-flequillo.jpg'
    }
  ],

  // -----------------------------------------------------------------------
  // Reglas de la agenda
  // -----------------------------------------------------------------------
  // Coinciden con las variables de .env (ver docs/arquitectura.md, sección 5).
  booking: {
    slotMinutes: 30,
    minLeadMinutes: 120,
    maxAdvanceDays: 60,
    timezone: 'America/Argentina/Buenos_Aires'
  }
};
