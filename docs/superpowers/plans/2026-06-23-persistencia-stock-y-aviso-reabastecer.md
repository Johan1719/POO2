# Persistencia de stock al vender + aviso de reabastecimiento — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Al generar un pedido, persistir sus ítems y descontar el stock de forma atómica (bloqueando si falta stock y avisando si algún producto queda en 0); y reponer/re-descontar el stock al cancelar/reactivar un pedido.

**Architecture:** Una nueva clase `VentaRepository` es dueña de las transacciones que tocan `Pedido` + `Pedido_Producto` + `Producto`. Un DTO `ItemVenta` transporta los ítems del carrito desde la vista. El `PedidoController` orquesta (consolida ítems, delega, devuelve los productos que quedaron en 0) y el panel muestra el aviso.

**Tech Stack:** Java 21, Swing, SQL Server (mssql-jdbc), JDBC con transacciones manuales (`setAutoCommit(false)`), cláusula `OUTPUT INSERTED.CodPedido` de SQL Server.

## Global Constraints

- **No modificar el esquema de la base de datos.**
- **No se introduce framework de tests.** Verificación **manual**: compilar en el IDE, correr `MainAll`, observar la ventana y consultar la BD. `mvn` no está en el PATH.
- Toda operación que cruce `Pedido`/`Pedido_Producto`/`Producto` va en **una sola transacción** (`setAutoCommit(false)`, `commit`/`rollback`).
- El stock **nunca** queda negativo: si falta, se bloquea con `ValidationException`.
- Mensaje de stock insuficiente, exacto: `"Stock insuficiente de " + nombre + ": hay " + stock + ", pediste " + cantidad`.
- Mensaje de aviso de stock 0, exacto: `"Producto " + nombre + " ya no tiene stock, se sugiere reabastecer."`
- Idioma de UI y mensajes: español.
- Cada commit incluye solo los archivos de la tarea (no `target/`).

---

### Task 1: DTO `ItemVenta`

**Files:**
- Create: `src/main/java/com/laptitefrance/delivery/dtos/ItemVenta.java`

**Interfaces:**
- Produces: clase `ItemVenta` con constructor `ItemVenta(String codProducto, String nombreProducto, int cantidad)` y getters `getCodProducto()`, `getNombreProducto()`, `getCantidad()`.

- [ ] **Step 1: Crear el DTO**

```java
package com.laptitefrance.delivery.dtos;

/** Ítem del carrito que viaja de la vista al repository para registrar una venta. */
public class ItemVenta {
    private final String codProducto;
    private final String nombreProducto;
    private final int cantidad;

    public ItemVenta(String codProducto, String nombreProducto, int cantidad) {
        this.codProducto = codProducto;
        this.nombreProducto = nombreProducto;
        this.cantidad = cantidad;
    }

    public String getCodProducto() {
        return codProducto;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public int getCantidad() {
        return cantidad;
    }
}
```

- [ ] **Step 2: Compilar en el IDE**

Esperado: compila sin errores.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/dtos/ItemVenta.java
git commit -m "feat: DTO ItemVenta para transportar items del carrito"
```

---

### Task 2: `VentaRepository.registrarVenta` (venta atómica)

**Files:**
- Create: `src/main/java/com/laptitefrance/delivery/repositories/VentaRepository.java`

**Interfaces:**
- Consumes: `ItemVenta` (Task 1); `com.laptitefrance.delivery.models.Pedido`.
- Produces: `List<String> VentaRepository.registrarVenta(Pedido pedido, List<ItemVenta> items)` — inserta el pedido y sus ítems, descuenta stock en una transacción y devuelve los nombres de los productos que quedaron en stock 0. Lanza `ValidationException` si falta stock.

- [ ] **Step 1: Crear la clase con `registrarVenta`**

```java
package com.laptitefrance.delivery.repositories;

import com.laptitefrance.delivery.config.DBConnection;
import com.laptitefrance.delivery.dtos.ItemVenta;
import com.laptitefrance.delivery.exceptions.ValidationException;
import com.laptitefrance.delivery.models.Pedido;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Operaciones de venta que tocan varias tablas (Pedido, Pedido_Producto, Producto)
 * dentro de una sola transacción.
 */
public class VentaRepository {

