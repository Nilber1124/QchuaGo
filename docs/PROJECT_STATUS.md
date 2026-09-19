# Estado Actual del Proyecto QchuaGo

## 1. Descripción de QchuaGo

**QchuaGo** es una aplicación móvil para Android orientada al aprendizaje del idioma quechua ("Aprende quechua de forma sencilla"). 

En su estado actual, la aplicación se encuentra en una fase inicial de desarrollo (prototipo funcional) enfocada principalmente en el **módulo de autenticación de usuarios** (registro, inicio de sesión, persistencia y cierre de sesión) integrado con Firebase.

---

## 2. Tecnologías Utilizadas

* **Lenguaje de programación:** Kotlin (v2.2.10).
* **Interfaz de usuario:** Jetpack Compose (BOM `2026.02.01`) con componentes de Material Design 3.
* **Navegación:** Jetpack Navigation Compose (`v2.7.7`).
* **Arquitectura:** MVVM (Model-View-ViewModel) básico.
* **Backend y Servicios (BaaS):**
  * Firebase BoM (`v34.18.0`).
  * Firebase Authentication (Autenticación mediante Email/Contraseña).
  * Firebase Analytics (librería agregada).
  * Cloud Firestore (librería agregada).
* **Herramientas de construcción:** Gradle con Kotlin DSL (`build.gradle.kts`), Android Gradle Plugin (`v9.3.2`).
* **SDK objetivo / mínimo:** `compileSdk` / `targetSdk` 37 (`Android 16`), `minSdk` 29 (`Android 10`).

---

## 3. Árbol de Carpetas Principal

```text
QchuaGo/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── docs/
│   └── PROJECT_STATUS.md
└── app/
    ├── build.gradle.kts
    ├── google-services.json
    └── src/
        ├── androidTest/
        │   └── java/com/proyect/qchuago/ExampleInstrumentedTest.kt
        ├── test/
        │   └── java/com/proyect/qchuago/ExampleUnitTest.kt
        └── main/
            ├── AndroidManifest.xml
            ├── keepRules/
            │   └── rules.keep
            ├── java/com/proyect/qchuago/
            │   ├── MainActivity.kt
            │   ├── data/
            │   │   ├── entidad/
            │   │   │   └── Usuario.kt
            │   │   └── servicio/
            │   │       └── AuthService.kt
            │   └── ui/
            │       ├── navigation/
            │       │   └── AppNavigation.kt
            │       ├── screens/
            │       │   ├── HomeScreen.kt
            │       │   ├── LoginScreen.kt
            │       │   └── RegisterScreen.kt
            │       ├── theme/
            │       │   ├── Color.kt
            │       │   ├── Theme.kt
            │       │   └── Type.kt
            │       └── viewmodel/
            │           └── AuthViewModel.kt
            └── res/
                ├── drawable/
                │   ├── ic_launcher_background.xml
                │   ├── ic_launcher_foreground.xml
                │   └── logo_app.xml
                ├── mipmap-*/
                ├── values/
                │   ├── colors.xml
                │   ├── strings.xml
                │   └── themes.xml
                └── xml/
                    ├── backup_rules.xml
                    └── data_extraction_rules.xml
```

---

## 4. Explicación de las Carpetas Importantes

* `com.proyect.qchuago`: Paquete raíz del código fuente en Kotlin.
* `data/entidad`: Contiene los modelos de datos o entidades del negocio (`Usuario.kt`).
* `data/servicio`: Contiene las clases encargadas de interactuar con servicios externos (`AuthService.kt` para Firebase Auth).
* `ui/navigation`: Maneja la configuración del gráfico de navegación de Jetpack Compose (`AppNavigation.kt`).
* `ui/screens`: Contiene los composables correspondientes a las pantallas de la aplicación (`HomeScreen.kt`, `LoginScreen.kt`, `RegisterScreen.kt`).
* `ui/theme`: Define la paleta de colores, tipografía y tema visual de la aplicación (`Color.kt`, `Theme.kt`, `Type.kt`).
* `ui/viewmodel`: Aloja los ViewModel que gestionan el estado de la UI y la comunicación con la capa de datos (`AuthViewModel.kt`).
* `res/drawable`: Recursos gráficos en formato vectorial, incluyendo el logotipo principal de la aplicación (`logo_app.xml`).
* `res/values`: Definición de cadenas de texto (`strings.xml`), colores de recursos legacy (`colors.xml`) y temas base (`themes.xml`).

