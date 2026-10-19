# Catálogo de retos

Referencia para el Paso 0 de `/reto-codigo`. Cada tipo tiene pedidos de ejemplo (redactados como se le dicen al estudiante: pantalla y comportamiento, sin archivos ni clases) y los **criterios internos** que usas para evaluar — los criterios no se le muestran al estudiante.

Los ejemplos son guía, no un menú fijo: adapta el pedido a las pantallas y datos reales de la app del estudiante (ej. si su app es de productos, la validación será sobre productos, no sobre estudiantes).

## Niveles

- **Nivel 1 — Puntual:** un solo cambio en una pantalla.
- **Nivel 2 — Atraviesa la app:** un solo cambio que pasa por varias partes (pantalla → modelo → donde se guardan los datos → donde se muestran).

Empieza por Nivel 1. Sube a Nivel 2 cuando el estudiante haya resuelto bien al menos un reto de Nivel 1.

---

## 1. Validaciones · Nivel 1

Requiere: la app tiene un formulario.

Pedidos de ejemplo:
- "En el registro, el correo solo debe aceptar cuentas `@gmail.com`; si no, muestra un mensaje."
- "El DNI debe tener exactamente 8 dígitos y solo números."
- "La contraseña debe tener al menos 8 caracteres."
- "El campo 'confirmar contraseña' debe coincidir con la contraseña."
- "La edad debe estar entre 18 y 99."
- "No se debe poder guardar si el nombre está vacío."

Criterios internos:
- El mensaje de error es visible (junto al campo o en la pantalla).
- Con datos inválidos, no se guarda.
- Al corregir el dato, el mensaje desaparece.

## 2. Campo nuevo de punta a punta · Nivel 2

Requiere: la app tiene un formulario que guarda datos (base de datos local o API).

Pedidos de ejemplo:
- "Agrega el campo 'teléfono' al formulario de estudiantes: debe guardarse y verse en la lista."
- "Agrega un campo 'activo / inactivo' que se pueda marcar y que se guarde."

Criterios internos:
- El campo aparece en el formulario.
- El dato llega al modelo.
- Se guarda de verdad (sigue ahí al cerrar y volver a abrir la app, o al volver a consultar la API).
- Se muestra donde se listan o ven los registros.
- Los registros que ya existían no rompen la app.

## 3. Navegación · Nivel 1 → 2

Requiere: nada (Nivel 1). Para la variante con argumentos, que el estudiante haya visto navegación.

Pedidos de ejemplo:
- "Agrega un botón que abra una pantalla nueva con el mensaje 'Hola Nueva pantalla'."
- Variante: "…y que esa pantalla tenga un botón para volver."
- Variante: "…y que diga 'Hola, {nombre}' usando el nombre escrito en el formulario."
- Variante: "En vez de una pantalla, que salga una ventana emergente con el mensaje."

Criterios internos:
- El botón o link abre la pantalla o el diálogo.
- El mensaje es visible.
- Se puede volver atrás (botón o gesto).
- En la variante con nombre: el dato llega correcto a la otra pantalla.

## 4. Estados de carga y error · Nivel 1

Requiere: la app consume una API o hace alguna operación que tarda.

Pedidos de ejemplo:
- "Mientras se espera la respuesta de la API, muestra un indicador de carga."
- "Si no hay internet o la API falla, muestra un mensaje de error en lugar de que la app se cierre."
- "Agrega un botón 'Reintentar' cuando falle la carga."
- "Desactiva el botón Guardar mientras se está guardando, para evitar registros duplicados."

Criterios internos:
- El indicador aparece al empezar y desaparece al terminar (tanto si sale bien como si falla).
- Un error no cierra la app; se muestra un mensaje.
- El botón se vuelve a activar cuando termina la operación.

## 5. Retroalimentación al usuario · Nivel 1

Requiere: la app tiene alguna acción de guardar o eliminar.

Pedidos de ejemplo:
- "Al guardar, muestra el mensaje 'Guardado correctamente'."
- "Antes de eliminar, pregunta '¿Seguro que deseas eliminar?'."
- "Después de guardar, limpia el formulario."

Criterios internos:
- El mensaje aparece después de la acción, no antes ni si falló.
- Si el usuario cancela la confirmación, no se borra nada.
- Tras guardar, los campos quedan vacíos.

## 6. Mejoras a una lista · Nivel 1

Requiere: la app muestra una lista de registros.

Pedidos de ejemplo:
- "Si la lista está vacía, muestra 'No hay registros todavía'."
- "Muestra arriba el total: 'Tienes 5 productos'."
- "Agrega un buscador que filtre por nombre mientras escribes."
- "Ordena la lista alfabéticamente."

Criterios internos:
- Funciona con la lista vacía y con la lista llena.
- El contador se actualiza al agregar o borrar.
- El filtro no borra los datos originales (al limpiar el buscador vuelven todos).

## 7. Presentación de datos · Nivel 1

Requiere: la app muestra datos (precios, cantidades, textos).

Pedidos de ejemplo:
- "Muestra los precios como `S/ 12.50`, siempre con 2 decimales."
- "Si el stock es menor a 5, muestra el número en rojo."
- "Agrega un ícono para mostrar u ocultar la contraseña."

Criterios internos:
- El formato es correcto en todos los casos (ej. `12` → `S/ 12.00`).
- La condición cambia la vista en el momento justo (ej. pasa a rojo exactamente al bajar de 5).
