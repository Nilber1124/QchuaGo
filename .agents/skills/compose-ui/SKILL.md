---
name: compose-ui
description: Guiar la creación y modificación de interfaces de usuario con Jetpack Compose en QchuaGo de acuerdo con Material 3 y las pautas del proyecto.
---

# Skill: compose-ui

Esta skill establece las pautas para diseñar, crear y modificar componentes y pantallas de interfaz de usuario con Jetpack Compose en QchuaGo.

## Reglas de desarrollo UI

- **Material 3 y tema:** Respetar la configuración de Material 3 definida en `Theme.kt`, `Color.kt` y `Type.kt`. Utilizar `MaterialTheme.colorScheme` y `MaterialTheme.typography`.
- **Reutilización:** Usar componentes existentes de la aplicación antes de crear componentes nuevos.
- **Componentes reutilizables:** Crear componentes reutilizables solo cuando exista repetición real en la interfaz.
- **Claridad y tamaño:** Mantener los composables comprensibles. Evitar archivos o funciones gigantescas dividiendo componentes internos en el mismo archivo o en subcomposables auxiliares.
- **Aislamiento de lógica:** Evitar cualquier lógica de negocio o acceso directo a Firebase dentro de la UI.
- **Estados en ViewModel:** Los estados de UI con lógica o persistencia deben provenir del `ViewModel` (mediante `StateFlow` recolectados con `collectAsState()`).
- **Control de estados de UI:** Incluir siempre la representación visual para estados de carga (`CircularProgressIndicator`), error (`Text` con color de error) y éxito.
- **Consistencia visual:** Mantener la coherencia estética con las pantallas existentes (`LoginScreen`, `RegisterScreen`, `HomeScreen`).
- **Guía por capturas/diseño:** Si el usuario proporciona una imagen o captura de referencia, utilizarla como guía visual sin alterar innecesariamente el estilo general de la aplicación.

## Pasos para implementar o adaptar una pantalla

1. **Revisar el tema existente:** Inspeccionar `ui/theme/Theme.kt`, `Color.kt` y `Type.kt` para usar los colores y estilos adecuados.
2. **Revisar pantallas existentes:** Inspeccionar las pantallas en `ui/screens/` para mantener patrones de diseño homogéneos.
3. **Revisar el ViewModel asociado:** Verificar los estados (`StateFlow`) y funciones expuestas por el `ViewModel` en `ui/viewmodel/`.
4. **Crear o adaptar la UI:** Escribir el composable de la pantalla principal en su propio archivo dentro de `ui/screens/`.
5. **Conectar estados:** Vincular las propiedades del ViewModel a la pantalla y delegar eventos a través de callbacks (ej. `onIniciarSesion`, `onRegistrar`).
6. **Verificar navegación:** Conectar la pantalla en `AppNavigation.kt` pasándole las rutas y parámetros correspondientes.
7. **Probar compatibilidad:** Verificar la legibilidad y presentación tanto en modo claro como en modo oscuro.