---

## 5. Pantallas Existentes

1. **Pantalla de Carga Inicial (Splash / Loading - interna en `AppNavigation.kt`):**
   * Muestra el logo de QchuaGO, el título y un `CircularProgressIndicator`.
   * Se muestra brevemente al abrir la aplicación mientras se verifica si existe una sesión de usuario guardada.

2. **LoginScreen (`ui/screens/LoginScreen.kt`):**
   * Permite ingresar correo electrónico y contraseña.
   * Valida que los campos no estén vacíos.
   * Muestra indicadores de carga y mensajes de error comprensibles cuando falla la autenticación.
   * Incluye enlace para navegar a la pantalla de registro (`RegisterScreen`).

3. **RegisterScreen (`ui/screens/RegisterScreen.kt`):**
   * Formulario para crear una cuenta nueva con: Nombre, Correo electrónico, Contraseña y Confirmación de contraseña.
   * Realiza validaciones previas en la interfaz:
     * Nombre no vacío.
     * Formato válido de correo electrónico.
     * Contraseña con mínimo 6 caracteres.
     * Coincidencia entre la contraseña y la confirmación.
   * Al registrarse con éxito, actualiza el perfil del usuario en Firebase con el nombre ingresado.
   * Incluye enlace para regresar a la pantalla de inicio de sesión.

4. **HomeScreen (`ui/screens/HomeScreen.kt`):**
   * Contenedor principal rediseñado con `Scaffold` y la barra de navegación inferior interactiva (`QchuaGoBottomBar`). Conecta las 5 pestañas activando la pantalla correspondiente según el ítem seleccionado.
   * Contiene los composables vacíos independientes para cada apartado:
     * `InicioScreen.kt` (`ui/screens/InicioScreen.kt`): Vista base del apartado Inicio.
     * `ProgresoScreen.kt` (`ui/screens/ProgresoScreen.kt`): Vista base del apartado Progreso.
     * `DiccionarioScreen.kt` (`ui/screens/DiccionarioScreen.kt`): Vista base del apartado Diccionario.
     * `RepasoScreen.kt` (`ui/screens/RepasoScreen.kt`): Vista base del apartado Repaso.
     * `PerfilScreen.kt` (`ui/screens/PerfilScreen.kt`): Vista de perfil conectada al usuario persistido, organizada en secciones pequeñas de encabezado, estadísticas, progreso, preferencias y diálogos. Conserva edición de nombre, cambio de contraseña, tema y cierre de sesión.
   * Redirige automáticamente a la pantalla de inicio de sesión cuando el usuario cierra su sesión.




---

## 6. Funcionamiento de la Navegación

* La navegación está centralizada en `AppNavigation.kt` utilizando `NavHost` de Jetpack Navigation Compose.
* Rutas definidas en el objeto `Rutas`:
  * `Rutas.SPLASH` (`"splash"`): Ruta inicial.
  * `Rutas.LOGIN` (`"login"`): Pantalla de inicio de sesión.
  * `Rutas.REGISTRO` (`"registro"`): Pantalla de registro de usuario.
  * `Rutas.HOME` (`"home"`): Pantalla principal tras iniciar sesión.
* **Flujo de sesión:**
  * Al arrancar, `AppNavigation` observa `sesionInicializada` en `AuthViewModel`.
  * Cuando se confirma el estado de la sesión, la app navega a `Rutas.HOME` si hay un usuario autenticado, o a `Rutas.LOGIN` si no lo hay, limpiando el historial previo (`popUpTo(navController.graph.id) { inclusive = true }`).
  * Cada pantalla sensible al estado de autenticación usa un `LaunchedEffect(usuario)` para redirigir si el estado del usuario cambia.

---

## 7. Uso de Firebase

* **Firebase Authentication:**
  * Se utiliza el proveedor de correo electrónico y contraseña.
  * Está completamente aislado en `AuthService.kt`. La UI no invoca Firebase directamente.
  * Operaciones implementadas:
    * `iniciarSesion(correo, contrasena)`: Inicia sesión usando `signInWithEmailAndPassword`.
    * `registrar(correo, contrasena, nombre)`: Crea el usuario con `createUserWithEmailAndPassword` y actualiza su nombre de visualización con `updateProfile`.
    * `cerrarSesion()`: Cierra la sesión activa mediante `signOut`.
  * Conversión de tareas asíncronas de Firebase (`Task`) a corrutinas de Kotlin (`suspendCancellableCoroutine`).
  * Traducción de códigos de error de Firebase (`FirebaseAuthException`) a mensajes amigables en español.