    private static final String INSERT_PEDIDO =
            "INSERT INTO Pedido (FechaSolicitud, MontoPedido, Estado, TiempoEntEstimado, TiempoEntReal, HoraEnvio, DireccionEntrega, CodAsistente, CodRepartidor, IDCliente, CodTarifa, CodPago) " +
            "OUTPUT INSERTED.CodPedido " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_PRODUCTO = "SELECT NombreProd, Stock FROM Producto WHERE CodProducto = ?";
    private static final String INSERT_PP = "INSERT INTO Pedido_Producto (CodProducto, CodPedido, CantProd) VALUES (?, ?, ?)";
    private static final String UPDATE_STOCK = "UPDATE Producto SET Stock = ? WHERE CodProducto = ?";

    /**
     * Inserta el pedido y sus ítems, descontando stock, todo en una transacción.
     * @return nombres de productos cuyo stock quedó en 0.
     * @throws ValidationException si algún ítem no tiene stock suficiente.
     */
    public List<String> registrarVenta(Pedido pedido, List<ItemVenta> items) {
        List<String> productosEnCero = new ArrayList<>();

        try (Connection con = DBConnection.getConexion()) {
            con.setAutoCommit(false);
            try {
                // 1) Insertar Pedido y recuperar el CodPedido autogenerado.
                String codPedido;
                try (PreparedStatement ps = con.prepareStatement(INSERT_PEDIDO)) {
                    setTimestampOrNull(ps, 1, pedido.getFechaSolicitud());
                    ps.setDouble(2, pedido.getMontoPedido());
                    ps.setString(3, pedido.getEstado());
                    setTimestampOrNull(ps, 4, pedido.getTiempoEntEstimado());
                    setTimestampOrNull(ps, 5, pedido.getTiempoEntReal());
                    setTimestampOrNull(ps, 6, pedido.getHoraEnvio());
                    ps.setString(7, pedido.getDireccionEntrega());
                    ps.setString(8, pedido.getCodAsistente());
                    ps.setString(9, pedido.getCodRepartidor());
                    ps.setString(10, pedido.getIdCliente());
                    ps.setString(11, pedido.getCodTarifa());
                    ps.setString(12, pedido.getCodPago());

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("No se pudo obtener el CodPedido generado.");
                        }
                        codPedido = rs.getString(1);
                    }
                }

                // 2) Por cada ítem: validar stock, insertar Pedido_Producto, descontar.
                for (ItemVenta item : items) {
                    String nombre;
                    int stockActual;
                    try (PreparedStatement ps = con.prepareStatement(SELECT_PRODUCTO)) {
                        ps.setString(1, item.getCodProducto());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                throw new ValidationException("No existe el producto con código: " + item.getCodProducto());
                            }
                            nombre = rs.getString("NombreProd");
                            stockActual = rs.getShort("Stock");
                        }
                    }

                    if (stockActual < item.getCantidad()) {
                        throw new ValidationException(
                                "Stock insuficiente de " + nombre + ": hay " + stockActual + ", pediste " + item.getCantidad());
                    }

                    try (PreparedStatement ps = con.prepareStatement(INSERT_PP)) {
                        ps.setString(1, item.getCodProducto());
                        ps.setString(2, codPedido);
                        ps.setShort(3, (short) item.getCantidad());
                        ps.executeUpdate();
                    }

                    int nuevoStock = stockActual - item.getCantidad();
                    try (PreparedStatement ps = con.prepareStatement(UPDATE_STOCK)) {
                        ps.setShort(1, (short) nuevoStock);
                        ps.setString(2, item.getCodProducto());
                        ps.executeUpdate();
                    }

                    if (nuevoStock == 0) {
                        productosEnCero.add(nombre);
                    }
                }

                con.commit();
                return productosEnCero;
            } catch (RuntimeException | SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar la venta: " + e.getMessage(), e);
        }
    }

    private static void setTimestampOrNull(PreparedStatement ps, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            ps.setTimestamp(index, null);
        } else {
            ps.setTimestamp(index, Timestamp.valueOf(value));
        }
    }
}
```

- [ ] **Step 2: Compilar en el IDE**

Esperado: compila sin errores.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/repositories/VentaRepository.java
git commit -m "feat: VentaRepository.registrarVenta (venta atomica con descuento de stock)"
```

---

### Task 3: Reposición y re-descuento de stock (cancelar/reactivar)

**Files:**
- Modify: `src/main/java/com/laptitefrance/delivery/repositories/VentaRepository.java`

**Interfaces:**
- Produces:
  - `void VentaRepository.reponerStockPorCancelacion(String codPedido, String nuevoEstado)` — devuelve el stock de los ítems del pedido y fija su estado.
  - `void VentaRepository.descontarStockPorReactivacion(String codPedido, String nuevoEstado)` — valida y descuenta el stock de los ítems del pedido y fija su estado. Lanza `ValidationException` si falta stock.

