---
description: Analiza el progreso real del proyecto y recomienda el siguiente paso
agent: plan
---

Quiero conocer el estado actual del proyecto BarberTrend AI y saber por dónde continuar.

Antes de responder:

1. Lee `AGENTS.md`, `MEMORY.md` y `feature1.md`, si existen.
2. Inspecciona la estructura actual del proyecto y los archivos relevantes.
3. Compara las tareas de `feature1.md` con el código realmente implementado.
4. Revisa las pruebas disponibles y las evidencias de funcionamiento cuando sea posible.
5. Identifica las diferencias entre el progreso registrado y el estado real del proyecto.

Presenta un informe con las siguientes secciones:

## 1. Estado general
- Fase actual de la hoja de ruta.
- Porcentaje aproximado de avance, calculado a partir de las tareas verificadas.
- Breve explicación de por qué el proyecto se encuentra en ese punto.

## 2. Resumen por fase
Para cada fase de `feature1.md`, indica:
- Tareas completadas y verificadas. Marcalas como tal
- Tareas implementadas que todavía necesitan pruebas.
- Tareas en progreso.
- Tareas pendientes.
- Tareas cuyo estado no se puede determinar con la evidencia disponible.

## 3. Próximo paso recomendado
- Indica qué tarea conviene realizar a continuación.
- Explica por qué es prioritaria.
- Señala si existen dependencias o tareas previas que deban completarse.

## 4. Problemas y bloqueos
- Identifica errores, dependencias faltantes, decisiones pendientes, riesgos técnicos o posibles costos externos.
- Distingue los bloqueos reales de las mejoras opcionales.

## 5. Documentación desactualizada
- Señala si `feature1.md`, `MEMORY.md` o `AGENTS.md` necesitan actualizarse.
- Explica qué información debería corregirse o añadirse.

## Reglas importantes
- No modifiques archivos ni implementes funcionalidades.
- No marques una tarea como completada solo porque exista un archivo o código relacionado.
- Considera una tarea completada únicamente cuando exista evidencia razonable de que funciona y cumple sus requisitos.
- Si no puedes verificar algo, indícalo explícitamente.
- No inventes avances, pruebas, funcionalidades ni porcentajes.
- Si el proyecto todavía está en preparación, indícalo sin asumir que las fases posteriores comenzaron.
- Respeta las reglas de `AGENTS.md` y utiliza `FEATURES.md` como referencia principal para la hoja de ruta.

Finaliza con una lista breve de las próximas tres tareas recomendadas, en orden de prioridad.