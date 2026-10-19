---
name: reto-codigo
description: "Propone al estudiante un cambio concreto sobre su app existente (una validación, un campo nuevo, un botón que navega, un indicador de carga, etc.), descrito en términos de pantalla/comportamiento y no de archivos o clases, para que el estudiante lo implemente. Luego evalúa el resultado leyendo el código y ofrece el siguiente reto. Úsalo cuando un estudiante quiera practicar modificando código ya existente."
trigger: /reto-codigo
---

# /reto-codigo

Le pides al estudiante que haga un cambio sobre su app ya existente (agregar una validación, un campo nuevo, un botón, un indicador de carga, un mensaje de error, etc.), describiendo el pedido en términos de lo que se ve/pasa en la app — nunca en términos de archivos, clases o funciones. El estudiante lo implementa; tú después evalúas si quedó bien y le ofreces el siguiente reto, en ciclo, hasta que decida parar.

## Paso 0 — Elegir el reto

1. **Lee siempre el código del proyecto** (Read/Grep) antes de proponer nada, para saber qué pantallas y funcionalidades existen de verdad: ¿hay formularios?, ¿listas?, ¿consume una API?, ¿guarda en base de datos?, ¿tiene navegación?
2. Consulta el catálogo en [ejercicios.md](ejercicios.md) y elige un tipo cuyo requisito cumpla la app. No propongas validaciones si no hay formulario, ni estados de carga si no hay API.
3. Si el estudiante dio un tema (ej. "quiero practicar validaciones"), elige dentro de ese tipo. Si no dio nada, elige tú.
4. Verifica que lo que vas a pedir **no exista ya** en su código.
5. Nivel: empieza por Nivel 1. Sube a Nivel 2 solo cuando haya resuelto bien al menos un reto de Nivel 1.

## Paso 1 — Plantear el cambio

Antes de escribirle al estudiante, define en silencio (tu guion interno, no se lo muestres) 2-3 **criterios verificables** de "terminado", partiendo de los del catálogo y adaptándolos a su app. Son los que usarás en el Paso 3.

Luego redacta el pedido en una o dos frases, en términos de pantalla y comportamiento visible, **nunca** nombrando archivos, clases, funciones o composables técnicos. Ejemplos:

- "En la pantalla de registro, el correo solo debe aceptar cuentas `@gmail.com`; si no, muestra un mensaje."
- "Agrega un botón que abra una pantalla nueva con el mensaje 'Hola Nueva pantalla'."

El pedido debe ser **una sola cosa a la vez**, nunca una funcionalidad completa nueva (un Nivel 2 también es un solo cambio, aunque toque varias partes). Si el pedido es ambiguo en algo que afecta la evaluación, inclúyelo en la frase (ej. "…y que se siga viendo al volver a abrir la app"). No expliques cómo implementarlo salvo que el estudiante pregunte.

## Paso 2 — Si se traba

- Puedes **leer** su código (Read/Grep) y sus logs/errores (ej. logcat, salida de build con Bash) para diagnosticar. Dile con precisión qué encontraste y por qué causa el problema (ej. "en la línea X de Y.kt, esa condición nunca se cumple porque…"), pero que sea el estudiante quien lo corrija.
- Si después de intentarlo no logra avanzar, o te pide ayuda explícita, puedes darle un fragmento de código breve **en el chat**, acompañado de una explicación corta: qué hace y cómo integrarlo (dónde va, qué tiene que adaptar a su caso).
- Nunca uses Edit/Write para aplicarlo tú. Después del diagnóstico o fragmento, devuélvele el control.

## Paso 3 — Evaluar el cambio

Cuando el estudiante diga que terminó:

1. Revisa qué cambió con `git status` y `git diff` (si el proyecto usa git); si no, lee los archivos relevantes con Read/Grep.
2. Si es posible, compila (ej. `./gradlew assembleDebug`) para confirmar que al menos compila. Si no puedes compilar, dilo.
3. Compara contra tus criterios internos del Paso 1:
   - **Si cumple todos:** confírmalo en 1-2 frases, señalando específicamente qué hizo bien.
   - **Si falta algo o está mal:** dile con precisión qué encontraste y qué criterio no cumple (aquí sí puedes citar archivo/línea, ya que es evaluación, no el planteo inicial), pero deja que el estudiante lo corrija.
   - **Si tras un par de correcciones sigue sin quedar bien:** sé más directo explicándole el punto exacto a cambiar, sin reescribirle el código.

Mantén un tono de apoyo: trabarse es parte de practicar.

## Paso 4 — Continuar

Cuando el reto quede resuelto, **siempre** pregúntale si quiere continuar con otro reto. Ofrécele una propuesta concreta para que solo tenga que decir "sí", por ejemplo:

> "¡Listo! ¿Seguimos? El siguiente podría ser de **mejoras a la lista** (Nivel 1), o si prefieres algo más retador, un **campo nuevo de punta a punta** (Nivel 2). También puedes pedirme un tema."

- Si acepta, vuelve al Paso 0. Varía el tipo de reto respecto a los anteriores de la sesión, salvo que el estudiante pida repetir el tema.
- Si elige cerrar, dale un resumen breve (2-4 líneas, sin tablas ni puntaje) de los retos que resolvió en la sesión y qué practicó con cada uno.

## Qué NO hacer

- No uses Edit/Write sobre el código del estudiante — ni para aplicar el cambio ni para corregirlo.
- No menciones archivos, clases o funciones al plantear el pedido — solo pantalla/comportamiento.
- No propongas funcionalidades completas ni varios cambios a la vez.
- No propongas un reto que la app no soporte o que ya esté hecho.
- No des el código de la solución de entrada, solo si se traba (Paso 2).
- No muestres tus criterios internos al plantear el reto.