- [ ] **Step 1: Agregar las constantes SQL**

En `VentaRepository`, junto a las otras constantes, agregar:

```java
    private static final String SELECT_ITEMS_PEDIDO = "SELECT CodProducto, CantProd FROM Pedido_Producto WHERE CodPedido = ?";
    private static final String UPDATE_ESTADO = "UPDATE Pedido SET Estado = ? WHERE CodPedido = ?";
    private static final String SELECT_PRODUCTO_POR_COD = "SELECT NombreProd, Stock FROM Producto WHERE CodProducto = ?";
```

- [ ] **Step 2: Agregar `reponerStockPorCancelacion`**

Antes del método privado `setTimestampOrNull`, agregar:

```java
    /** Devuelve el stock de los ítems del pedido y fija su estado (cancelación). */
    public void reponerStockPorCancelacion(String codPedido, String nuevoEstado) {
        try (Connection con = DBConnection.getConexion()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement psItems = con.prepareStatement(SELECT_ITEMS_PEDIDO)) {
                    psItems.setString(1, codPedido);
                    try (ResultSet rs = psItems.executeQuery()) {
                        while (rs.next()) {
                            String codProducto = rs.getString("CodProducto");
                            int cant = rs.getShort("CantProd");
                            int stockActual;
                            try (PreparedStatement psStock = con.prepareStatement(SELECT_PRODUCTO_POR_COD)) {
                                psStock.setString(1, codProducto);
                                try (ResultSet rsStock = psStock.executeQuery()) {
                                    stockActual = rsStock.next() ? rsStock.getShort("Stock") : 0;
                                }
                            }
                            try (PreparedStatement psUpd = con.prepareStatement(UPDATE_STOCK)) {
                                psUpd.setShort(1, (short) (stockActual + cant));
                                psUpd.setString(2, codProducto);
                                psUpd.executeUpdate();
                            }
                        }
                    }
                }

                try (PreparedStatement psEstado = con.prepareStatement(UPDATE_ESTADO)) {
                    psEstado.setString(1, nuevoEstado);
                    psEstado.setString(2, codPedido);
                    psEstado.executeUpdate();
                }

                con.commit();
            } catch (RuntimeException | SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al reponer stock por cancelación: " + e.getMessage(), e);
        }
    }
```

- [ ] **Step 3: Agregar `descontarStockPorReactivacion`**

Debajo del método anterior, agregar:

```java
    /** Valida y descuenta el stock de los ítems del pedido y fija su estado (reactivación). */
    public void descontarStockPorReactivacion(String codPedido, String nuevoEstado) {
        try (Connection con = DBConnection.getConexion()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement psItems = con.prepareStatement(SELECT_ITEMS_PEDIDO)) {
                    psItems.setString(1, codPedido);
                    try (ResultSet rs = psItems.executeQuery()) {
                        while (rs.next()) {
                            String codProducto = rs.getString("CodProducto");
                            int cant = rs.getShort("CantProd");
                            String nombre;
                            int stockActual;
                            try (PreparedStatement psStock = con.prepareStatement(SELECT_PRODUCTO_POR_COD)) {
                                psStock.setString(1, codProducto);
                                try (ResultSet rsStock = psStock.executeQuery()) {
                                    if (!rsStock.next()) {
                                        throw new ValidationException("No existe el producto con código: " + codProducto);
                                    }
                                    nombre = rsStock.getString("NombreProd");
                                    stockActual = rsStock.getShort("Stock");
                                }
                            }
                            if (stockActual < cant) {
                                throw new ValidationException(
                                        "Stock insuficiente de " + nombre + ": hay " + stockActual + ", pediste " + cant);
                            }
                            try (PreparedStatement psUpd = con.prepareStatement(UPDATE_STOCK)) {
                                psUpd.setShort(1, (short) (stockActual - cant));
                                psUpd.setString(2, codProducto);
                                psUpd.executeUpdate();
                            }
                        }
                    }
                }

                try (PreparedStatement psEstado = con.prepareStatement(UPDATE_ESTADO)) {
                    psEstado.setString(1, nuevoEstado);
                    psEstado.setString(2, codPedido);
                    psEstado.executeUpdate();
                }

                con.commit();
            } catch (RuntimeException | SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al descontar stock por reactivación: " + e.getMessage(), e);
        }
    }
```

