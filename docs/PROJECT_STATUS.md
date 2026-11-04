# Estado del proyecto QchuaGo

Última actualización: 4 de noviembre de 2026.

## Arquitectura y tecnología

- Aplicación Android en Kotlin con Jetpack Compose y Material 3.
- Navegación principal centralizada en `AppNavigation.kt` y pestañas dentro de `HomeScreen.kt`.
- MVVM de 2 capas: ViewModels y pantallas Compose. **Ya no existe `data/servicio/`**
  (ver sección "Eliminación de la capa Service" más abajo): el ViewModel llama
  directamente a Firebase.
- Firebase Authentication para identidad y Cloud Firestore para perfiles y contenido del curso.
- Compatibilidad mínima: Android 10 (API 29).
- `Actividad.tipo` es un `enum class TipoActividad` (no `String`) desde la auditoría del
  17 de octubre de 2026 — ver sección "Auditoría de código" más abajo.
- Las tarjetas de ejercicio de `LeccionScreen.kt` viven en el subpaquete
  `ui/screens/leccion/` (una por tipo), con `LeccionScreen.kt` como orquestador.
- `LeccionScreen.kt` usa `.systemBarsPadding()` en su contenedor raíz (17 de octubre
  de 2026).
- No existe el concepto de "niveles" de dificultad (Básico/Intermedio/Avanzado): se
  eliminó por completo el 17 de octubre de 2026 (ver sección RF06 más abajo). El
  curso es únicamente `unidades` → `lecciones` → `actividades`.

## Eliminación de la capa Service (26 de octubre de 2026)

Cambio de arquitectura solicitado explícitamente: se eliminó por completo
`data/servicio/` para que el flujo quede en solo 2 capas, `ViewModel <-> Composable`,
sin intermediario.

- **`AuthService.kt` y `FirestoreService.kt` → `AuthViewModel.kt`:** el ViewModel
  ahora tiene sus propias instancias de `FirebaseAuth`/`FirebaseFirestore` y hace las
  llamadas directamente (login, registro, actualizar nombre/contraseña, obtener o
  crear el perfil en Firestore). Mismo `Result<T>`, mismos mensajes de error en
  español y misma lógica de reversión optimista de `actualizarUsuario`.
- **`CursoService.kt` (unidades) → `CursoViewModel.kt`:** `cargarCurso()` consulta
  Firestore directamente.
- **`CursoService.kt` (actividades) y `FirestoreService.registrarRespuesta` →
  `LeccionViewModel.kt`:** `cargarActividades()` y el registro de respuestas
  (fire-and-forget) se ejecutan directamente desde el ViewModel.
- **`AudioService.kt` → `LeccionViewModel.kt`:** la reproducción/grabación con
  `MediaPlayer`/`MediaRecorder` pasó a ser lógica privada del ViewModel; ahora recibe
  un `Context` en el constructor en vez de un `AudioService` ya construido.
- **`FirebaseExt.kt`** se reubicó a `ui/viewmodel/` y luego se **eliminó** el 4 de
  noviembre de 2026 (ver sección siguiente): ahora se usa `.await()` oficial.
- `LeccionScreen.kt` ya no construye un `AudioService`; su `ViewModelProvider.Factory`
  manual se reemplazó por el DSL `viewModelFactory { initializer { ... } }` de
  `androidx.lifecycle.viewmodel` (ya era dependencia del proyecto).
- `AGENTS.md` se actualizó para reflejar la nueva arquitectura (ya no menciona
  `data/servicio/` ni la regla de "todo acceso a Firebase vía Servicios").
- Verificado con `./gradlew assembleDebug` (compila sin errores).

## Fases 1-3 del plan de mejoras (4 de noviembre de 2026)

Ver `docs/PLAN_MEJORAS.md`. Sin cambios de funcionalidad:

- **Componentes comunes:** nuevo `ui/screens/ComponentesComunes.kt` con
  `PantallaCargando`, `PantallaError` (con `onVolver` opcional) y
  `ContenidoRecurso(recurso, onReintentar) { datos -> ... }`. Reemplazan los bloques
  copiados en `InicioScreen`, `ProgresoScreen` y `LeccionScreen` (se borraron
  `CargandoLeccion`/`ErrorLeccion`). Pequeño cambio visual: el error de Progreso ahora
  muestra el ⚠️ como el de Inicio.
- **Navegación:** `Rutas.LECCION_PATRON` y `Rutas.leccion(unidadId, leccionId)`; ya no se
  arma la ruta con strings en las pantallas. `InicioScreen`/`ProgresoScreen` reciben
  `onAbrirLeccion: (Unidad, Leccion) -> Unit` (lo arma `HomeScreen`) y
  `LoginScreen`/`RegisterScreen` reciben `onAutenticado`, `onIrARegistro`/`onVolver`
  en vez de `navController`. `AppNavigation` tiene el helper local `irSinVolver(ruta)`.
