# Estado del proyecto QchuaGo

Última actualización: 17 de octubre de 2026.

## Arquitectura y tecnología

- Aplicación Android en Kotlin con Jetpack Compose y Material 3.
- Navegación principal centralizada en `AppNavigation.kt` y pestañas dentro de `HomeScreen.kt`.
- MVVM básico: servicios de Firebase, ViewModels y pantallas Compose.
- Firebase Authentication para identidad y Cloud Firestore para perfiles y contenido del curso.
- Compatibilidad mínima: Android 10 (API 29).

## Funcionalidades presentes

- RF01-RF03: autenticación, registro y sesión existentes; no fueron modificados por RF06.
- Onboarding post-autenticación:
  - `Usuario.onboardingCompletado` (Firestore, por defecto `false`) indica si el
    usuario ya pasó por el flujo de bienvenida.
  - `Rutas.destinoTrasAutenticar(usuario)` centraliza la decisión de navegación tras
    login/registro: si `onboardingCompletado == false` va a `Rutas.BIENVENIDA`; si
    no, a `Rutas.HOME` (comportamiento usado por `AppNavigation`, `LoginScreen` y
    `RegisterScreen`).
  - `BienvenidaScreen` (ruta `bienvenida`): pantalla estática de saludo
    ("¡Allin p'unchay, {nombre}!") con botón "¡Empezar! →" hacia `elegir_nivel`. Solo
    se ve una vez por usuario.
  - `ElegirNivelScreen` (ruta `elegir_nivel`): "Principiante" marca
    `onboardingCompletado = true` y navega directo a `Rutas.HOME` (pantalla de
    niveles existente); "Test de nivel" navega a `Rutas.PRUEBA_NIVEL`.
  - `Rutas.PRUEBA_NIVEL` reutiliza `LeccionScreen`/`LeccionViewModel` con una lista
    de `Actividad` estática en memoria (`ActividadesPruebaNivel`, sin Firestore) a
    modo de demo. Al terminar o salir, siempre marca `onboardingCompletado = true` y
    continúa a `Rutas.HOME`. **Pendiente**: la selección real de nivel según
    aciertos del test (hoy no se evalúa el resultado para desbloquear nada).
- Pantalla Home con las pestañas Inicio, Progreso, Diccionario, Repaso y Perfil.
- Perfil de usuario, estadísticas y preferencia de tema.
- Curso cargado desde Firestore con unidades y subcolecciones de lecciones.
- RF06 - Niveles de aprendizaje (**aclaración de alcance, 17 de octubre de 2026**: por
  "nivel" el requerimiento se refiere a cada paso/lección dentro de una misma unidad —
  ej. completar la lección de saludo desbloquea la siguiente de esa unidad — **no** a
  las dificultades Básico/Intermedio/Avanzado de la colección Firestore `niveles`, que
  es un concepto distinto y nunca fue parte de este requerimiento):
  - **Cumplido.** El desbloqueo secuencial lección-a-lección (y unidad-a-unidad) ya
    estaba implementado por RF07 en `CursoViewModel.estaDesbloqueada` /
    `estaDesbloqueadaUnidad` / `estaCompletada`, pero quedaba inerte porque nada
    marcaba `Usuario.leccionesCompletadasIds`. RF14 (ver más abajo) cerró ese hueco:
    ahora, al terminar una lección real, `AppNavigation` agrega su clave
    `unidadId/leccionId` a esa lista, lo que activa el desbloqueo de la siguiente
    lección/unidad automáticamente (reactivo vía `StateFlow`, sin recargar la app).
  - Ya no existe una pantalla de "Selecciona tu nivel" (Básico/Intermedio/Avanzado):
    `CursoViewModel.cargarCurso()` lee la colección `niveles` (ordenada por `orden`,
    solo `activo == true`) y **auto-selecciona el primer nivel**, consultando directo
    sus unidades (`nivelId` coincide) y mostrando el mapa de unidades/lecciones. La
    colección Firestore `niveles` sigue existiendo tal cual (Básico/Intermedio/
    Avanzado) por si se retoma más adelante, pero Intermedio y Avanzado no son
    alcanzables desde la UI — eso es una decisión aparte, no forma parte de RF06.
- RF07 - Unidades y temas:
  - Al seleccionar un nivel se muestran directamente todas sus unidades y lecciones, incluidas las bloqueadas.
  - Las unidades se ordenan por `orden` y cada subcolección de lecciones se consulta ordenada por `orden`.
  - El desbloqueo es secuencial: una unidad requiere completar todas las anteriores y una lección requiere completar las que la preceden.
  - Las lecciones se identifican para el progreso mediante la clave `unidadId/leccionId`, evitando colisiones entre documentos de distintas unidades con el mismo ID.
  - No existe acceso al contenido educativo porque RF05 todavía no está implementado.
