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
- RF06 - Niveles de aprendizaje:
  - Ya no existe una pantalla de "Selecciona tu nivel": `CursoViewModel.cargarCurso()`
    lee la colección `niveles` (ordenada por `orden`, solo `activo == true`) y
    **auto-selecciona el primer nivel** (Básico en los datos actuales), consultando
    directo sus unidades (`nivelId` coincide) y mostrando el mapa de unidades/lecciones.
  - La colección Firestore `niveles` sigue existiendo tal cual (Básico/Intermedio/
    Avanzado); solo se eliminó la UI manual de elección y el botón "← Niveles" para
    volver a ella. Intermedio y Avanzado quedan simplemente sin usar hasta que RF14
    implemente progreso real y una forma de cambiar de nivel.
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

## Flujo actual de RF06

```text
INICIO -> (auto-selecciona el primer nivel activo) -> Todas las unidades y lecciones del nivel
```

No existe pantalla de selección de nivel ni de selección de unidades.

## Pendientes relacionados

- RF14 debe definir y registrar la finalización real de lecciones antes de habilitar niveles posteriores.
- Cuando RF14 esté disponible, deberá persistir en `leccionesCompletadasIds` las claves `unidadId/leccionId` y el desbloqueo de niveles deberá reutilizar ese progreso.
- El resultado del "Test de nivel" del onboarding (aciertos sobre `ActividadesPruebaNivel`) todavía no determina el nivel inicial real del usuario; queda para una iteración futura.
- Faltan pruebas unitarias e instrumentadas específicas para el curso.

## Verificación

- `gradlew.bat assembleDebug`: compilación correcta el 15 de octubre de 2026.
- `./gradlew assembleDebug`: compilación correcta el 16 de octubre de 2026 tras el rediseño del onboarding (bienvenida + elección de nivel).
- `./gradlew assembleDebug`: compilación correcta el 16 de octubre de 2026 tras eliminar la pantalla "Selecciona tu nivel" de RF06 (auto-selección del primer nivel).
- `./gradlew assembleDebug`: compilación correcta el 17 de octubre de 2026 tras agregar el tipo `ASOCIAR` y el registro de respuestas (RF08).
