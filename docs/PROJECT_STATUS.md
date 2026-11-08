# Estado del proyecto QchuaGo

Última actualización: 8 de noviembre de 2026.

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

## Navegación con NavController directo (8 de noviembre de 2026, tarde)

Decisión explícita del usuario: **se eliminó la regla que prohibía pasar
`NavController` a las pantallas**. Antes, cada pantalla recibía lambdas de
navegación (`onIrARegistro`, `onAbrirLeccion`, `onVolver`...) que `AppNavigation.kt`
mapeaba una por una en cada bloque `composable(...)` — esto generaba "prop
drilling" y obligaba a tocar 3 archivos (la pantalla origen, su firma, y
`AppNavigation`) para agregar un solo botón de navegación nuevo. Se prioriza
velocidad de desarrollo y simplicidad sobre `@Preview`/testing de UI con
parámetros "puros" (que ya no se mantienen).

- Todas las pantallas con navegación reciben ahora `navController: NavHostController`
  (o `NavController` en `HomeScreen`/`InicioScreen`/`ProgresoScreen`, que ya lo usaban)
  y navegan ellas mismas con `navController.navigate(Rutas.X)` /
  `navController.popBackStack()`. Las que necesitan sesión también reciben
  `authViewModel: AuthViewModel` directamente (en vez de que `AppNavigation` les
  pase el dato ya extraído, ej. `uid`).
- **`AppNavigation.kt`** quedó reducido a solo registrar rutas:
  `composable(Rutas.X) { PantallaX(navController, authViewModel, ...) }`, sin
  lambdas `onIrAX` construidas por ruta. Se eliminó el helper `irSinVolver` (solo
  se usaba una vez, en el salto inicial de sesión).
- **`LoginScreen.kt`/`RegisterScreen.kt`:** se fusionó el patrón de "función wrapper
  + función pura" (existía solo para facilitar `@Preview`/testing, que ya no
  interesa) en una sola función que recibe `navController` y `authViewModel`.
- **`LeccionScreen.kt`:** ya no recibe `onSalir` ni `uid`; calcula `uid` leyendo
  `authViewModel.usuario` y decide internamente a dónde salir (si viene de "Test
  de nivel" —`actividadesEstaticas != null`— completa el onboarding y va a Home;
  si es una lección real, actualiza el perfil si corresponde y hace
  `popBackStack()`).
- **`EvaluacionScreen.kt`:** ya no recibe `onResultadoGuardado`/`onVolver`/`uid`;
  llama `authViewModel.actualizarPerfil()` directamente y usa `navController`.
- **`BienvenidaScreen.kt`/`ElegirNivelScreen.kt`:** reciben `navController` +
  `authViewModel` en vez de `usuario`/`onEmpezar`/`onPrincipiante`/`onTestNivel`.
- **`InicioScreen.kt`/`ProgresoScreen.kt`:** reciben `navController` en vez de
  `onAbrirLeccion`/`onAbrirEvaluacion`; navegan directo con `Rutas.leccion(...)`/
  `Rutas.evaluacion(...)`. `HomeScreen.kt` ya no construye esas lambdas, solo
  reenvía su propio `navController`.
- `AGENTS.md` se actualizó: la regla "Pantallas sin NavController" se reemplazó
  por "NavController directo" en la sección Jetpack Compose, y se agregó una nota
  en la sección Navegación.
- Verificado con `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug`
  (compila sin errores). No se probó en emulador/dispositivo real.

## Auditoría de legibilidad: parámetros y Firebase (8 de noviembre de 2026)

Revisión pedida explícitamente por el usuario: demasiados parámetros/lambdas en
composables y conexión confusa con Firebase/ViewModels, siendo QchuaGo un trabajo
de curso que debe ser simple y entendible, no "a prueba de todo". Sin cambios de
funcionalidad ni de arquitectura (sigue siendo `ViewModel <-> Composable`).

