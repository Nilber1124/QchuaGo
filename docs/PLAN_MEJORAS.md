# Plan de mejoras: código fácil de modificar

Fecha: 4 de noviembre de 2026. Objetivo: que cualquier integrante del equipo pueda
agregar o cambiar algo **rápido y sin miedo** durante el examen, sin cambiar la
arquitectura actual (MVVM de 2 capas, sin Service, sin Hilt).

## Diagnóstico rápido

Lo que ya está bien (no tocar):

- Arquitectura plana y clara: `Composable <-> ViewModel <-> Firebase`.
- `Recurso<T>` (Cargando / Exito / Error) como patrón único de estado.
- Pantallas divididas en "envoltorio con ViewModel" + "pantalla sin estado"
  (`PerfilScreen`, `LoginScreen`, `RegisterScreen`). Es el patrón correcto.
- Tarjetas de ejercicio separadas en `ui/screens/leccion/` (una por tipo).
- `Usuario` con valores por defecto: agregar un campo nuevo es 1 línea.

Lo que hoy hace difícil modificar:

| # | Problema | Dónde | Por qué molesta en un examen |
|---|----------|-------|------------------------------|
| 1 | Pantallas de "Cargando..." y "Error + Reintentar" copiadas 3 veces | `InicioScreen.kt:101-130`, `ProgresoScreen.kt:67-83`, `LeccionScreen.kt` (`CargandoLeccion`, `ErrorLeccion`) | Una pantalla nueva obliga a copiar/pegar 30 líneas otra vez |
| 2 | La ruta de lección se arma "a mano" con strings | `InicioScreen.kt:180`, `ProgresoScreen.kt:124`, `AppNavigation.kt` | Si cambia la ruta hay que buscarla en varios archivos; un typo no da error de compilación |
| 3 | Lógica de negocio dentro de la navegación (marcar lección completada) | `AppNavigation.kt` ruta `LECCION` | No es obvio dónde buscar; rompe la regla "lógica en el ViewModel" |
| 4 | El cálculo del resumen de progreso se repite con `as? Recurso.Exito` + `remember` | `InicioScreen`, `ProgresoScreen`, `PerfilScreen` | 3 lugares que cambiar si cambia la regla de progreso |
| 5 | Agregar un tipo de actividad toca ~6 sitios, 2 de ellos son listas sueltas (`TIPOS_EVALUABLES`, `TIPOS_CONTENIDO`) | `LeccionViewModel.kt:20`, `LeccionScreen.kt:48` | Fácil olvidarse de uno y que el ejercicio "no cuente" |
| 6 | `continuar()` y `anterior()` repiten el mismo bloque de reinicio de campos | `LeccionViewModel.kt` | Al agregar un campo nuevo al estado hay que acordarse de limpiarlo en 2 lugares |
| 7 | Algunas pantallas reciben `navController` y otras lambdas (`onEmpezar`, `onSalir`) | `InicioScreen`, `ProgresoScreen`, `LoginScreen`, `RegisterScreen`, `HomeScreen` | Inconsistente; con lambdas la pantalla es más simple y se puede previsualizar |
| 8 | Funciones duplicadas en `AuthViewModel` (2× `actualizarUsuario`, 2× `registrar`) y un solo `error` compartido por Login/Registro/Perfil | `AuthViewModel.kt` | Confunde cuál usar; un error de Perfil puede aparecer en Login |
| 9 | Colores fijos dentro de pantallas y colores de plantilla sin uso (`Purple80`, `Pink40`…) | `InicioScreen.kt:272,400,401,449`, `Color.kt:5-11` | Cambiar el color de "bloqueado" obliga a buscar hex sueltos |
| 10 | No hay ningún `@Preview` | todas las pantallas | Para ver un cambio visual hay que compilar y abrir el emulador cada vez |
| 11 | Datos viejos: `Usuario.totalLecciones = 9`, `nivelANumero()` con niveles eliminados | `Usuario.kt`, `InicioScreen.kt:473` | Confunde: parece que siguen existiendo niveles |

## Plan por fases

Cada fase compila sola y no cambia lo que ve el usuario. Hacerlas en orden; se puede
parar después de cualquiera.

### Fase 1 — Componentes comunes (prioridad ALTA, ~30 min)

> ✅ **Aplicada el 4 de noviembre de 2026.**

1. Crear `ui/screens/ComponentesComunes.kt` con:
   - `PantallaCargando(texto: String = "Cargando...")`
   - `PantallaError(mensaje: String, onReintentar: () -> Unit, onVolver: (() -> Unit)? = null)`
   - (opcional) `ContenidoRecurso(recurso, onReintentar) { datos -> ... }`: un `when`
     listo que muestra cargando/error y solo te pide qué dibujar en `Exito`.