* **Cloud Firestore:**
  * `FirestoreService.kt` mantiene documentos `usuarios/{uid}` con `nombre`, `correo`, `temaOscuro`, `nivel`, `xp`, `racha`, `vidas`, `leccionesCompletadas` y `totalLecciones`.
  * Al cargar una sesión, crea el documento solo si falta y completa únicamente campos ausentes, sin reiniciar estadísticas existentes.
* **Firebase Analytics:**
  * La dependencia está incluida en `app/build.gradle.kts`, pero **no se registra ningún evento personalizado** en el código fuente.
* **Configuración:**
  * Archivo `app/google-services.json` configurado para el proyecto Firebase `quechuago` y paquete `com.proyect.qchuago`.

---

## 8. Componentes Importantes Existentes

* **`Usuario.kt`:** Modelo de datos de perfil con identidad, preferencia de tema, nivel y estadísticas.
* **`AuthService.kt`:** Servicio encapsulated para abstraer Firebase Authentication y retornar resultados envueltos en la clase `Result<Usuario>`.
* **`FirestoreService.kt`:** Servicio encapsulado para cargar, crear de forma segura y actualizar campos del perfil en Cloud Firestore, manteniendo explícitos los campos faltantes sin reiniciar estadísticas.
* **`AuthViewModel.kt`:** ViewModel que mantiene y expone los estados reactivos `sesionInicializada`, `usuario`, `temaOscuro`, `cargando` y `error` como `StateFlow`.
* **`logo_app.xml`:** Vector gráfico con diseño nativo (sol andino dorado y montañas quechuas en tonos verde/azul) utilizado en la pantalla de carga y de login.
* **`QchuaGOTheme`:** Definición del tema Material 3 con esquemas claro/oscuro propios, controlados por la preferencia persistida del perfil.

---

## 9. Funcionalidades que Ya Funcionan

* Verification automática de sesión al abrir la app.
* Registro de usuarios con validaciones de formulario en el cliente y creación de cuenta en Firebase Auth.
* Guardado del nombre de usuario en el perfil de Firebase Auth (`displayName`).
* Inicio de sesión con correo y contraseña.
* Manejo y visualización de errores de autenticación en español.
* Cierre de sesión.
* Navegación reactiva según el estado de autenticación.
* Perfil persistido en Firestore sin reemplazar el progreso existente.
* Visualización de nivel, XP, racha, vidas, lecciones y progreso del usuario.
* Sincronización del nombre entre Firebase Auth y Firestore, y cambio seguro de contraseña.
* Tema claro/oscuro aplicado de inmediato y persistido por usuario.

---

## 10. Funcionalidades Incompletas o No Implementadas

* **Lecciones / Contenido de Quechua:** No existe ninguna pantalla, modelo de datos, ni lógica referente a cursos, vocabulario, módulos o ejercicios de enseñanza de quechua.
* **Eventos de Analytics:** No hay código para el rastreo de eventos o métricas de uso de la app.
* **Foto de perfil:** No está implementada; el avatar actual usa las iniciales del nombre.

---

## 11. Problemas, TODOs o Código Pendiente

1. **Instanciación directa del servicio en ViewModel:**
   * En `AuthViewModel.kt`, se realiza `AuthService()` directamente en los parámetros por defecto. No se utiliza un framework de inyección de dependencias (como Hilt o Koin).
2. **Escuchador de cambios de autenticación en tiempo real:**
   * `AuthViewModel` asigna `authService.usuarioActual` únicamente en su inicialización (`init`). No está suscrito a un `AuthStateListener` de Firebase para reaccionar inmediatamente a revocaciones de token o cambios externos de sesión.
3. **Pruebas unitarias e instrumentadas de ejemplo:**
   * Las clases `ExampleUnitTest.kt` y `ExampleInstrumentedTest.kt` contienen únicamente el código generado por defecto por Android Studio y no prueban la lógica real de la app.
4. **Reglas R8/ProGuard:**
   * El archivo `rules.keep` está vacío/en su plantilla por defecto.