- **`LeccionScreen.kt`:** la función privada `ContenidoLeccion` tenía 13 parámetros
  (11 lambdas `onX` sueltas) — el peor caso del proyecto. Se agruparon en una
  `data class` privada `AccionesLeccion` (una sola instancia construida en
  `LeccionScreen`); `ContenidoLeccion` pasó a 4 parámetros
  (`estado`, `onSalir`, `acciones`, `modifier`). Ningún `TarjetaXxx` ni
  `LeccionViewModel` cambiaron.
- **`AuthViewModel.kt`:** era el único ViewModel que mezclaba `StateFlow` con
  callbacks anidados (`actualizarNombre(nuevoNombre, onSuccess)` envolvía
  `onSuccess` dentro de `onCompletado` de `actualizarUsuario`). Se agregó
  `guardadoExitoso: StateFlow<Boolean>` (+ `limpiarGuardadoExitoso()`) y
  `actualizarNombre`/`cambiarContrasena` perdieron su parámetro `onSuccess`,
  quedando alineados al resto de ViewModels (100% `StateFlow`, ninguna función
  pública recibe una lambda). `PerfilScreen.kt` observa `guardadoExitoso` con
  `collectAsState()` y un `LaunchedEffect` que cierra el diálogo abierto; sus
  parámetros `onActualizarNombre`/`onCambiarContrasena` pasaron de
  `(String, () -> Unit) -> Unit` a `(String) -> Unit` (y similar).
- **`Recurso.kt`:** nueva función `cargarComoRecurso(mensajeError) { bloque }` que
  envuelve el `try { Recurso.Exito(...) } catch { Recurso.Error(mensaje) }`
  repetido en `CursoViewModel.cargarCurso()`, `DiccionarioViewModel.cargar()` y
  `RankingViewModel.cargarRanking()` (las 3 usan `Recurso<T>`). No se tocó
  `LeccionViewModel`/`EvaluacionViewModel` (usan sus propias máquinas de estado,
  no `Recurso<T>`).
- **`UsuarioRanking`** se movió de `RankingViewModel.kt` a
  `data/entidad/UsuarioRanking.kt`, igual que el resto de modelos de dominio
  (`Usuario`, `Actividad`, `Unidad`, `Leccion`, `EntradaDiccionario`).
  `ProgresoScreen.kt` ahora lo importa desde `data.entidad` en vez de
  `ui.viewmodel`.