2. Reemplazar los bloques duplicados en `InicioScreen`, `ProgresoScreen` y
   `LeccionScreen` por esos componentes.

Resultado: una pantalla nueva con datos de Firestore se escribe así:

```kotlin
ContenidoRecurso(estado, onReintentar = vm::cargar) { palabras ->
    LazyColumn { items(palabras) { Text(it.quechua) } }
}
```

### Fase 2 — Navegación sin strings sueltos (prioridad ALTA, ~20 min)

> ✅ **Aplicada el 4 de noviembre de 2026.**

1. En `Rutas` agregar `fun leccion(unidadId: String, leccionId: String) = "$LECCION/$unidadId/$leccionId"`
   y una constante `LECCION_PATRON = "$LECCION/{unidadId}/{leccionId}"`.
2. Cambiar `InicioScreen` y `ProgresoScreen` para recibir `onAbrirLeccion: (Unidad, Leccion) -> Unit`
   en vez de `navController`. `HomeScreen` es quien llama a `navController.navigate(Rutas.leccion(...))`.
3. (Opcional) Lo mismo con `LoginScreen`/`RegisterScreen`: `onIrARegistro`, `onAutenticado`.

### Fase 3 — Lógica de vuelta al ViewModel (prioridad ALTA, ~20 min)

> ✅ **Aplicada el 4 de noviembre de 2026.**

1. `AuthViewModel.marcarLeccionCompletada(clave: String)`: mueve aquí el bloque que
   hoy está en `AppNavigation` (evitar duplicados + sincronizar `leccionesCompletadas`).
2. `CursoViewModel.resumen(leccionesCompletadasIds)`: hace internamente el
   `as? Recurso.Exito` y devuelve `ResumenProgresoCurso()` vacío si aún no cargó.
   Las 3 pantallas quedan con una sola línea.
3. Unificar las sobrecargas de `AuthViewModel`:
   `actualizarUsuario(transformar, onCompletado: ((Boolean) -> Unit)? = null)` (una sola función)
   y dejar solo `registrar(nombre, correo, contrasena)`.

### Fase 4 — Agregar ejercicios nuevos sin olvidar nada (prioridad MEDIA, ~30 min)

1. Mover "es evaluable" al enum:

   ```kotlin
   enum class TipoActividad(val evaluable: Boolean) {
       VOCABULARIO(false), PRONUNCIACION(false),
       SELECCION(true), COMPLETAR(true), ORDENAR(true), ASOCIAR(true),
   }
   ```

   y borrar `TIPOS_EVALUABLES` / `TIPOS_CONTENIDO` (usar `actividad.tipo.evaluable`).
   Firestore sigue guardando el nombre (`"SELECCION"`), así que no rompe datos.
2. En `LeccionViewModel` crear `private fun irA(indice: Int)` que hace el reinicio de
   campos; `continuar()` y `anterior()` lo llaman. Un campo nuevo se limpia en un solo sitio.
3. Comentario `// PASO X para nuevo tipo` en cada `when (actividad.tipo)` para que se
   encuentren con Ctrl+F (ver receta abajo). Como los `when` no tienen `else`, Kotlin
   **marca error** si falta un tipo: eso es bueno, no agregar `else`.

### Fase 5 — Previews y limpieza (prioridad MEDIA, ~30 min)

1. Agregar `@Preview` a las versiones sin estado: `PerfilScreen(usuario = Usuario(...), ...)`,
   `BienvenidaScreen`, `ElegirNivelScreen`, cada `Tarjeta*` de `leccion/`.
   Se ven en Android Studio sin compilar al emulador.
2. Mover colores fijos de `InicioScreen` a `Color.kt` (`ColorBloqueado`,
   `ColorCheckpoint`, `ColorSiguiente`) y borrar `Purple*/Pink*` sin uso.
3. Quitar `nivelANumero` (mostrar `usuario.nivel` directo) y `Usuario.totalLecciones`
   (ya se calcula del curso real). Verificar con búsqueda que nada más los usa.

### Fase 6 — Opcional (solo si sobra tiempo)

- Errores separados por pantalla en `AuthViewModel` (`errorLogin`, `errorPerfil`) o
  llamar `limpiarError()` al entrar a cada pantalla.
- Pasar la lógica pura de desbloqueo de `CursoViewModel` a funciones sin Firebase para
  poder escribir pruebas unitarias (`app/src/test` hoy está vacío).
- **No recomendado:** separar el audio de `LeccionViewModel` en otra clase. Se entiende
  bien como está y agregaría archivos sin ganancia real para el equipo.

## Simplificar los métodos que hablan con Firebase

> **Aplicado el 4 de noviembre de 2026** (F1–F9; F10 queda opcional). Ver `PROJECT_STATUS.md`.