- [ ] **Step 4: Compilar en el IDE**

Esperado: compila sin errores. (`ValidationException` ya está importado de la Task 2.)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/repositories/VentaRepository.java
git commit -m "feat: reponer/descontar stock al cancelar/reactivar pedido"
```

---

### Task 4: `PedidoController.generarPedido` consolida ítems y delega

**Files:**
- Modify: `src/main/java/com/laptitefrance/delivery/controllers/PedidoController.java`

**Interfaces:**
- Consumes: `ItemVenta` (Task 1); `VentaRepository.registrarVenta` (Task 2).
- Produces: `List<String> PedidoController.generarPedido(Cliente cliente, List<ItemVenta> items, double total, String direccionEntrega, String codTarifa, String codPago, boolean esRecojo)` — devuelve los nombres de productos que quedaron en 0.

- [ ] **Step 1: Agregar imports**

En `PedidoController.java`, junto a los imports existentes, agregar:

```java
import java.util.LinkedHashMap;
import java.util.Map;

import com.laptitefrance.delivery.dtos.ItemVenta;
import com.laptitefrance.delivery.repositories.VentaRepository;
```

- [ ] **Step 2: Reemplazar `generarPedido` y su validación**

Reemplazar el método `generarPedido(...)` (el actual recibe `int cantidadProductosEnCarrito`) y el método `validarDatosGeneracion(...)` por:

```java
    public List<String> generarPedido(
            Cliente cliente,
            List<ItemVenta> items,
            double total,
            String direccionEntrega,
            String codTarifa,
            String codPago,
            boolean esRecojo
    ) {
        validarDatosGeneracion(cliente, items, total, direccionEntrega, codTarifa, codPago, esRecojo);

        // Consolidar ítems por producto (evita violar la PK de Pedido_Producto y suma cantidades).
        Map<String, ItemVenta> consolidados = new LinkedHashMap<>();
        for (ItemVenta it : items) {
            ItemVenta previo = consolidados.get(it.getCodProducto());
            if (previo == null) {
                consolidados.put(it.getCodProducto(), it);
            } else {
                consolidados.put(it.getCodProducto(),
                        new ItemVenta(it.getCodProducto(), it.getNombreProducto(),
                                previo.getCantidad() + it.getCantidad()));
            }
        }

        Pedido pedido = ensamblarNuevoPedido(cliente, total, direccionEntrega, codTarifa, codPago, this.codCajeroActivo);
        return new VentaRepository().registrarVenta(pedido, new java.util.ArrayList<>(consolidados.values()));
    }
```

Y la validación:

```java
    private static void validarDatosGeneracion(
            Cliente cliente,
            List<ItemVenta> items,
            double total,
            String direccionEntrega,
            String codTarifa,
            String codPago,
            boolean esRecojo
    ) {
        if (cliente == null) {
            throw new ValidationException("Debe seleccionar un cliente.");
        }
        if (items == null || items.isEmpty()) {
            throw new ValidationException("Debe agregar productos al carrito.");
        }
        if (total <= 0) {
            throw new ValidationException("El total del pedido debe ser mayor a 0.");
        }
        if (!esRecojo && (direccionEntrega == null || direccionEntrega.trim().isEmpty())) {
            throw new ValidationException("La dirección de entrega no puede estar vacía.");
        }
        if (codTarifa == null || codTarifa.trim().isEmpty()) {
            throw new ValidationException("Debe seleccionar una tarifa.");
        }
        if (codPago == null || codPago.trim().isEmpty()) {
            throw new ValidationException("Debe seleccionar un método de pago.");
        }
    }
```

- [ ] **Step 3: Compilar en el IDE**

Esperado: `PedidoController.java` compila. `PanelNuevaVenta` quedará con error (aún llama con la firma vieja) — se resuelve en la Task 6.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/controllers/PedidoController.java
git commit -m "feat: generarPedido consolida items y delega en VentaRepository"
```

---

### Task 5: `PedidoController.actualizarEstadoPedido` rutea cancelación/reactivación

**Files:**
- Modify: `src/main/java/com/laptitefrance/delivery/controllers/PedidoController.java`

**Interfaces:**
- Consumes: `VentaRepository.reponerStockPorCancelacion`, `VentaRepository.descontarStockPorReactivacion` (Task 3).
- Produces: `void PedidoController.actualizarEstadoPedido(String codPedido, String nuevoEstado)` (misma firma; nuevo comportamiento).

