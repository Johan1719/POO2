# Diseño: Checkout recojo/delivery + saludo al repartidor en la API

Fecha: 2026-06-23
Estado: Aprobado por el usuario (alcance A + B)

## Contexto

Sistema de delivery "La P'tite France" (proyecto POO2). Dos ajustes pedidos:

- **A.** Al generar un pedido, la dirección de entrega se exige siempre. Debe pedirse
  **solo** cuando es delivery. Primero el cajero elige modalidad: recojo en tienda o
  delivery a domicilio.
- **B.** La API del repartidor debe devolver el nombre del repartidor asociado al código
  y el frontend mostrar "Bienvenido, [nombre]".

## Hallazgos del código actual

- `DireccionEntrega VARCHAR(100)` en la tabla `Pedido` **es nullable** ([SQLQuery1.sql:113]).
  La obligatoriedad es lógica de aplicación, no del esquema.
- La obligatoriedad se impone en dos lugares:
  - `PanelNuevaVenta.generarPedido()` (línea ~308).
  - `PedidoController.validarDatosGeneracion()` (línea ~162).
- Las modalidades/zonas ya existen en la tabla `Tarifa`:
  - `T01 Retiro en Tienda` (precio 0.00, 5 min) → recojo.
  - `T02 Huaral Centro` (5.00, 20 min), `T03 Alrededores Huaral` (8.50, 45 min) → delivery.
- El combo de tarifas ya se carga en el checkout (`PanelNuevaVenta` ~273-274).
- `Repartidor.CodRepartidor` es FK a `Empleado.CodEmpleado`; el `Nombre` vive en `Empleado`.
- La API `ApiRepartidor` ya hace JOIN Pedido↔Cliente en `RepartidorPedidosRepository`.

## Decisiones tomadas

- **No se cambia el esquema de la base de datos.**
- Una tarifa cuenta como **recojo** si: `PrecioTarifa == 0.00` **o** su `NombreZona`
  contiene (case-insensitive) "retiro" o "tienda". En caso contrario es **delivery**.
- Se mantiene el único punto de recojo actual (`Retiro en Tienda`). Agregar más puntos
  en el futuro = insertar tarifas con precio 0.00, sin tocar código.

## Parte A: Checkout recojo/delivery

### Flujo del diálogo "Generar Pedido"

1. El cajero elige **modalidad**: `🏪 Recojo en tienda` / `🛵 Delivery a domicilio`.
2. **Recojo**: se muestra un combo solo con tarifas de recojo (precio 0). El campo de
   dirección se oculta y **no** se valida. La dirección guardada en el pedido será un
   texto descriptivo, p. ej. `"RECOJO EN TIENDA: <NombreZona>"`, para que monitor/API
   muestren claramente que es recojo.
3. **Delivery**: se muestra el cajón de dirección (**obligatorio**) y el combo de zonas
   de delivery (precio > 0). Esa dirección es la que ya consume la API del repartidor.
4. El método de pago se mantiene como hoy.

### Cambios

- `PanelNuevaVenta`: rediseñar el formulario de `generarPedido()` para incluir el selector
  de modalidad y mostrar/ocultar la dirección y filtrar el combo de tarifas en consecuencia.
  Construir la "dirección" para recojo con el prefijo descriptivo.
- `PedidoController.generarPedido()` / `validarDatosGeneracion()`: exigir dirección **solo**
  para delivery. Para recojo, aceptar la cadena descriptiva.
- Helper para clasificar tarifa como recojo/delivery (precio 0 o nombre con "retiro"/"tienda").

## Parte B: Saludo al repartidor en la API

### Cambios

- `RepartidorPedidosRepository`: nuevo método `obtenerNombreRepartidor(String cod)` →
  `SELECT e.Nombre FROM Empleado e WHERE e.CodEmpleado = ?` (el código de repartidor es el
  de empleado). Devuelve `null` si no existe.
- `ApiRepartidor` (GET `/api/repartidores/{cod}/pedidos`): si el repartidor no existe →
  `404 {"error":"Repartidor no encontrado"}`. Si existe, agregar `nombreRepartidor` al JSON
  junto a `pedidos`.
- `web-repartidor/index.html`: tras cargar pedidos, mostrar arriba de la tabla
  "Bienvenido, [nombre] 👋" usando `nombreRepartidor` de la respuesta.

## Manejo de errores

- Recojo sin dirección no es error (se autogenera la cadena descriptiva).
- Delivery sin dirección sigue lanzando `ValidationException` (mensaje claro).
- Código de repartidor inexistente en la API → 404 explícito (hoy podría dar lista vacía).

## Pruebas / verificación

- Recojo: generar pedido sin escribir dirección → se crea con `"RECOJO EN TIENDA: ..."`.
- Delivery: sin dirección → error de validación; con dirección → se guarda y la API la
  muestra al repartidor asignado.
- API: `GET /api/repartidores/E004/pedidos` devuelve `nombreRepartidor` (Johan Vasquez);
  código inexistente → 404. Frontend muestra el saludo.

## Fuera de alcance

- Tabla dedicada de puntos de recojo.
- Múltiples sucursales con direcciones reales.
- Autenticación con contraseña (el login sigue siendo por código).