- Verificado con `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug`
  (compila sin errores). No se probó en emulador/dispositivo real por no haber
  ninguno disponible en la sesión.

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
  - **Cumplido.** El desbloqueo secuencial lección-a-lección (y originalmente
    unidad-a-unidad) ya
    estaba implementado por RF07 en `CursoViewModel.estaDesbloqueada` /
    `estaDesbloqueadaUnidad` / `estaCompletada`, pero quedaba inerte porque nada
    marcaba `Usuario.leccionesCompletadasIds`. RF14 cerró ese hueco: ahora, al
    terminar una lección real, `AppNavigation` agrega su clave `unidadId/leccionId` a
    esa lista, lo que activa el desbloqueo de la siguiente lección/unidad
    automáticamente (reactivo vía `StateFlow`, sin recargar la app). Desde RF19,
    completar las lecciones habilita la evaluación y aprobarla desbloquea la siguiente
    unidad.
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
- RF10 - Pronunciación y escucha:
  - La reproducción del audio real de vocabulario/pronunciación ya existía (`TarjetaContenido`,
    `LeccionViewModel.reproducirAudio`). Lo nuevo es el **ejercicio de escucha**: nuevo
    `TipoActividad.ESCUCHA`, evaluable, que reutiliza los campos existentes de `Actividad`
    (`audioUrl`/`audioLocal`, `enunciado`, `opciones`, `respuestaCorrecta`) sin agregar campos
    nuevos a la entidad.
  - `TarjetaEscucha.kt` (`ui/screens/leccion/`) reproduce el audio automáticamente al entrar a la
    pregunta y ofrece un botón para repetirlo; **nunca muestra** `actividad.quechua`/`traduccion`,
    para que la respuesta dependa solo de lo que el estudiante escuchó. La calificación
    (`LeccionViewModel.comprobar()`) compara `respuestaSeleccionada` contra `respuestaCorrecta`,
    igual que `SELECCION`.
  - **Calificación de pronunciación hablada (7 de noviembre de 2026, noche): implementada por
    similitud acústica, no por reconocimiento de voz.** Se descartó el ASR (reconocimiento de
    voz) por no existir de forma confiable para quechua (ninguna API comercial lo soporta, y los
    modelos de investigación requieren infraestructura propia fuera del stack actual). En su
    lugar, `PRONUNCIACION` ahora es **evaluable**: al grabar la voz y presionar "Comprobar",
    `LeccionViewModel.evaluarPronunciacion()` decodifica la grabación y el audio de referencia de
    la actividad a PCM (`MediaExtractor`/`MediaCodec`, SDK de Android, sin dependencias nuevas) y
    compara ambos con **MFCC + DTW** (`SimilitudAudio.kt`, funciones puras sin dependencias de
    Android, con pruebas unitarias en `app/src/test/.../SimilitudAudioTest.kt`) — un puntaje de
    similitud 0-100, no una transcripción. Se aprueba con un umbral (`UMBRAL_SIMILITUD_PRONUNCIACION`,
    hoy `55f`, **sin calibrar con voz real**: requiere ajuste al probar en un dispositivo).
    `TIPOS_EVALUABLES` incluye `PRONUNCIACION`; `TIPOS_CONTENIDO` en `LeccionScreen.kt` ya solo
    es `VOCABULARIO` (que sigue siendo práctica libre, sin calificar). Si la decodificación falla
    (sin internet para un audio de referencia remoto, archivo corrupto), no se marca como
    incorrecta: se muestra un error y se puede reintentar. `PanelRetroalimentacion` tiene mensajes
    propios para este tipo ("¡Buena pronunciación!"/"Sigue practicando esa pronunciación.") en vez
    de "La respuesta era: " (que no aplica aquí), y muestra el % de similitud.
  - Espejo en `QchuaGo-Admin`: `types.ts`, `SelectorTipoActividad.tsx`, `VistaPreviaMovil.tsx` y
    `ActividadesPage.tsx` (nuevo `ESCUCHA` en el selector de tipo, con el mismo bloque de
    opciones/respuesta correcta que `SELECCION` y el campo de audio, antes exclusivo de
    `VOCABULARIO`/`PRONUNCIACION`, ahora extraído a `renderCampoAudio()` y reutilizado también
    para `ESCUCHA`).
