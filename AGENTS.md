# QchuaGo

QchuaGo es una aplicación Android para aprendizaje de quechua.

## Stack actual

El proyecto utiliza únicamente las siguientes tecnologías comprobadas en la base de código:

- **Kotlin:** Lenguaje de desarrollo principal (v2.2.10).
- **Jetpack Compose:** Framework declarativo para la UI (BOM `2026.02.01`).
- **Material 3:** Sistema de diseño para componentes UI.
- **Navigation Compose:** Gestión de rutas y pantallas (`androidx.navigation:navigation-compose:2.7.7`).
- **MVVM básico:** Patrón de arquitectura Model-View-ViewModel sin frameworks complejos de inyección de dependencias.
- **Firebase Authentication:** Autenticación de usuarios mediante Email/Password (`com.google.firebase:firebase-auth`).
- **Cloud Firestore:** Integrado como dependencia (`com.google.firebase:firebase-firestore`) para almacenamiento y persistencia de datos.
- **Gradle Kotlin DSL:** Configuración de compilación basada en scripts `.gradle.kts` e inventario `libs.versions.toml`.

## Fuente de verdad

Antes de realizar cambios importantes, cualquier agente o desarrollador debe revisar el archivo:

`docs/PROJECT_STATUS.md`

Este documento contiene la descripción detallada del estado real y actualizado del proyecto.

## Arquitectura actual

El código fuente en `app/src/main/java/com/proyect/qchuago/` respeta la siguiente estructura modular por paquetes:

```text
data/
  entidad/      # Clases de modelo/datos (ej. Usuario)
  servicio/     # Clases de servicio para integración externa (ej. AuthService)

ui/
  navigation/   # Rutas y contenedor de navegación (AppNavigation.kt)
  screens/      # Pantallas Compose (LoginScreen, RegisterScreen, HomeScreen)
  theme/        # Colores, tipografía y tema visual (Theme.kt, Color.kt, Type.kt)
  viewmodel/    # ViewModels para gestión de estado de la UI (AuthViewModel)
```

**Regla de oro:** No cambiar ni reorganizar esta estructura sin una razón importante y explícita.

## Reglas de desarrollo

- **Sencillez:** Mantener el proyecto sencillo y directo. Evitar la sobrearquitectura.
- **Sin capas innecesarias:** No agregar Hilt, Koin, patrones Repository complejas, UseCases u otras capas adicionales salvo que se solicite explícitamente.
- **MVVM básico:** Mantener la arquitectura en el esquema simple: `Servicio <-> ViewModel <-> Composable`.
- **Aislamiento de Firebase:** Las pantallas Compose **nunca** deben acceder directamente a la SDK de Firebase ni ejecutar lógica de negocio de backend.
- **Uso de Servicios:** Todo acceso a Firebase o APIs externas debe realizarse a través de clases en `data/servicio/`.
- **Coordinación en ViewModel:** Los ViewModel coordinan el estado de la UI y los servicios.
- **Consistencia:** Mantener las convenciones de nombres, idioma (español para respuestas e interfaz) y estilo existentes en el proyecto.
- **Reutilización:** Reutilizar el código y componentes existentes antes de crear nuevas soluciones.
- **Foco:** No modificar funcionalidades que no formen parte del requerimiento actual.
- **Integridad:** No eliminar ni alterar código funcional sin una justificación clara.
- **Dependencias:** Evitar agregar dependencias Gradle innecesarias.
- **Concurrencia:** Utilizar corrutinas de Kotlin (`suspend`, `viewModelScope`, `Flow`/`StateFlow`) para operaciones asíncronas.
- **Manejo de estados:** Manejar siempre adecuadamente los estados de carga (*loading*), éxito (*success*) y error (*error*).
- **Idioma del usuario:** Todos los textos e interfaces mostrados al usuario final deben estar en español.
- **Compatibilidad:** Mantener compatibilidad con el `minSdk` del proyecto (`minSdk 29` - Android 10).

## Jetpack Compose

- **Archivos independientes:** Cada pantalla importante debe residir en su propio archivo dentro de `ui/screens/` (ej. `LoginScreen.kt`, `HomeScreen.kt`).
- **Nombres claros:** Usar nombres descriptivos que terminen en `Screen` para las pantallas principales.
- **Modularidad:** Dividir los composables en funciones más pequeñas cuando un archivo o función sea demasiado grande.
- **Reutilización:** Priorizar la creación de componentes reutilizables cuando exista repetición real de UI.
- **Elevación de estado:** Mantener los estados con lógica de negocio o persistencia en el ViewModel.
- **Lógica limpia:** Evitar cualquier tipo de lógica Firebase o de red dentro de funciones composables.

## Firebase

- **Autenticación:** Utilizar Firebase Auth para todo el manejo de identidad de usuario.
- **Firestore:** Utilizar Cloud Firestore para almacenar información adicional que requiera persistencia.
- **Seguridad:** Nunca colocar claves, secretos ni credenciales sensibles directamente en código Kotlin.
- **Encapsulamiento:** Mantener todo acceso a Firebase estrictamente dentro de la carpeta `data/servicio/`.
- **Manejo de errores:** Capturar las excepciones de Firebase y traducirlas a mensajes amigables y comprensibles en español.

## Navegación

- Centralizar la configuración de rutas y el `NavHost` dentro de `ui/navigation/AppNavigation.kt`.
- No instanciar objetos `NavController` independientes sin necesidad.
- Evitar la duplicación de rutas o constantes de navegación.

## Antes de implementar un requerimiento

Cualquier agente debe seguir estrictamente este orden:

1. Leer `AGENTS.md`.
2. Leer `docs/PROJECT_STATUS.md`.
3. Revisar las clases existentes relacionadas con el requerimiento.
4. Identificar lo que ya está implementado.
5. Implementar únicamente lo necesario para satisfacer la solicitud.
6. Verificar que el proyecto compile correctamente.
7. Actualizar `docs/PROJECT_STATUS.md` si la implementación modificó o amplió el estado del proyecto.
