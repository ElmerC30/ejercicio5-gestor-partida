# Refactorización del GestorPartida

Los diagramas de clases de antes y después están en `diagramas-gestor-partida.mdj` (StarUML), en los diagramas **Antes** y **Después**. Cada cambio lleva una nota con el principio que aplica.

| Cambio | Principio | Por qué |
|---|---|---|
| Las reglas (sacar con 5, puede mover, meta exacta, 6 vuelve a tirar, captura, ganar) salen de `GestorPartida` a `ReglasParchis`. | SRP | `ReglasParchis` solo cambia si cambian las reglas. |
| El envío por socket y el modo espectador salen de `GestorPartida` a `ComunicacionSocket`. | SRP | `ComunicacionSocket` solo cambia si cambia la red. |
| Los textos `"rojo"`/`"azul"` y todo lo que depende del color (su `Color`, casilla de salida, base, pasillo y lugar en la meta) pasan al enum `ColorFicha`. | DRY | El color se define en un solo lugar en vez de en doce. |
| La regla "¿puede mover esta ficha?" estaba copiada en `procesarDado`, `mousePressed` y `dibujarFichas`; ahora es `ReglasParchis.puedeMover`. Lo mismo con "con 6 vuelve a tirar" (`vuelveATirar`). | DRY | Si cambia la regla, se corrige en un solo método. |
| Se eliminan `JugadorRojo` y `JugadorAzul`; `Jugador` es una clase concreta que **tiene** un `ColorFicha`. | Composición sobre herencia | Un color nuevo es otro valor del enum, no otra subclase. |
| `GestorPartida` depende de la interfaz `Comunicacion` y la recibe por su constructor; `main` decide usar `ComunicacionSocket`. | Diseñar orientado a la interfaz | Se puede cambiar el socket por otro canal, o por un objeto de prueba, sin tocar `GestorPartida`. |

## Clases resultantes

- `ColorFicha` (nueva)
- `ReglasParchis` (nueva)
- `Comunicacion` (nueva, interfaz)
- `ComunicacionSocket` (nueva)
- `Jugador` (modificada: de abstracta a concreta)
- `GestorPartida` (modificada: ya no tiene reglas, colores ni socket)

El juego, los mensajes de la bitácora, los mensajes de red (`DADO;rojo;5`, `MOVIMIENTO;...`) y la tabla `estadisticas` siguen igual que antes.

Queda en `GestorPartida` el dibujo del tablero y el acceso a MySQL; un siguiente paso sería separarlos en una `VistaTablero` y un `EstadisticasDAO`.