- RF08 - Ejercicios interactivos y registro de respuestas:
  - Nuevo tipo de actividad `ASOCIAR` (asociación de palabras): reutiliza los campos
    existentes de `Actividad` sin agregar ninguno nuevo. `elementos` son las palabras en
    quechua (columna izquierda, en el orden a mostrar); `opciones` son las traducciones
    en español ya mezcladas (columna derecha); `respuestasCorrectas[i]` es la traducción
    correcta de `elementos[i]` (no necesariamente en el mismo orden que aparece en
    `opciones`). Interacción tap-based en `TarjetaAsociar` (sin drag-and-drop ni
    librerías nuevas): tocar un elemento izquierdo lo selecciona, tocar una opción
    derecha completa el par, tocar un par ya formado lo deshace. El modelo mantiene el
    espejo 1:1 con el admin web (`QchuaGo-Admin/src/types.ts` y `ActividadesPage.tsx`),
    que ya permite crear actividades de este tipo.
  - `LeccionViewModel.TIPOS_EVALUABLES` ahora incluye `ASOCIAR` junto a
    SELECCION/COMPLETAR/ORDENAR.
  - Registro de respuestas del usuario: `FirestoreService.registrarRespuesta` escribe en
    `usuarios/{uid}/respuestas/{unidadId_leccionId_actividadId}` si una respuesta de
    cualquier tipo evaluable fue correcta o incorrecta. Se dispara desde
    `LeccionViewModel.comprobar()` de forma fire-and-forget (no bloquea la lección ni
    muestra error si falla) y solo cuando la lección proviene de Firestore
    (`actividadesEstaticas == null`) y existe un `uid` válido. El modo "Test de nivel"
    (`Rutas.PRUEBA_NIVEL`, datos estáticos en memoria) nunca escribe nada.
  - `AppNavigation` pasa `authViewModel.usuario?.uid` a `LeccionScreen` solo en la ruta
    real `Rutas.LECCION`; `LeccionViewModel` no depende de `AuthViewModel`/`AuthService`,
    solo recibe el `uid` como `String?` simple.
  - `TarjetaCompletar` ahora muestra, cuando `actividad.opciones` no está vacío, un
    "Banco de palabras" (chips reutilizando el composable `Chip` ya usado por
    `ORDENAR`) debajo del campo de texto; tocar una palabra la escribe completa en el
    campo (reutiliza `onCambiarTexto`, sin cambios en `LeccionViewModel`). Si una
    actividad `COMPLETAR` no tiene `opciones`, el campo sigue siendo texto libre como
    antes (compatible con contenido existente).
- RF14 - Seguimiento del progreso:
  - Una lección real (`Rutas.LECCION`, no la de "Test de nivel") queda marcada como
    completada al llegar a `EstadoLeccion.Finalizada` y presionar "Volver" en
    `ResultadoLeccion` (reaching the end cuenta como completar, sin importar el
    puntaje — igual que ya hacía el onboarding con el test de nivel). `LeccionScreen`
    expone esto cambiando `onSalir` a `(completada: Boolean) -> Unit`; `AppNavigation`
    agrega la clave `unidadId/leccionId` a `Usuario.leccionesCompletadasIds` (vía
    `AuthViewModel.actualizarUsuario`, evitando duplicados) y sincroniza
    `leccionesCompletadas` con el tamaño de esa lista. Salir sin terminar (✕ o error)
    nunca marca nada.
  - `ProgresoScreen.kt` dejó de ser un placeholder: reutiliza el mismo
    `CursoViewModel` que `InicioScreen` (comparten `ViewModelStoreOwner` dentro de
    `HomeScreen`, así que comparten el curso ya cargado y la lógica de
    bloqueo/completado de RF07 sin duplicarla). Muestra: porcentaje de avance del
    nivel actual, lecciones y unidades completadas, una tarjeta "Continuar
    aprendiendo" que navega directo a la siguiente lección pendiente (mismo cálculo
    de `siguienteLeccion` que ya usaba `InicioScreen` para resaltar la burbuja "¡!"),
    y el detalle de avance por unidad.
  - **Alcance deliberado**: el porcentaje se calcula solo para el nivel accesible hoy
    (el que auto-selecciona `CursoViewModel`). Intermedio y Avanzado (las dificultades
    de la colección `niveles`, no relacionadas con RF06) no tienen unidades cargadas
    en Firestore todavía y no son alcanzables en la UI, así que no se les calcula
    progreso por separado; si en el futuro se decide hacerlos alcanzables,
    `ProgresoScreen` tendría que iterar sobre varios niveles en lugar de uno solo.
  - No se construyó una pantalla de historial de respuestas individuales (eso viviría
    sobre `usuarios/{uid}/respuestas` de RF08); "resultados" se cubre con los
    porcentajes y conteos agregados.

## Flujo actual al entrar a Inicio

```text
INICIO -> (auto-selecciona el primer nivel activo de la colección `niveles`) ->
Todas las unidades y lecciones de ese nivel, con desbloqueo secuencial lección a
lección y unidad a unidad (RF06+RF07+RF14)
```

No existe pantalla de selección de nivel (Básico/Intermedio/Avanzado) ni de selección
de unidades — eso nunca fue parte de RF06; RF06 es el desbloqueo secuencial dentro del
mapa de unidades/lecciones, que ya funciona de punta a punta.

## Pendientes relacionados

- Intermedio y Avanzado (dificultades de la colección `niveles`) siguen sin contenido
  y sin ser alcanzables desde la UI; es una decisión explícita del usuario al
  simplificar la selección de nivel, no un requerimiento pendiente de RF06.
- El resultado del "Test de nivel" del onboarding (aciertos sobre `ActividadesPruebaNivel`) todavía no determina el nivel inicial real del usuario; queda para una iteración futura.
- Faltan pruebas unitarias e instrumentadas específicas para el curso.

## Verificación

- `gradlew.bat assembleDebug`: compilación correcta el 15 de octubre de 2026.
- `./gradlew assembleDebug`: compilación correcta el 16 de octubre de 2026 tras el rediseño del onboarding (bienvenida + elección de nivel).
- `./gradlew assembleDebug`: compilación correcta el 16 de octubre de 2026 tras eliminar la pantalla "Selecciona tu nivel" de RF06 (auto-selección del primer nivel).
- `./gradlew assembleDebug`: compilación correcta el 17 de octubre de 2026 tras agregar el tipo `ASOCIAR` y el registro de respuestas (RF08).
- `./gradlew assembleDebug`: compilación correcta el 17 de octubre de 2026 tras implementar el marcado de lecciones completadas y la pantalla de Progreso real (RF14).