- [ ] **Step 1: Reemplazar el cuerpo de `actualizarEstadoPedido`**

Reemplazar el método actual por:

```java
    public void actualizarEstadoPedido(String codPedido, String nuevoEstado) {
        if (codPedido == null || codPedido.trim().isEmpty()) {
            throw new ValidationException("Debe seleccionar un pedido válido.");
        }
        if (nuevoEstado == null || nuevoEstado.trim().isEmpty()) {
            throw new ValidationException("Debe proporcionar un estado válido.");
        }

        String cod = codPedido.trim();
        String nuevo = nuevoEstado.trim();

        Pedido pedido = pedidoRepository.findById(cod)
                .orElseThrow(() -> new ValidationException("No existe Pedido con codPedido=" + cod));

        String estadoAnterior = pedido.getEstado() == null ? "" : pedido.getEstado().trim();
        boolean eraCancelado = estadoAnterior.equalsIgnoreCase("CANCELADO");
        boolean seraCancelado = nuevo.equalsIgnoreCase("CANCELADO");

        VentaRepository ventaRepository = new VentaRepository();

        if (!eraCancelado && seraCancelado) {
            // Activo -> CANCELADO: devolver stock.
            ventaRepository.reponerStockPorCancelacion(cod, nuevo);
        } else if (eraCancelado && !seraCancelado) {
            // CANCELADO -> activo: volver a descontar stock (valida y bloquea si falta).
            ventaRepository.descontarStockPorReactivacion(cod, nuevo);
        } else {
            // Transición que no cruza el límite de CANCELADO: solo actualizar estado.
            pedido.setEstado(nuevo);
            pedidoRepository.update(pedido);
        }
    }
```

- [ ] **Step 2: Compilar en el IDE**

Esperado: compila sin errores.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/controllers/PedidoController.java
git commit -m "feat: actualizarEstadoPedido repone/descuenta stock al cancelar/reactivar"
```

---

### Task 6: `PanelNuevaVenta` arma los ítems y muestra el aviso de stock 0

**Files:**
- Modify: `src/main/java/com/laptitefrance/delivery/views/PanelNuevaVenta.java`

**Interfaces:**
- Consumes: `ItemVenta` (Task 1); `PedidoController.generarPedido(...)` que ahora devuelve `List<String>` (Task 4).

- [ ] **Step 1: Agregar el import de `ItemVenta`**

En `PanelNuevaVenta.java`, junto a los otros imports `com.laptitefrance.delivery...`, agregar:

```java
import com.laptitefrance.delivery.dtos.ItemVenta;
```

- [ ] **Step 2: Reemplazar el bloque de armado y llamada dentro de `generarPedido()`**

En el método `generarPedido()`, reemplazar el bloque que arma la dirección y llama al controlador (desde `// Dirección: ...` hasta el `JOptionPane.showMessageDialog(... "Transacción Exitosa" ...)` inclusive y `limpiarPantalla();`) por:

```java
            // Dirección: en recojo se genera un texto descriptivo; en delivery la escribe el cajero.
            String direccionEntrega;
            if (esRecojo) {
                direccionEntrega = "RECOJO EN TIENDA: " + tarifaSeleccionada.getNombreZona();
            } else {
                direccionEntrega = txtDireccionEntrega.getText() == null ? "" : txtDireccionEntrega.getText().trim();
                if (direccionEntrega.isEmpty()) {
                    throw new ValidationException("La dirección de entrega no puede estar vacía.");
                }
            }

            // Armar los ítems del carrito (Código, Nombre, Cant) para descontar stock.
            java.util.List<ItemVenta> items = new java.util.ArrayList<>();
            for (int i = 0; i < modeloCarrito.getRowCount(); i++) {
                String codProd = (String) modeloCarrito.getValueAt(i, 0);
                String nomProd = (String) modeloCarrito.getValueAt(i, 1);
                int cant = ((Number) modeloCarrito.getValueAt(i, 2)).intValue();
                items.add(new ItemVenta(codProd, nomProd, cant));
            }

            java.util.List<String> productosEnCero = pedidoController.generarPedido(
                    clienteSeleccionado,
                    items,
                    totalCarrito,
                    direccionEntrega,
                    tarifaSeleccionada.getCodTarifa(),
                    pagoSeleccionado.getCodPago(),
                    esRecojo
            );

            JOptionPane.showMessageDialog(
                    this,
                    "¡Pedido generado exitosamente!\nTotal: S/ " + String.format("%.2f", totalCarrito),
                    "Transacción Exitosa",
                    JOptionPane.INFORMATION_MESSAGE
            );

            if (productosEnCero != null && !productosEnCero.isEmpty()) {
                StringBuilder aviso = new StringBuilder();
                for (String nombre : productosEnCero) {
                    aviso.append("⚠️ Producto ").append(nombre).append(" ya no tiene stock, se sugiere reabastecer.\n");
                }
                JOptionPane.showMessageDialog(this, aviso.toString().trim(), "Reabastecer", JOptionPane.WARNING_MESSAGE);
            }

            limpiarPantalla();
```

