# Diseño: Mejoras de venta (alerta stock, total con delivery), PedidoBuilder, filtro CANCELADO y renombre de zonas

Fecha: 2026-06-23
Estado: Aprobado por el usuario

## Contexto

Cinco mejoras pedidas sobre el sistema de delivery "La P'tite France" (POO2):

1. La validación de stock recién ocurre al generar el pedido; debe avisar **al ingresar la
   cantidad** en el carrito.
2. Cuando es delivery con costo (tarifa con precio > 0), el cliente no ve el **total con
   envío**. Debe visualizarse y guardarse el monto total (productos + envío).
3. Mover la construcción de `Pedido` al patrón Builder, en un archivo `PedidoBuilder` aparte.
4. El filtro de estado del monitor no incluye `CANCELADO`.
5. Las zonas de delivery se llaman `Huaral Centro` / `Alrededores Huaral`; deben ser
   `Lima Metropolitana` / `Callao`.
6. No hay forma de vaciar el carrito completo para empezar de cero; solo se puede quitar
   ítem por ítem. Falta un botón "Vaciar carrito".

## Decisiones tomadas

- **Total con delivery:** `montoPedido = total de productos + precio de la tarifa`. Es el
  total que paga el cliente; se guarda y se muestra. En recojo la tarifa vale 0, así que el
  total no cambia.
- **Alerta de stock:** valida la cantidad ingresada contra el stock **disponible** =
  `stock del menú − unidades de ese producto ya en el carrito`. El bloqueo final en
  `VentaRepository.registrarVenta` se mantiene como red de seguridad.
- **PedidoBuilder en archivo aparte:** clase top-level `PedidoBuilder`; su `build()` usa el
  constructor público vacío de `Pedido` + setters (ya usados en todo el código). Se elimina
  la clase anidada `Pedido.Builder` y el constructor privado `Pedido(Builder)`.
- **Renombre de zonas:** solo cambia el nombre; se mantienen precios (5.00 / 8.50) y tiempos.
- **Sin otros cambios de esquema** de la base de datos.

## Cambios por componente

### 1. Alerta de stock al agregar al carrito

`PanelNuevaVenta.agregarAlCarrito()`: tras parsear la cantidad, calcular el stock disponible
del producto seleccionado = stock mostrado en el menú (columna "Stock") menos las unidades de
ese mismo `CodProducto` ya presentes en `modeloCarrito`. Si `cantidad > disponible`, mostrar
`JOptionPane.WARNING_MESSAGE`: "Stock insuficiente de [nombre]: disponible [N]" y **no** agregar
la fila.

### 2. Total con delivery visible y guardado

`PanelNuevaVenta.generarPedido()`:
- En el formulario de **delivery**, agregar un `JLabel` de solo lectura que muestre el desglose
  y se **actualice en vivo** al cambiar la zona/tarifa (vía `ActionListener` en el combo):
  "Productos: S/ X — Envío: S/ Y — TOTAL: S/ Z", con `Y = tarifa.getPrecioTarifa()`.
- Calcular `montoTotal = totalCarrito + tarifaSeleccionada.getPrecioTarifa()` (en recojo, el
  precio es 0). Pasar `montoTotal` como `total` a `pedidoController.generarPedido(...)` y usarlo
  en el mensaje de éxito.
- El controlador y el repositorio no cambian: guardan el `total` recibido como `montoPedido`.

### 3. PedidoBuilder en archivo aparte

- Crear `src/main/java/com/laptitefrance/delivery/models/PedidoBuilder.java`: métodos fluidos
  (`idCliente`, `montoPedido`, `estado`, `fechaSolicitud`, `direccionEntrega`, `codAsistente`,
  `codRepartidor`, `codTarifa`, `codPago`, `tiempoEntEstimado`, `tiempoEntReal`, `codPedido`),
  cada uno retornando `this`; `build()` crea `new Pedido()` y aplica los setters.
- En `Pedido.java`: eliminar la clase anidada `Builder` y el constructor privado
  `Pedido(Builder)`. Mantener el constructor público vacío y los setters.
- En `PedidoController.ensamblarNuevoPedido(...)`: reemplazar los setters por
  `new PedidoBuilder().idCliente(...).montoPedido(...)....build()`.

### 4. "CANCELADO" en el filtro del monitor

`PanelMonitorPedidos` línea ~53: agregar `"CANCELADO"` al arreglo del `cbxFiltroEstado`
(`{"TODOS", "EN ESPERA", "EN CAMINO", "ENTREGADO", "CANCELADO"}`). El filtrado por estado ya es
genérico (compara el string de estado), así que no requiere más cambios.

### 6. Botón "Vaciar carrito"

`PanelNuevaVenta`: agregar un botón "🗑 Vaciar Carrito" junto a los del carrito que borre
todas las filas de `modeloCarrito`, ponga `totalCarrito = 0` y refresque el total. Permite
re-elegir productos desde cero sin quitar uno por uno. (No toca el cliente seleccionado ni el
menú.)

### 5. Renombre de zonas de delivery

- `SQLQuery1.sql`: cambiar el seed de Tarifa a
  `('T02', 'Lima Metropolitana', 5.00, 20), ('T03', 'Callao', 8.50, 45)` (T01 sin cambios).
- Para la base existente, ejecutar (lo corre el usuario en SQL Server):
  ```sql
  UPDATE Tarifa SET NombreZona = 'Lima Metropolitana' WHERE CodTarifa = 'T02';
  UPDATE Tarifa SET NombreZona = 'Callao'             WHERE CodTarifa = 'T03';
  ```

## Manejo de errores

- Cantidad ingresada no numérica o <= 0 → mensaje existente, sin cambios.
- Cantidad > disponible al agregar → advertencia y no se agrega (nuevo).
- Bloqueo por stock insuficiente al generar → se mantiene (red de seguridad).

## Pruebas / verificación (manual)

1. Stock al agregar: intentar agregar más unidades que el stock (o más que stock−carrito) →
   advertencia inmediata, no se agrega.
2. Total delivery: en checkout de delivery, al cambiar de zona el TOTAL refleja productos +
   envío; el pedido guarda ese total; en recojo el total = productos.
3. Builder: generar un pedido normal → se crea igual que antes (mismos datos en BD).
4. Filtro CANCELADO: en el monitor, filtrar por CANCELADO → lista solo los cancelados.
5. Zonas: tras el UPDATE, el checkout de delivery muestra "Lima Metropolitana" y "Callao".

## Fuera de alcance

- Cambios de precios/tiempos de las tarifas.
- Recalcular montos de pedidos ya existentes.
- Validación de stock en tiempo real entre cajas concurrentes (más allá de la red de seguridad
  transaccional ya existente).