Revisión de `AuthViewModel`, `CursoViewModel` y `LeccionViewModel`. Se puede quitar
bastante código **sin agregar dependencias**: `kotlinx-coroutines-play-services`
(la que trae `.await()`) ya viene incluida con Firebase (comprobado con
`./gradlew :app:dependencies`, versión 1.9.0).

### Hallazgos

| # | Qué pasa hoy | Dónde | Propuesta |
|---|--------------|-------|-----------|
| F1 | Helper propio `esperar(tarea)` para esperar un `Task` de Firebase | `ui/viewmodel/FirebaseExt.kt`, ~20 llamadas | Usar el oficial `tarea.await()` (`import kotlinx.coroutines.tasks.await`) y **borrar `FirebaseExt.kt`**. Es lo que sale en la documentación de Firebase y en tutoriales |
| F2 | Las funciones privadas devuelven `Result<T>` y luego se desenvuelven con `.onSuccess { } .onFailure { }` | `obtenerOCrearUsuario`, `guardarUsuario` | Que lancen la excepción normal y usar **un solo `try/catch`** en la función pública. Una forma de manejar errores en vez de dos |
| F3 | Cada login/arranque hace `get()` **y además** un `set(merge)` aunque el usuario ya exista | `obtenerOCrearUsuario` | Solo escribir si el documento no existe. `toObject` ya rellena los campos nuevos con su valor por defecto, y el próximo `actualizarUsuario` los guarda |
| F4 | El mismo bloque "cargar perfil → poner tema → poner usuario → si falla, error" está copiado en `init`, `iniciarSesion` y `recargarPerfil` | `AuthViewModel` | Una sola función privada `cargarPerfil(usuarioAuth)` |
| F5 | El registro hace 3 llamadas anidadas (crear cuenta, `updateProfile` en Auth, guardar en Firestore) con un `try` dentro de otro | `registrar` | El nombre ya vive en Firestore: quitar `updateProfile` y el `try` interno. Quedan 2 llamadas seguidas |
| F6 | `actualizarNombre` vuelve a hacer `updateProfile` en Auth + guarda en Firestore con su propio flujo | `actualizarNombre` | Validar y luego llamar a `actualizarUsuario { it.copy(nombre = ...) }`, que ya existe |
| F7 | `AuthStateListener` + `cancelarEscuchaAuth` + `onCleared` solo para detectar un cierre de sesión que la app ya maneja en `cerrarSesion()` | `AuthViewModel` (init) | Quitarlo: ~12 líneas menos y un concepto menos que explicar |
| F8 | Respuestas de ejercicios: `viewModelScope.launch` + `runCatching` + `esperar` para algo que "no importa si falla" | `LeccionViewModel.registrarRespuestaSiCorresponde` | Llamar `.set(datos)` **sin esperar**: Firestore lo encola solo (incluso sin internet). Sin corrutina ni `try` |
| F9 | Se ordenan las unidades en Kotlin (`sortedBy`) pero las lecciones en Firestore (`orderBy`) | `CursoViewModel.cargarCurso` | Usar `.orderBy("orden")` también en unidades, igual que en lecciones. Mismo estilo en todo el archivo |
| F10 | Nombres de colecciones (`"usuarios"`, `"unidades"`, `"lecciones"`, `"actividades"`, `"respuestas"`) escritos a mano en los 3 ViewModels | 3 archivos | Opcional: `object Colecciones { const val USUARIOS = "usuarios" ... }` en `Recurso.kt`. Evita errores de tipeo |

### Ejemplo: así quedaría la parte de perfil en `AuthViewModel`

Antes (≈45 líneas entre `obtenerOCrearUsuario`, `guardarUsuario` y los 3 bloques copiados):

```kotlin
obtenerOCrearUsuario(usuarioAuth)
    .onSuccess { u -> _temaOscuro.value = u.temaOscuro; _usuario.value = u }
    .onFailure { _error.value = "..."; _usuario.value = usuarioAuth }
```

Después (≈15 líneas en total):

```kotlin
private suspend fun cargarPerfil(usuarioAuth: Usuario) {
    try {
        val documento = usuariosCollection.document(usuarioAuth.uid)
        val snapshot = documento.get().await()
        val usuario = snapshot.toObject(Usuario::class.java)
            ?: usuarioAuth.also { documento.set(it).await() } // primera vez: lo crea
        _usuario.value = usuario
        _temaOscuro.value = usuario.temaOscuro
    } catch (e: Exception) {
        _usuario.value = usuarioAuth
        _error.value = "No se pudo cargar tu perfil. Inténtalo nuevamente."
    }
}
```

Y el registro:

```kotlin
fun registrar(nombre: String, correo: String, contrasena: String) {
    _cargando.value = true
    _error.value = null
    viewModelScope.launch {
        try {
            val user = auth.createUserWithEmailAndPassword(correo.trim(), contrasena).await().user!!
            val nuevo = Usuario(uid = user.uid, nombre = nombre.trim(), correo = correo.trim())
            usuariosCollection.document(user.uid).set(nuevo).await()
            _usuario.value = nuevo
        } catch (e: Exception) {
            _error.value = mensajeComprensible(e)
        }
        _cargando.value = false
    }
}
```

### Resultado esperado

- `AuthViewModel`: de ~330 a ~200 líneas, todas las funciones con el mismo molde:
  `cargando = true → try { ...await() } catch { error en español } → cargando = false`.
- `FirebaseExt.kt` desaparece (un archivo menos).
- Ningún cambio visible para el usuario ni en la estructura de Firestore.

**Precaución:** probar después login, registro, cambiar nombre, cambiar tema y
completar una lección, porque `AuthViewModel` lo usan casi todas las pantallas.

## Recetas para el examen

Guía paso a paso de los cambios más probables. Copiar esto a una hoja aparte.

### A. Agregar un campo al usuario (ej. "monedas")

1. `data/entidad/Usuario.kt`: `val monedas: Int = 0,`
2. Mostrarlo: en la pantalla, `usuario?.monedas`.
3. Cambiarlo: `authViewModel.actualizarUsuario { it.copy(monedas = it.monedas + 10) }`.
   Firestore se actualiza solo (merge). No hay que tocar nada más.

### B. Agregar una pantalla nueva con navegación

1. `ui/screens/MiNuevaScreen.kt` con `fun MiNuevaScreen(onVolver: () -> Unit)`.
2. `AppNavigation.kt`: `const val MI_NUEVA = "mi_nueva"` en `Rutas` y
   `composable(Rutas.MI_NUEVA) { MiNuevaScreen(onVolver = { navController.popBackStack() }) }`.
3. Desde otra pantalla: lambda `onIrAMiNueva` que llama `navController.navigate(Rutas.MI_NUEVA)`.

### C. Agregar / reemplazar una pestaña del Home

1. `HomeScreen.kt` → `enum class NavTab`: agregar entrada con título e ícono en `res/drawable`.
2. En el `when (selectedTab)` agregar la rama (Kotlin marca error si falta).

### D. Pantalla que lee una colección de Firestore (ej. Diccionario)

1. Entidad: `data/entidad/Palabra.kt` con `@Keep data class` y valores por defecto.
2. ViewModel: `ui/viewmodel/DiccionarioViewModel.kt` copiando la forma de `CursoViewModel`
   (`MutableStateFlow<Recurso<List<Palabra>>>`, `cargar()` con `esperar(...)` y `try/catch`
   con mensaje en español).
3. Pantalla: `val vm: DiccionarioViewModel = viewModel()` + `ContenidoRecurso(...)` (Fase 1).
4. Si va con la cuenta del usuario, guardar bajo `usuarios/{uid}/...` como hace
   `registrarRespuestaSiCorresponde`.

### E. Agregar un tipo de ejercicio nuevo (ej. VERDADERO_FALSO)

1. `Actividad.kt`: agregar al enum (`VERDADERO_FALSO(true)` tras la Fase 4). Si necesita
   datos nuevos, reutilizar `opciones` / `respuestaCorrecta` antes de crear campos.
2. Compilar: Kotlin marcará cada `when` que falta (`comprobar()` y `LeccionScreen`).
3. `LeccionViewModel.comprobar()`: regla de si es correcta.
4. Si necesita estado propio: campo en `EstadoLeccion.EnCurso` + función pública que lo
   cambie + reinicio en `irA()`.
5. `ui/screens/leccion/TarjetaVerdaderoFalso.kt` copiando `TarjetaSeleccion.kt`.
6. `LeccionScreen.kt`: rama en el `when` que dibuja la tarjeta.
7. Admin web (`QchuaGo-Admin/src/types.ts`) para poder crearlo desde el panel.

## Orden sugerido y reparto

| Fase | Riesgo | Quién puede hacerla |
|------|--------|---------------------|
| 1 Componentes comunes | Bajo | Cualquiera (buena práctica de Compose) |
| 2 Navegación | Bajo | Quien maneje `AppNavigation` |
| 3 Lógica al ViewModel | Medio | Quien conozca `AuthViewModel` |
| 4 Tipos de ejercicio | Medio | Quien hizo las tarjetas de lección |
| 5 Previews y limpieza | Bajo | Cualquiera |

Después de cada fase: `./gradlew assembleDebug` (con `JAVA_HOME=/opt/android-studio/jbr`),
probar la app un minuto y hacer commit. Así, si algo falla, se vuelve atrás una sola fase.