- **Lógica al ViewModel:** `AuthViewModel.marcarLeccionCompletada(unidadId, leccionId)`
  y `AuthViewModel.completarOnboarding()` (antes estaban en `AppNavigation`).
  `CursoViewModel.resumen(leccionesCompletadasIds)` reemplaza el
  `as? Recurso.Exito` + `remember` repetido en Inicio, Progreso y Perfil
  (`calcularResumenProgreso` ahora es privada). La barra superior de Inicio ya no usa
  `Usuario.totalLecciones` como respaldo mientras carga (muestra 0).
- Verificado con `./gradlew assembleDebug` (compila sin errores).

## Simplificación de los métodos Firebase (4 de noviembre de 2026)

Sin cambios visibles para el usuario ni en la estructura de Firestore:

- Se reemplazó el helper propio `esperar(Task)` por el oficial `.await()`
  (`kotlinx.coroutines.tasks.await`, ya incluido transitivamente por Firebase; no se
  agregó ninguna dependencia). Se borró `ui/viewmodel/FirebaseExt.kt`.
- `AuthViewModel` (~330 → ~230 líneas): un solo molde `try { ...await() } catch` por
  función, sin `Result<T>` intermedio.
  - `cargarPerfil()` reemplaza a `obtenerOCrearUsuario()` y a los 3 bloques copiados en
    `init`, `iniciarSesion` y el `recargarPerfil()` (este último no se usaba y se quitó).
    Solo escribe en Firestore la primera vez (antes hacía `set(merge)` en cada login).
  - Una sola `actualizarUsuario(onCompletado = {}, transformar)`; se usa con lambda final:
    `actualizarUsuario { it.copy(...) }`. Su mensaje de error ya no muestra texto técnico.
  - Una sola `registrar(nombre, correo, contrasena)` (`RegisterScreen` se adaptó). Ya no
    llama a `updateProfile` de Auth: el nombre vive en Firestore.
  - `actualizarNombre()` reutiliza `actualizarUsuario` (antes duplicaba la lógica y
    actualizaba también el `displayName` de Auth).
  - Se quitó el `AuthStateListener`: el cierre de sesión ya lo maneja `cerrarSesion()`.
- `CursoViewModel.cargarCurso()`: las unidades se ordenan con `orderBy("orden")` en
  Firestore (igual que las lecciones) en vez de `sortedBy` en Kotlin.
- `LeccionViewModel`: `registrarRespuestaSiCorresponde()` llama `.set(datos)` sin
  corrutina ni `await` (Firestore encola la escritura sola).
- Verificado con `./gradlew assembleDebug` (compila sin errores).

## Simplificación y legibilidad de código (18 de octubre de 2026)

Revisión orientada a claridad pedagógica para principiantes sin alterar apariencia ni funcionalidad:

- **Unificación de progreso en `CursoViewModel`:** Se introdujo `ResumenProgresoCurso` y
  el método `cursoViewModel.calcularResumenProgreso(...)`. Esto eliminó ~30 líneas de
  cálculos repetidos con transformaciones de listas anidadas en `ProgresoScreen`,
  `PerfilScreen` e `InicioScreen`.
- **`InicioScreen.BarraSuperiorAprendizaje` sincronizada:** Ahora refleja el progreso
  real de lecciones completadas frente al total dinámico del curso (resolviendo el
  pendiente que tenía del valor fijo).
- **Componente base `TarjetaPerfil` en `PerfilScreen`:** Centraliza el contenedor
  redondeado con borde para evitar repetir 5 veces los mismos modificadores visuales en
  tarjetas de estadísticas, preferencias y botones.
- **Comentarios didácticos:** Se documentaron conceptos clave como la lógica del camino
  en zig-zag de `BurbujaLeccion`, la organización de `ProgresoScreen` y las reglas de
  desbloqueo secuencial.

## Auditoría previa de código (17 de octubre de 2026)

Revisión de legibilidad/mantenibilidad sin cambiar arquitectura ni funcionalidad.
Cambios aplicados:

- **`Actividad.tipo` pasó de `String` a `enum class TipoActividad`** (mismo archivo,
  `data/entidad/Actividad.kt`).