- [ ] **Step 3: Compilar en el IDE**

Esperado: compila sin errores; ya no quedan errores pendientes en el proyecto.

- [ ] **Step 4: Verificación manual — venta y descuento**

Correr `MainAll`, login `E001`, Nueva Venta, buscar cliente (`987654321`), agregar 2 unidades de "Croissant Clásico" (stock inicial 50), generar pedido (recojo o delivery). Esperado: "Pedido generado exitosamente". En BD: `Pedido_Producto` tiene la fila con `CantProd=2`; `Producto.Stock` de Croissant pasó de 50 a 48.

- [ ] **Step 5: Verificación manual — bloqueo por stock insuficiente**

Agregar al carrito una cantidad mayor al stock (ej. 999 de un producto). Generar. Esperado: mensaje "Stock insuficiente de [nombre]: hay X, pediste 999"; NO se crea el pedido; el stock no cambia.

- [ ] **Step 6: Verificación manual — aviso de stock 0**

Para un producto con poco stock, vender exactamente todo lo que queda. Esperado: tras "Pedido generado", aparece la ventana "⚠️ Producto [nombre] ya no tiene stock, se sugiere reabastecer."; el producto desaparece del menú al refrescar.

- [ ] **Step 7: Verificación manual — cancelación y reactivación**

En el monitor (`PanelMonitorPedidos`), tomar el pedido recién creado y cambiar su estado a `CANCELADO`. Esperado: el stock de sus productos vuelve a subir por la cantidad vendida. Luego reactivarlo (a `EN ESPERA`). Esperado: el stock se vuelve a descontar; si no alcanzara, se bloquea con el mensaje de stock insuficiente.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/views/PanelNuevaVenta.java
git commit -m "feat: PanelNuevaVenta arma items y avisa productos en stock 0"
```

---

## Self-Review

- **Cobertura del spec:**
  - Persistir ítems en `Pedido_Producto`: Task 2 (insert PP). ✅
  - Descontar stock al generar: Task 2 + Task 4. ✅
  - Bloqueo por stock insuficiente (no negativo): Task 2 (validación) + verif. Task 6 Step 5. ✅
  - Aviso de stock 0 en ventana propia: Task 6 Step 2. ✅
  - Transacción atómica: Tasks 2 y 3 (`setAutoCommit(false)`/commit/rollback). ✅
  - Reposición al cancelar: Task 3 (`reponerStockPorCancelacion`) + Task 5 (ruteo). ✅
  - Re-descuento al reactivar: Task 3 (`descontarStockPorReactivacion`) + Task 5. ✅
  - Sin cambios de esquema: ninguna tarea altera el esquema. ✅
- **Sin placeholders:** todos los pasos de código incluyen el código real. ✅
- **Consistencia de tipos:**
  - `ItemVenta(String, String, int)` + getters: definido en Task 1, usado en Tasks 4 y 6. ✅
  - `registrarVenta(Pedido, List<ItemVenta>) → List<String>`: Task 2, consumido en Task 4. ✅
  - `reponerStockPorCancelacion(String, String)` / `descontarStockPorReactivacion(String, String)`: Task 3, consumidos en Task 5. ✅
  - `generarPedido(...) → List<String>`: Task 4, consumido en Task 6. ✅
  - Constantes `UPDATE_STOCK`, `SELECT_PRODUCTO_POR_COD`, `SELECT_ITEMS_PEDIDO`, `UPDATE_ESTADO`: definidas en Tasks 2/3 y usadas consistentemente. ✅

## Verificación final (todo el flujo)

1. Venta normal → `Pedido_Producto` poblada, stock descontado.
2. Stock insuficiente → bloqueo con mensaje, sin cambios.
3. Stock a 0 → aviso de reabastecimiento.
4. Cancelar → stock repuesto. Reactivar → stock re-descontado (o bloqueo si falta).
