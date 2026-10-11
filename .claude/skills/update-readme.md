# Skill: update-readme

Actualiza `README.md` cuando el estado real del proyecto haya cambiado respecto a lo documentado.

## Cuándo usar esta skill

Invoca `/update-readme` (o actívala manualmente) después de:
- Agregar o eliminar pantallas
- Implementar funcionalidades de la lista "Pendiente"
- Cambiar versiones de dependencias en `libs.versions.toml`
- Modificar la arquitectura o estructura de carpetas

## Proceso

Sigue exactamente estos pasos en orden:

### 1. Leer las fuentes de verdad

Lee los tres archivos en paralelo:
- `docs/PROJECT_STATUS.md` — estado actual oficial del proyecto
- `README.md` — lo que está documentado públicamente ahora
- `gradle/libs.versions.toml` — versiones reales de dependencias

### 2. Identificar diferencias relevantes

Compara `PROJECT_STATUS.md` con `README.md` y busca discrepancias en:

| Sección del README | Fuente a comparar |
|---|---|
| Tabla de versiones (Stack) | `libs.versions.toml` — sección `[versions]` |
| Tabla de pantallas | Sección "5. Pantallas Existentes" de PROJECT_STATUS |
| Funcionalidades implementadas | Sección "9. Funcionalidades que Ya Funcionan" |
| Pendiente | Sección "10. Funcionalidades Incompletas" |
| Descripción de Firebase | Sección "7. Uso de Firebase" |

### 3. Decidir si actualizar

**Actualiza** si hay al menos una de estas diferencias:
- Una pantalla aparece en PROJECT_STATUS pero no en el README (o viceversa)
- Una funcionalidad pasó de "pendiente" a "implementada"
- Una versión de dependencia cambió
- Se agregó o eliminó un servicio de Firebase

**No actualices** si los cambios son solo:
- Reescrituras de código sin cambio funcional (refactors, renombres)
- Correcciones de bugs internos sin impacto en features
- Cambios en archivos de test o configuración de build menores

### 4. Aplicar los cambios

Si hay diferencias relevantes, edita únicamente las secciones afectadas de `README.md`. No reescribas secciones que no cambiaron.

Reglas de estilo del README:
- Mantén las tablas en formato markdown
- Las funcionalidades implementadas usan `- [x]`, las pendientes `- [ ]`
- No agregues secciones nuevas salvo que corresponda a una nueva categoría mayor (ej. nueva integración de servicio)
- El tono es directo, sin texto de relleno

### 5. Reportar

Al final responde con una de estas dos opciones (nada más):

**Si se actualizó:**
```
README actualizado. Cambios: <lista de 1-3 ítems concretos, ej. "pantalla X agregada", "versión Y de Z actualizada">
```

**Si no hay diferencias relevantes:**
```
README al día. Sin cambios necesarios.
```