- **Parseo de color unificado**: `colorDesdeHex(hex, fallback)` en `ui/theme/Color.kt`.
- **`PerfilScreen` muestra el progreso real del curso**, igual que `ProgresoScreen`.
- **`LeccionScreen.kt` (674 líneas) se dividió** en `ui/screens/leccion/`.

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
- RF06 - Desbloqueo secuencial dentro de las unidades (**aclaración de alcance, 17 de
  octubre de 2026**: por "nivel" el requerimiento se refiere a cada paso/lección
  dentro de una misma unidad — ej. completar la lección de saludo desbloquea la
  siguiente de esa unidad — **no** a las dificultades Básico/Intermedio/Avanzado que
  existían como colección Firestore `niveles`, un concepto distinto que nunca fue
  parte de este requerimiento):
  - **Cumplido.** El desbloqueo secuencial lección-a-lección (y unidad-a-unidad) ya
    estaba implementado por RF07 en `CursoViewModel.estaDesbloqueada` /
    `estaDesbloqueadaUnidad` / `estaCompletada`, pero quedaba inerte porque nada
    marcaba `Usuario.leccionesCompletadasIds`. RF14 cerró ese hueco: ahora, al
    terminar una lección real, `AppNavigation` agrega su clave `unidadId/leccionId` a
    esa lista, lo que activa el desbloqueo de la siguiente lección/unidad
    automáticamente (reactivo vía `StateFlow`, sin recargar la app).
  - **El concepto de "niveles" de dificultad (Básico/Intermedio/Avanzado) se eliminó
    por completo el 17 de octubre de 2026**, a pedido del usuario, porque en la
    práctica nunca aportaba nada: `CursoViewModel` solo cargaba el primer nivel y los
    demás eran inalcanzables. Se quitó: la entidad `Nivel.kt`, el campo
    `Unidad.nivelId`, `CursoService.obtenerNiveles()` (ahora `obtenerUnidades()` lee
    **todas** las unidades directamente, sin filtrar por nivel), y
    `EstadoCurso.Exito` ya no carga un `Nivel`, solo `unidades: List<Unidad>`. En el
    admin web (`QchuaGo-Admin`) se eliminó `NivelesPage.tsx`, `nivelesService.ts`, la
    ruta `/niveles`, el link de navegación, el tipo `Nivel` y el campo `nivelId`/selector
    de nivel en `UnidadesPage.tsx`. El script `scripts/seed_firestore.js` ya no crea
    niveles ni le agrega `nivelId` a las unidades.
  - Los documentos huérfanos `niveles/nivel_1`, `nivel_2`, `nivel_3` y el campo
    `nivelId` en `unidades/unidad_1`, `unidad_2`, `unidad_3` se borraron de la base de
    datos real el 17 de octubre de 2026 (script puntual con `firebase-admin`,
    ejecutado y luego eliminado — no quedó como herramienta permanente).
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
  - El porcentaje se calcula sobre **todas** las unidades existentes (ya no hay
    agrupación por nivel de dificultad desde que RF06 se simplificó — ver arriba).
  - No se construyó una pantalla de historial de respuestas individuales (eso viviría
    sobre `usuarios/{uid}/respuestas` de RF08); "resultados" se cubre con los
    porcentajes y conteos agregados.

## Flujo actual al entrar a Inicio

```text
INICIO -> Todas las unidades y lecciones existentes (sin agrupación por nivel de
dificultad), con desbloqueo secuencial lección a lección y unidad a unidad (RF06+RF07+RF14)
```

No existe pantalla de selección de nivel ni de selección de unidades — RF06 es el
desbloqueo secuencial dentro del mapa de unidades/lecciones, que ya funciona de punta
a punta. El concepto de niveles de dificultad (Básico/Intermedio/Avanzado) se eliminó
por completo (ver sección RF06 arriba).

## Pendientes relacionados

- El resultado del "Test de nivel" del onboarding (aciertos sobre `ActividadesPruebaNivel`) todavía no determina el nivel inicial real del usuario; queda para una iteración futura. (Esto es el campo de perfil `Usuario.nivel`, sin relación con la colección `niveles` que se eliminó.)
- Faltan pruebas unitarias e instrumentadas específicas para el curso.

## Verificación

- `gradlew.bat assembleDebug`: compilación correcta el 15 de octubre de 2026.
- `./gradlew assembleDebug`: compilación correcta el 16 de octubre de 2026 tras el rediseño del onboarding (bienvenida + elección de nivel).
- `./gradlew assembleDebug`: compilación correcta el 16 de octubre de 2026 tras eliminar la pantalla "Selecciona tu nivel" de RF06 (auto-selección del primer nivel).
- `./gradlew assembleDebug`: compilación correcta el 17 de octubre de 2026 tras agregar el tipo `ASOCIAR` y el registro de respuestas (RF08).
- `./gradlew assembleDebug`: compilación correcta el 17 de octubre de 2026 tras implementar el marcado de lecciones completadas y la pantalla de Progreso real (RF14).
- `./gradlew assembleDebug`: compilación correcta el 17 de octubre de 2026 tras la auditoría de código (enum `TipoActividad`, unificación de color, progreso real en Perfil, división de `LeccionScreen.kt`).
- `./gradlew assembleDebug` y `npm run build` (admin): compilación correcta el 17 de octubre de 2026 tras eliminar por completo el concepto de niveles de dificultad y corregir el desborde de `LeccionScreen` bajo la barra de estado (`systemBarsPadding()`).
