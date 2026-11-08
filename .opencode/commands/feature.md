---
description: Planifica una nueva funcionalidad antes de tocar código
agent: plan
---

Quiero añadir esta funcionalidad: $ARGUMENTS

Antes de escribir o modificar código, analiza el proyecto existente y prepárame un plan de implementación.

Debes seguir estas indicaciones:

1. **Analizar el contexto**
   - Leer `AGENTS.md` y `MEMORY.md`, si existen.
   - Revisar la estructura actual del proyecto y los archivos relacionados con la funcionalidad.
   - Consultar `feature1.md` para comprobar cómo encaja la funcionalidad en la hoja de ruta.
   - Identificar las tecnologías, convenciones y funcionalidades existentes que deben respetarse.

2. **Explicar la implementación**
   - Describir cómo implementarías la funcionalidad paso a paso.
   - Respetar las reglas de `AGENTS.md` y reutilizar el código existente cuando corresponda.
   - Priorizar una solución sencilla, mantenible y coherente con la arquitectura actual.
   - Identificar dependencias externas, costos potenciales y limitaciones técnicas, si los hubiera.

3. **Identificar los archivos afectados**
   - Enumerar los archivos existentes que modificarías y explicar cada cambio.
   - Indicar los archivos nuevos que sería necesario crear y justificar su necesidad.
   - No proponer cambios innecesarios ni asumir que existen archivos que todavía no se han creado.

4. **Analizar casos límite y dudas**
   - Identificar errores posibles, validaciones, casos límite y aspectos de seguridad.
   - Señalar las decisiones que necesito tomar antes de implementar la funcionalidad.
   - Diferenciar las dudas que realmente bloquean la implementación de aquellas que admiten una decisión técnica razonable.

5. **Definir las pruebas**
   - Indicar qué pruebas unitarias, de integración o manuales serían necesarias.
   - Explicar cómo comprobarías que la funcionalidad funciona y que los cambios no rompen lo existente.

6. **Proponer actualizaciones de documentación**
   - Explicar si sería necesario actualizar `AGENTS.md`, `MEMORY.md`, `FEATURES.md` o el `README.md`.
   - Para cada archivo, especificar qué información agregarías o modificarías.
   - No modificar estos archivos durante esta etapa.

## Formato de respuesta

Presenta el resultado con estas secciones:

1. Resumen de la funcionalidad.
2. Análisis del estado actual.
3. Plan de implementación paso a paso.
4. Archivos que se crearán o modificarán.
5. Casos límite, riesgos y decisiones pendientes.
6. Plan de pruebas.
7. Actualizaciones de documentación propuestas.

Si falta información, explicita los supuestos y continúa con un plan provisional siempre que sea posible.

**Importante:** esta etapa es exclusivamente de planificación. No escribas ni modifiques código, no instales dependencias y no ejecutes cambios en el proyecto. Espera mi aprobación antes de comenzar la implementación.