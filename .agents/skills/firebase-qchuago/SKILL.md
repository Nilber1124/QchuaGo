---
name: firebase-qchuago
description: Guiar la integración de servicios de Firebase (Authentication y Cloud Firestore) en QchuaGo manteniendo el aislamiento en la capa de servicios.
---

# Skill: firebase-qchuago

Esta skill establece las reglas y el flujo de trabajo para implementar cualquier funcionalidad relacionada con Firebase dentro de QchuaGo.

## Uso actual en el proyecto

- **Firebase Authentication:** Utilizado para la gestión de identidad de usuarios (registro e inicio de sesión con Email y Contraseña).
- **Cloud Firestore:** Integrado como dependencia en el proyecto (`firebase-firestore`) para almacenar información adicional persistente.

## Reglas de desarrollo con Firebase

- **Servicios encapsulados:** Todas las llamadas a las SDKs de Firebase deben residir estrictamente dentro de clases en `data/servicio/` (ej. `AuthService.kt`).
- **Consumo desde ViewModel:** Los `ViewModel` consumen los servicios en `data/servicio/`.
- **Prohibición en UI:** La capa visual de Jetpack Compose **nunca** debe importar ni llamar directamente a clases o SDKs de Firebase.
- **Asociación por UID:** Utilizar siempre el `uid` del usuario autenticado (`auth.currentUser?.uid`) para vincular documentos e información personal.
- **Seguridad de datos:**
  - **Nunca** guardar contraseñas en Cloud Firestore.
  - **Nunca** guardar claves, API keys o secretos en documentos de Firestore.
- **Manejo de errores:** Capturar las excepciones de Firebase y devolver mensajes claros traducidos al español para el usuario final.
- **Verificación de existencia:** No asumir que un documento de Firestore existe; validar siempre si el snapshot contiene datos antes de mapear.
- **Operaciones CRUD:** Diferenciar explícitamente entre creación (`set`), lectura (`get`) y actualización (`update`).
- **Lecturas vs. Listeners:**
  - Evitar listeners en tiempo real (`addSnapshotListener`) si una lectura puntual es suficiente para el caso de uso.
  - Utilizar listeners en tiempo real únicamente cuando el requerimiento exija actualización reactiva e inmediata en vivo.

## Pasos para implementar nuevas funciones con Firebase

1. **Revisar servicios existentes:** Inspeccionar `data/servicio/` (ej. `AuthService.kt`) para determinar si la función corresponde a un servicio existente.
2. **Reutilizar o crear servicio:**
   - Reutilizar el servicio si pertenece al mismo dominio.
   - Crear un nuevo servicio en `data/servicio/` si responde a una responsabilidad totalmente distinta.
3. **Crear o actualizar entidad:** Definir o ajustar las clases de modelo en `data/entidad/`.
4. **Conectar con ViewModel:** Agregar métodos en el `ViewModel` correspondiente que invoquen los métodos suspendidos del servicio.
5. **Exponer estados a Compose:** Representar el resultado de la operación mediante `StateFlow` (`cargando`, `error`, `resultado`).
6. **Manejar loading/error/success:** Asegurar el control de la interfaz según el estado reportado por el ViewModel.
7. **Verificar la persistencia:** Probar la funcionalidad cerrando y reabriendo la pantalla o la sesión para validar que los datos se almacenaron correctamente.
