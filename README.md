# QchuaGo

Aplicación Android para aprender quechua de forma sencilla e interactiva.

> Estado actual: prototipo funcional — módulo de autenticación completo, contenido de lecciones pendiente.

---

## Stack

| Tecnología | Versión |
|---|---|
| Kotlin | 2.2.10 |
| Jetpack Compose BOM | 2026.02.01 |
| Material 3 | — |
| Navigation Compose | 2.7.7 |
| Firebase BoM | 34.18.0 |
| Android Gradle Plugin | 9.3.3 |
| minSdk / targetSdk | 29 / 37 |

---

## Arquitectura

MVVM básico sin frameworks de inyección de dependencias.

```
Servicio  ←→  ViewModel  ←→  Composable
```

```
app/src/main/java/com/proyect/qchuago/
├── data/
│   ├── entidad/        # Modelos de datos (Usuario)
│   └── servicio/       # Acceso a Firebase (AuthService, FirestoreService)
├── ui/
│   ├── navigation/     # NavHost y rutas (AppNavigation)
│   ├── screens/        # Pantallas Compose
│   ├── theme/          # Colores, tipografía, tema
│   └── viewmodel/      # Estado reactivo (AuthViewModel)
└── MainActivity.kt
```

---

## Pantallas

| Pantalla | Descripción |
|---|---|
| Splash | Verificación de sesión al arrancar |
| Login | Inicio de sesión con email/contraseña |
| Registro | Creación de cuenta con validaciones |
| Home | Contenedor con barra de navegación inferior (5 pestañas) |
| Inicio | Placeholder — contenido de lecciones pendiente |
| Progreso | Placeholder |
| Diccionario | Placeholder |
| Repaso | Placeholder |
| Perfil | Estadísticas, edición de nombre, cambio de contraseña, tema claro/oscuro, cierre de sesión |

---

## Firebase

- **Auth**: email/contraseña, completamente aislado en `AuthService`.
- **Firestore**: colección `usuarios/{uid}` con perfil, nivel, XP, racha, vidas y lecciones. Acceso exclusivo en `FirestoreService`.
- **Analytics**: dependencia incluida, sin eventos personalizados implementados aún.

> Las credenciales (`google-services.json`) no están en el repositorio. Configura tu propio proyecto Firebase y coloca el archivo en `app/`.

---

## Configuración del entorno

### Prerrequisitos
- Android Studio Ladybug o superior
- JDK 17
- Cuenta en [Firebase Console](https://console.firebase.google.com/)

### Pasos

1. Clona el repositorio:
   ```bash
   git clone <url-del-repo>
   cd QchuaGo
   ```

2. Crea un proyecto en Firebase Console con el paquete `com.proyect.qchuago`.

3. Habilita **Authentication → Email/Password** y **Cloud Firestore**.

4. Descarga `google-services.json` y colócalo en `app/`.

5. Abre el proyecto en Android Studio y ejecuta en un dispositivo/emulador con API 29+.

---

## Funcionalidades implementadas

- [x] Verificación automática de sesión al abrir la app
- [x] Registro con validaciones de cliente y persistencia en Firestore
- [x] Inicio de sesión con mensajes de error en español
- [x] Perfil de usuario (nombre, XP, racha, vidas, lecciones)
- [x] Edición de nombre y cambio de contraseña
- [x] Tema claro/oscuro persistido por usuario
- [x] Cierre de sesión con redirección automática
- [x] Optimistic UI con rollback automático en errores
- [x] Listener de estado de auth para revocar sesiones externas

## Pendiente

- [ ] Lecciones y contenido de quechua
- [ ] Foto de perfil
- [ ] Eventos de Analytics
- [ ] Tests unitarios e instrumentados reales
- [ ] Reglas R8/ProGuard

---

## Documentación interna

- [`AGENTS.md`](AGENTS.md) — reglas de desarrollo para agentes e IA
- [`docs/PROJECT_STATUS.md`](docs/PROJECT_STATUS.md) — estado detallado del proyecto
