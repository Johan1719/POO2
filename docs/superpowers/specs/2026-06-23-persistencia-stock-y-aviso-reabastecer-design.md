# Diseño: Persistencia de stock al vender + aviso de reabastecimiento

Fecha: 2026-06-23
Estado: Aprobado por el usuario

## Contexto

Sistema de delivery "La P'tite France" (proyecto POO2). Al generar un pedido el stock de
los productos no se actualiza. Verificación del código actual reveló un problema más
profundo:

- `PedidoRepository.insert()` solo escribe en la tabla `Pedido`; nunca descuenta stock.
- `PanelNuevaVenta.generarPedido()` pasa al controlador únicamente
  `modeloCarrito.getRowCount()` (cantidad de filas) y `totalCarrito`. **Los códigos de
  producto y las cantidades del carrito nunca llegan al controlador.**
- En consecuencia la tabla `Pedido_Producto` queda **vacía**: `PedidoProductoRepository.insert()`
  existe pero ningún flujo de venta lo llama.
- Sin saber qué y cuánto se vendió, hoy es imposible descontar stock.

## Objetivos

1. Persistir los ítems del carrito en `Pedido_Producto` al generar el pedido.
2. Descontar el stock vendido de `Producto.Stock`.
3. Bloquear el pedido si algún producto no tiene stock suficiente (el stock nunca queda
   negativo), con un mensaje que nombre el producto y las cantidades.
4. Tras generar el pedido, si uno o más productos quedaron en stock 0, mostrar una ventana
   de advertencia: "Producto [nombre] ya no tiene stock, se sugiere reabastecer".
5. Si un pedido se cancela (estado `CANCELADO`), **devolver** el stock de sus ítems, porque
   el pedido no se concretó. Y si un pedido cancelado se reactiva, volver a descontarlo.

## Decisiones tomadas

- **Momento del descuento:** el stock se descuenta **al generar** el pedido.
- **Reposición al cancelar:** al pasar un pedido a `CANCELADO` se devuelve el stock; al
  reactivarlo (de `CANCELADO` a un estado activo) se vuelve a descontar.
- **Transacción atómica:** insertar `Pedido` + insertar filas de `Pedido_Producto` +
  descontar stock van en una sola transacción. Si la validación de stock o cualquier paso
  falla → `rollback` y el pedido no se crea. La cancelación/reactivación también es atómica.
- **Bloqueo por stock insuficiente** (no se permite sobre-venta; el stock no queda negativo).
  Aplica tanto al generar como al reactivar un pedido cancelado.
- **Aviso de stock 0 en ventana propia** tras el mensaje de éxito. Solo stock 0 (sin umbral
  de stock bajo).
- **Sin cambios de esquema** de la base de datos.

## Arquitectura

Nueva clase **`VentaRepository`** (paquete `repositories`), dueña de la transacción de
checkout. Razón: la operación toca tres tablas (`Pedido`, `Pedido_Producto`, `Producto`) y
necesita una conexión compartida; los repositorios actuales abren una conexión por método,
así que no comparten transacción. Una clase dedicada mantiene la responsabilidad clara.

Nuevo DTO **`ItemVenta`** (paquete `dtos`): `codProducto`, `nombreProducto`, `cantidad`.
Transporta los ítems del carrito desde la vista hasta el repository.

`VentaRepository` también expone la lógica de cancelación/reactivación de stock, ya que
opera sobre `Pedido_Producto` y `Producto` de forma transaccional.

## Flujo de datos

1. `PanelNuevaVenta.generarPedido()` arma `List<ItemVenta>` leyendo las filas de
   `modeloCarrito` (columnas: Código, Nombre, Cant, Subtotal) y la pasa al controlador.
2. `PedidoController.generarPedido(...)` recibe `List<ItemVenta>` (en vez del conteo),
   valida lo básico, delega en `VentaRepository.registrarVenta(pedido, items)` y devuelve
   `List<String>` con los nombres de productos que quedaron en 0.
