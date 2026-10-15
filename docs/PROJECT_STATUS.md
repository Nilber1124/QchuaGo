# Estado del proyecto QchuaGo

Última actualización: 15 de octubre de 2026.

## Arquitectura y tecnología

- Aplicación Android en Kotlin con Jetpack Compose y Material 3.
- Navegación principal centralizada en `AppNavigation.kt` y pestañas dentro de `HomeScreen.kt`.
- MVVM básico: servicios de Firebase, ViewModels y pantallas Compose.
- Firebase Authentication para identidad y Cloud Firestore para perfiles y contenido del curso.
- Compatibilidad mínima: Android 10 (API 29).

## Funcionalidades presentes

- RF01-RF03: autenticación, registro y sesión existentes; no fueron modificados por RF06.
- Pantalla Home con las pestañas Inicio, Progreso, Diccionario, Repaso y Perfil.
- Perfil de usuario, estadísticas y preferencia de tema.
- Curso cargado desde Firestore con unidades y subcolecciones de lecciones.
- RF06 - Niveles de aprendizaje:
  - INICIO muestra primero los documentos activos de la colección `niveles`, ordenados por `orden`.
  - Cada nivel presenta nombre, descripción, icono y color.
  - El primer nivel (Básico en los datos actuales) está habilitado.
  - Al elegirlo se consultan las unidades cuyo `nivelId` coincide y se muestran todas con sus lecciones en el diseño existente.
  - La vista del curso incluye una acción para regresar a la selección de niveles.
  - Intermedio y Avanzado permanecen bloqueados porque RF14 todavía no registra lecciones completadas. No se simula progreso ni se agregó un sistema alternativo.
- RF07 - Unidades y temas:
  - Al seleccionar un nivel se muestran directamente todas sus unidades y lecciones, incluidas las bloqueadas.
  - Las unidades se ordenan por `orden` y cada subcolección de lecciones se consulta ordenada por `orden`.
  - El desbloqueo es secuencial: una unidad requiere completar todas las anteriores y una lección requiere completar las que la preceden.
  - Las lecciones se identifican para el progreso mediante la clave `unidadId/leccionId`, evitando colisiones entre documentos de distintas unidades con el mismo ID.
  - No existe acceso al contenido educativo porque RF05 todavía no está implementado.

## Flujo actual de RF06

```text
INICIO -> Seleccionar nivel -> Todas las unidades y lecciones del nivel
                              -> Volver a niveles
```

No existe una pantalla intermedia para seleccionar unidades.

## Pendientes relacionados

- RF14 debe definir y registrar la finalización real de lecciones antes de habilitar niveles posteriores.
- Cuando RF14 esté disponible, deberá persistir en `leccionesCompletadasIds` las claves `unidadId/leccionId` y el desbloqueo de niveles deberá reutilizar ese progreso.
- Faltan pruebas unitarias e instrumentadas específicas para el curso.

## Verificación

- `gradlew.bat assembleDebug`: compilación correcta el 15 de octubre de 2026.