- RF11 - Diccionario y desafío de traducción bidireccional:
  - Nueva pestaña **Diccionario** real (antes placeholder): `DiccionarioScreen.kt` carga un
    diccionario quechua-español **local, embebido en la app** (`app/src/main/assets/diccionario_quechua.csv`,
    1254 entradas: 972 `palabra` + 282 `expresion`), sin Firestore ni administración desde
    `QchuaGo-Admin` — es contenido de referencia fijo, no contenido de lección. `DiccionarioViewModel`
    lo parsea una sola vez (`Dispatchers.IO`) con un separador de CSV propio (soporta campos entre
    comillas con comas internas) y expone `Recurso<List<EntradaDiccionario>>` (se reutiliza el
    `Recurso<T>` genérico ya existente, sin inventar un sealed class de estado nuevo). La pantalla
    tiene un buscador que filtra por texto en quechua o español.
  - Botón flotante **"🎯 Desafío"** sobre la lista: abre una **ventana flotante** (`Dialog` de
    Compose, no una pantalla ni ruta nueva) con `DesafioDiccionario.kt`/`DesafioViewModel.kt` — un
    ejercicio de traducción de práctica libre: toma una palabra/expresión al azar del diccionario,
    una dirección al azar (quechua→español o español→quechua) y arma 4 opciones (1 correcta + 3
    distractores de otras entradas). Reutiliza `OpcionTarjeta` de `ui/screens/leccion/` (es
    `internal`, visible en todo el módulo `app`). Lleva un contador de aciertos/intentos solo en
    memoria de esa sesión del diálogo — **no otorga XP, no afecta la racha y no se guarda en
    Firestore**, es práctica suelta sin relación con lecciones específicas.
  - **Columnas `alias`/`nota` (7 de noviembre de 2026, misma tarde):** el CSV se amplió a 5
    columnas (`tipo,quechua,espanol,alias,nota`, 1259 filas) porque varias palabras en español
    (ej. "hola", "adiós", "gracias" en su sentido coloquial) no tienen equivalente léxico literal
    en quechua, solo una expresión que cumple esa función social (ej. `Rimaykullayki` no significa
    literalmente "hola"). `alias` (términos separados por `;`) agrega esos sinónimos coloquiales a
    la búsqueda sin tocar el campo `espanol`; `nota` (52 filas) explica la aclaración lingüística.
    `EntradaDiccionario` ganó `alias: List<String>` y `nota: String`; la búsqueda de
    `DiccionarioScreen` ahora también compara contra `alias`; la fila de la lista y la
    retroalimentación del Desafío muestran la `nota` ("💡 ...") cuando existe. El `alias` no
    participa en la calificación del Desafío (sigue siendo opción múltiple contra `quechua`/
    `espanol` exactos).
  - **Aviso genérico de "sin traducción literal" (7 de noviembre de 2026, misma tarde):** cuando
    la búsqueda solo encuentra resultados por `alias` (ninguna entrada tiene el texto buscado
    literal en `quechua`/`espanol` — ej. buscar "hola"), `DiccionarioScreen` muestra un aviso fijo
    y genérico debajo del buscador ("💡 "...no existe como palabra o frase literal en quechua...")
    antes de la lista. Si el texto buscado sí aparece literal en algún resultado (ej. "no" →
    `mana`), no se muestra nada nuevo. Es independiente de la `nota` por fila, que sigue mostrando
    el detalle específico de cada expresión.
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
- RF15 - Puntos y experiencia (XP):
  - Cada actividad evaluable respondida correctamente entrega 5 XP una sola vez. El
    documento de respuesta guarda `xpOtorgado = true`, por lo que repetir la lección
    no vuelve a premiar la misma actividad. Las respuestas incorrectas entregan 0 XP.
  - Completar una lección real por primera vez entrega 20 XP adicionales. La misma
    transacción comprueba `leccionesCompletadasIds`, actualiza el progreso y evita
    repetir la bonificación.
  - El XP y el progreso se guardan juntos mediante una transacción de Firestore. Si
    falla, el resultado muestra un mensaje y permite reintentar; no se muestran ni se
    conservan puntos que Firebase no haya confirmado.
  - `ResultadoLeccion` muestra los XP obtenidos en ese intento y `PerfilScreen` sigue
    mostrando el total persistido en `Usuario.xp`. Al volver, `AuthViewModel` recarga
    el perfil para reflejar el nuevo total.
  - La prueba de nivel usa actividades estáticas y no entrega ni guarda XP.
- RF16 - Racha de aprendizaje:
  - `Usuario.ultimaFechaPractica` guarda la fecha local en formato `yyyy-MM-dd` y la
    zona horaria usada para calcular cada día es `America/Lima`.
  - Al completar una lección real, la misma transacción de RF15 compara la fecha:
    el mismo día conserva la racha, el día siguiente suma uno y cualquier intervalo
    mayor reinicia la racha en 1. Repetir una lección también registra la práctica.
  - La transacción evita incrementos duplicados por pulsaciones o reintentos y guarda
    `racha` junto con `ultimaFechaPractica` sin sobrescribir otros campos del usuario.
  - `PerfilScreen` y la barra de Inicio ya mostraban `Usuario.racha`; reutilizan sus
    iconos y componentes existentes y reciben el valor actualizado al recargar el
    perfil después de terminar la lección.
  - Abrir o abandonar una lección y completar la prueba de nivel no modifica la racha.
- RF19 - Evaluaciones de unidad:
  - Al completar todas las lecciones de una unidad aparece el botón "Evaluación
    final" en Inicio y también se ofrece desde la tarjeta de continuar en Progreso.
  - `EvaluacionViewModel` obtiene, en el orden del curso, hasta 10 actividades
    `SELECCION` válidas de las lecciones de esa unidad. Si existen menos, utiliza las
    disponibles; no crea preguntas ni respuestas nuevas.
  - `EvaluacionScreen` muestra una pregunta por vez y calcula el porcentaje entero con
    `(correctas * 100) / total`. Se aprueba con 70 % o más y, si se desaprueba, se
    puede repetir.
  - El resultado se guarda mediante una transacción en
    `usuarios/{uid}/evaluaciones/{unidadId}` con `unidadId`, `calificacion`, `aprobada`
    y `fecha`. Se conserva la mejor calificación y una aprobación nunca vuelve a
    perderse. `Usuario.evaluacionesAprobadasIds` permite reflejar el desbloqueo de
    forma reactiva y conservarlo al reiniciar.
  - La primera unidad permanece disponible. Dentro de una unidad, las lecciones siguen
    desbloqueándose secuencialmente; la siguiente unidad requiere que la evaluación
    anterior esté aprobada. El ViewModel también comprueba en Firestore que todas las
    lecciones estén completas antes de iniciar una evaluación.
  - Las evaluaciones no llaman a la lógica de finalización de lecciones, por lo que no
    entregan XP, no actualizan la racha y no modifican vidas ni respuestas.
- RF20 - Clasificación o ranking semanal:
  - Muestra la tabla de posiciones semanal basada en los XP ganados durante la semana actual
    (`rankingsSemanales/{semanaId}/usuarios/{uid}`). La semana se calcula según el estándar
    ISO (lunes a domingo) en la zona horaria `America/Lima` (ej. `2026-W45`).
  - Los XP semanales se incrementan atómicamente dentro de la misma transacción de Firestore
    de RF15 en `LeccionViewModel.guardarResultado`, garantizando que no haya duplicados ni
    falsas recompensas. El XP total del usuario no se reinicia.
  - La interfaz está integrada al final de `ProgresoScreen.kt` (debajo de la lista de unidades)
    y consta de un podio visual de 3 barras verticales (2° en plata a la izquierda, 1° en oro
    en el centro más alto, 3° en bronce a la derecha) con medallas 🥇, 🥈, 🥉, más la lista
    de clasificación para puestos inferiores (#4 en adelante).
  - El usuario autenticado aparece resaltado con fondo y borde verde. Si no pertenece al
    top 20, se realiza una consulta adicional para obtener y mostrar su posición real.
  - Maneja adecuadamente los estados de carga, lista vacía, éxito y error con opción de reintento.

## Flujo actual al entrar a Inicio

```text
INICIO -> Unidad disponible -> Lecciones secuenciales -> Evaluación final (70 %)
       -> Siguiente unidad (RF06+RF07+RF14+RF19)
```

No existe pantalla de selección de nivel ni de selección de unidades — RF06 es el
desbloqueo secuencial dentro del mapa de unidades/lecciones, que ya funciona de punta
a punta. El concepto de niveles de dificultad (Básico/Intermedio/Avanzado) se eliminó
por completo (ver sección RF06 arriba).

## Contenido de prueba: 5 unidades regeneradas (7 de noviembre de 2026, noche)

`scripts/seed_firestore.js` se reescribió por completo: ya no es "crear si no existe"
(`crearSiNoExiste`), ahora **borra unidad_1 a unidad_5 recursivamente** (`db.recursiveDelete`)
y las vuelve a crear desde cero cada vez que se corre — pensado para regenerar contenido de
prueba, no para proteger datos reales de producción. Resultado de la última corrida: 5
unidades, 10 lecciones reales + 5 checkpoints, 70 actividades (los 7 tipos —
VOCABULARIO/PRONUNCIACION/SELECCION/COMPLETAR/ORDENAR/ASOCIAR/ESCUCHA— en cada lección),
todas con contenido real tomado de `diccionario_quechua.csv`:

- Unidad 1: Saludos · Unidad 2: Familia · Unidad 3: Números (regeneradas) · Unidad 4: Colores
  y cuerpo · Unidad 5: Animales y comida (nuevas).

**Audio de las 28 palabras/frases usadas:** generado con **edge-tts** (voces neuronales de
Microsoft, gratis, sin API key) con la voz en español peruano `es-PE-CamilaNeural` leyendo el
texto en quechua tal cual está escrito — no es pronunciación quechua nativa, es una
aproximación con voz en español (se investigó: ni gTTS ni edge-tts tienen quechua entre sus
idiomas/voces). Subido a Cloudinary con el mismo preset **unsigned** `qchuago_audios` que ya
usa `QchuaGo-Admin` (`src/cloudinary/audioService.ts`, carpeta `audios/seed_rf05/`), así que
todas las actividades usan `audioUrl` (ninguna usa `audioLocal`, a pedido explícito del
usuario: todo el contenido vive en Firestore, no embebido en la app). Las URLs quedaron
hardcodeadas en el propio `seed_firestore.js` (`const AUDIOS = {...}`). Cada actividad sigue
con `pendienteValidacionLinguistica: true`: el contenido y el audio no están validados por un
hablante nativo, es contenido de prueba para ejercitar los 7 tipos de la app end-to-end.

Requiere `scripts/serviceAccountKey.json` (Firebase Console → Configuración → Cuentas de
servicio → Generar nueva clave privada), nunca comiteado (ya está en `.gitignore`).

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
- `gradlew.bat assembleDebug`: compilación correcta el 7 de noviembre de 2026 tras implementar RF15.
- `gradlew.bat assembleDebug`: compilación correcta el 7 de noviembre de 2026 tras implementar RF16.
- `gradlew.bat assembleDebug`: compilación correcta el 7 de noviembre de 2026 tras implementar RF19.
- `gradlew.bat assembleDebug`: compilación correcta el 7 de noviembre de 2026 tras implementar RF20.
- `./gradlew assembleDebug` y `npm run build` (admin): compilación correcta el 7 de noviembre de
  2026 tras implementar el ejercicio de escucha de RF10 (`TipoActividad.ESCUCHA`).
- `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug`: compilación correcta el 7 de
  noviembre de 2026 tras implementar el diccionario local y el desafío de traducción de RF11. No
  se probó en un emulador/dispositivo real por no haber ninguno disponible en la sesión.
- `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug`: compilación correcta el 7 de
  noviembre de 2026 tras agregar soporte para `alias`/`nota` en el diccionario (palabras sin
  traducción literal). Tampoco se probó en emulador/dispositivo real.
- `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug`: compilación correcta el 7 de
  noviembre de 2026 tras agregar el aviso genérico de "sin traducción literal" en la búsqueda del
  diccionario. Tampoco se probó en emulador/dispositivo real.
- `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug` y `./gradlew test`: compilación y
  pruebas unitarias correctas el 7 de noviembre de 2026 tras implementar la calificación de
  pronunciación por similitud acústica (MFCC+DTW). Las pruebas unitarias (`SimilitudAudioTest`,
  con tonos sintéticos) validan la matemática; **no se probó con voz real en un dispositivo** ni
  se calibró el umbral de aprobación, por no haber ninguno disponible en la sesión.
- `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug`: compilación correcta el 8 de
  noviembre de 2026 tras la auditoría de legibilidad de parámetros/Firebase (`AccionesLeccion`,
  `AuthViewModel` sin callbacks anidados, `cargarComoRecurso`, `UsuarioRanking` reubicado).
  Tampoco se probó en emulador/dispositivo real.
- `JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug`: compilación correcta el 8 de
  noviembre de 2026 (tarde) tras pasar `NavController` directo a todas las pantallas con
  navegación, eliminando las lambdas `onIrAX`/`onVolver`/`onAbrirX`. Tampoco se probó en
  emulador/dispositivo real.