3. `VentaRepository.registrarVenta(Pedido, List<ItemVenta>)` → en una transacción:
   - Para cada ítem: `SELECT NombreProd, Stock FROM Producto WHERE CodProducto = ?`.
     Si `Stock < cantidad` → `rollback` + `ValidationException`:
     "Stock insuficiente de [nombre]: hay [stock], pediste [cantidad]".
   - `INSERT INTO Pedido (...) OUTPUT INSERTED.CodPedido VALUES (...)` → recupera el
     `CodPedido` autogenerado por SQL Server.
   - Para cada ítem: `INSERT INTO Pedido_Producto (CodProducto, CodPedido, CantProd)`.
   - Para cada ítem: `UPDATE Producto SET Stock = Stock - ? WHERE CodProducto = ?`.
   - Junta los productos cuyo stock resultante es 0 → los devuelve (`List<String>` nombres).
   - `commit`.

### Cancelación / reactivación de pedido

`PedidoController.actualizarEstadoPedido(codPedido, nuevoEstado)` compara el estado anterior
con el nuevo y, cuando hay cambio de stock, delega en `VentaRepository`:

- **Activo → `CANCELADO`:** `VentaRepository.reponerStockPorCancelacion(codPedido)` en una
  transacción: lee las filas de `Pedido_Producto` del pedido, hace
  `UPDATE Producto SET Stock = Stock + CantProd` por cada ítem, y actualiza el estado del
  pedido a `CANCELADO`. Si el pedido ya estaba `CANCELADO`, no hace nada (evita doble
  reposición).
- **`CANCELADO` → activo (reactivación):** `VentaRepository.descontarStockPorReactivacion(codPedido, nuevoEstado)`
  en una transacción: valida stock de cada ítem (bloquea con mensaje si no alcanza), descuenta
  `Stock = Stock - CantProd` y actualiza el estado.
- **Cualquier otra transición** (sin cruzar el límite de `CANCELADO`): solo actualiza el
  estado, como hoy.

## Manejo de errores

- Stock insuficiente en cualquier ítem → no se genera el pedido; mensaje nombrando producto
  y cantidades (hay/pediste).
- Cualquier `SQLException` durante la transacción → `rollback`; el pedido no queda a medias.
- Carrito vacío / total <= 0 / cliente nulo → validación previa (ya existente).

## Interfaz de usuario

- Tras "¡Pedido generado exitosamente!", si la lista de productos en 0 no está vacía, se
  muestra un `JOptionPane.WARNING_MESSAGE` con una línea por producto:
  "⚠️ Producto [nombre] ya no tiene stock, se sugiere reabastecer."
- El carrito se limpia y el menú se recarga (ya ocurre vía `limpiarPantalla`), reflejando el
  stock actualizado.

## Pruebas / verificación (manual)

- Venta normal: generar pedido → `Pedido_Producto` tiene las filas, `Producto.Stock` bajó la
  cantidad vendida.
- Stock insuficiente: pedir más que el stock → no se crea el pedido; mensaje con hay/pediste;
  el stock no cambia.
- Stock a 0: vender exactamente el stock restante → pedido creado + ventana de aviso de
  reabastecimiento para ese producto; el producto desaparece del menú (filtra stock > 0).
- Cancelación: marcar un pedido como `CANCELADO` en el monitor → el stock de sus productos
  vuelve a subir por la cantidad vendida.
- Reactivación: pasar un pedido `CANCELADO` a un estado activo → el stock se vuelve a
  descontar (y se bloquea si ya no hay stock suficiente).

## Fuera de alcance

- Umbral de stock bajo (>0).
- Reabastecimiento automático.
- Reposición de stock al **eliminar** físicamente un pedido (`deleteById`); solo se contempla
  la transición de estado a `CANCELADO`.

## Idea futura (diferida, no en este trabajo)

- **Auditoría de pedidos a archivo:** registrar los pedidos concretados (ENTREGADO) y
  rechazados/cancelados en un archivo `.txt` o en un Excel que lleve las finanzas. El usuario
  pidió dejarlo para después y definir entonces el formato y la implementación.
