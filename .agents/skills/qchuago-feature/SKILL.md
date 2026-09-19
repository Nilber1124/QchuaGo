---
name: qchuago-feature
description: Guiar la implementación de nuevos requerimientos funcionales en el proyecto QchuaGo respetando la arquitectura y reglas existentes.
---

# Skill: qchuago-feature

Esta skill guía el flujo paso a paso para la implementación de nuevos requerimientos funcionales en la aplicación QchuaGo.

## Flujo de implementación

1. **Leer AGENTS.md:** Revisar las reglas de desarrollo y directrices del proyecto.
2. **Leer docs/PROJECT_STATUS.md:** Comprender el estado actual de la base de código y las características existentes.
3. **Analizar el requerimiento solicitado:** Comprender en detalle la funcionalidad requerida.
4. **Identificar archivos existentes relacionados:** Localizar entidades, servicios, ViewModels o pantallas que puedan relacionarse.
5. **Determinar archivos a modificar:** Identificar de forma precisa qué archivos existentes requieren cambios.
6. **Determinar si realmente hacen falta archivos nuevos:** Crear únicamente los componentes (entidades, servicios, ViewModels, pantallas) indispensables.
7. **Mantener la arquitectura actual:** Respetar la estructura MVVM básica (`data/entidad`, `data/servicio`, `ui/viewmodel`, `ui/screens`, `ui/navigation`).
8. **Implementar lógica primero:** Crear/actualizar entidades, servicios en `data/` y la gestión de estado en el `ViewModel`.
9. **Implementar o adaptar UI:** Crear o ajustar las pantallas Compose conectándolas a los `StateFlow` del ViewModel.
10. **Manejar estados:** Asegurar la gestión clara de estados de carga (*loading*), error (*error*) y éxito (*success*).
11. **Comprobar navegación:** Conectar las nuevas pantallas en `AppNavigation.kt` si aplica.
12. **Comprobar persistencia:** Verificar que la información en Firebase Auth o Firestore persista adecuadamente.
13. **Compilar el proyecto:** Verificar que el proyecto compile sin errores.
14. **Corregir errores introducidos:** Resolver cualquier problema de compilación o tipo detectado.
15. **Actualizar PROJECT_STATUS.md:** Documentar el nuevo estado del proyecto tras finalizar la característica.

## Reglas obligatorias

- **No implementar requerimientos adicionales:** Limitarse estrictamente a lo solicitado.
- **No agregar arquitectura innecesaria:** Evitar librerías como Hilt/Koin o patrones como Repository/UseCase salvo solicitud explícita.
- **No inventar funcionalidades:** No añadir características que no forman parte de la especificación.
- **No introducir datos falsos:** Evitar incluir datos falsos o harcodeados para simular funciones inexistentes.
- **Identificar dependencias:** Si una función depende de otro requerimiento aún no implementado, dejar clara la dependencia en la explicación sin desarrollar el requerimiento faltante.
