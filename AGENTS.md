# AGENTS.md — BarberTrend AI

## 1. Contexto del proyecto

BarberTrend AI es una aplicación web para una barbería masculina que combina dos funcionalidades principales:

1. Gestión y reserva de turnos online.
2. Asistente de marketing con IA para detectar tendencias y generar contenido para Instagram.

El proyecto debe desarrollarse de forma gradual, priorizando una primera versión funcional, simple y económica.

El usuario es estudiante de Ingeniería en Sistemas y quiere comprender las decisiones técnicas y el código generado. Explicá las decisiones de forma clara, sin asumir experiencia profesional avanzada.

## 2. Objetivos prioritarios

Orden de desarrollo:

1. Diseñar la web pública de la barbería.
2. Implementar el sistema de reservas.
3. Crear un panel administrativo privado.
4. Integrar confirmaciones por WhatsApp.
5. Incorporar el asistente de marketing con IA.
6. Automatizar recomendaciones y recordatorios.
7. Evaluar el despliegue y los costos operativos.

No implementar todas las etapas simultáneamente.

## 3. Tecnologías

Preferencias iniciales:

- Frontend: HTML, CSS y JavaScript.
- Backend: Java y Spring Boot.
- Base de datos: evaluar PostgreSQL para producción y alternativas sencillas para desarrollo.
- API: REST.
- Control de versiones: Git.
- Mensajería futura: WhatsApp Business Platform Cloud API oficial.
- IA futura: integración desacoplada de un proveedor específico.

Antes de incorporar una dependencia o servicio nuevo, explicar por qué es necesario.

No agregar React, microservicios, Redis, Docker, servicios pagos u otras tecnologías sin una justificación concreta.

Verificar las versiones disponibles y su compatibilidad antes de definirlas.

## 4. Forma de trabajo

Antes de modificar el proyecto:

1. Inspeccionar los archivos existentes.
2. Leer este archivo y MEMORY.md.
3. Consultar FEATURES.md para identificar las funcionalidades pendientes.
4. Comprender la estructura actual y evitar sobrescribir trabajo existente.
5. Proponer un plan breve cuando la tarea sea importante.

Implementar por etapas y mantener los cambios enfocados en la solicitud actual.

Después de cada etapa:

- Ejecutar las pruebas pertinentes.
- Corregir errores detectados.
- Explicar los archivos creados o modificados.
- Indicar cómo ejecutar y comprobar los cambios.
- Actualizar FEATURES.md cuando cambie el estado real de una funcionalidad.

No afirmar que una funcionalidad está terminada o probada sin haberlo verificado.

## 5. Código y arquitectura

- Mantener el código simple, modular y legible.
- Utilizar nombres descriptivos.
- Separar responsabilidades.
- Evitar duplicaciones innecesarias.
- Validar las entradas del usuario.
- Manejar errores de manera explícita.
- Aplicar principios de diseño cuando aporten valor real.
- No introducir abstracciones innecesarias para funcionalidades todavía inexistentes.

En el backend, mantener separadas las responsabilidades de controladores, servicios y acceso a datos cuando corresponda.

En el frontend, priorizar accesibilidad, responsive design y una experiencia sencilla.

## 6. Reservas y disponibilidad

El sistema debe evitar las reservas duplicadas.

La disponibilidad debe verificarse en el backend, no solamente en JavaScript.

Considerar solicitudes simultáneas, duración de los servicios, horarios de atención, cancelaciones y modificaciones.

No permitir que el usuario público acceda a información privada de otros clientes.

Los datos de ejemplo deben identificarse como tales.

No inventar el nombre comercial, los precios, los horarios ni la dirección real de la barbería.

## 7. Seguridad

- No incluir credenciales reales en el código.
- No subir archivos `.env` con secretos a Git.
- Proporcionar `.env.example` sin credenciales reales.
- Proteger las rutas administrativas mediante autenticación y autorización.
- Validar datos en el servidor.
- Evitar exponer números de teléfono y datos personales.
- Verificar las firmas y los mecanismos de autenticación de webhooks cuando corresponda.
- Evitar duplicar mensajes o procesar varias veces un mismo evento.
- Aplicar prácticas de seguridad adecuadas al framework utilizado.

No implementar un panel administrativo público sin protección.

## 8. WhatsApp e integraciones externas

Usar la API oficial cuando sea viable.

Antes de integrar un servicio, investigar sus requisitos, permisos, límites y costos vigentes.

Separar la lógica del negocio del proveedor externo para poder reemplazarlo.

Implementar un modo de prueba que no envíe mensajes reales.

Nunca simular que un mensaje fue enviado si no existe confirmación de la integración correspondiente.

Considerar las reglas de mensajería, plantillas y ventanas de atención aplicables.

## 9. Inteligencia artificial y tendencias

La IA debe generar contenido a partir de información disponible, no inventar tendencias.

Distinguir entre:

- Datos verificados.
- Estimaciones.
- Recomendaciones creativas.
- Información que no pudo obtenerse.

No afirmar que un video es viral sin evidencia suficiente.

No inventar enlaces, estadísticas, visualizaciones ni fechas.

Respetar los permisos, condiciones de uso y limitaciones de las fuentes consultadas.

Permitir que el sistema siga funcionando con datos de prueba si las APIs no están configuradas.

## 10. Costos y despliegue

Priorizar el desarrollo local y las herramientas gratuitas durante la etapa inicial.

No contratar servicios, crear cuentas, realizar pagos ni consumir APIs pagas sin autorización explícita del usuario.

Antes de desplegar, comparar las opciones de alojamiento y explicar sus costos y limitaciones.

No asumir que una base de datos local estará disponible cuando la aplicación se despliegue.

## 11. Diseño de interfaz

La aplicación debe tener una estética profesional apropiada para una barbería masculina.

Prioridades:

- Diseño responsive.
- Navegación clara.
- Contraste y legibilidad.
- Botones y formularios intuitivos.
- Estados de carga, éxito y error.
- Accesibilidad básica.
- Rendimiento razonable en celulares.

No usar datos, testimonios ni fotografías que puedan confundirse con información real de la barbería sin autorización.

## 12. Comunicación

Responder en español, de forma clara y natural.

Explicar conceptos técnicos con ejemplos cuando sea útil.

Evitar respuestas excesivamente teóricas.

Cuando existan varias alternativas, recomendar una y explicar brevemente el motivo.

Si falta un dato importante, preguntar antes de tomar una decisión irreversible. En los demás casos, avanzar con supuestos explícitos y fáciles de cambiar.

## 13. Definición de terminado

Una funcionalidad se considera terminada cuando:

- Cumple el comportamiento esperado.
- Tiene validaciones y manejo de errores apropiados.
- Se han ejecutado las pruebas pertinentes.
- No introduce regresiones conocidas.
- La documentación necesaria está actualizada.
- Se puede comprobar siguiendo instrucciones reproducibles.