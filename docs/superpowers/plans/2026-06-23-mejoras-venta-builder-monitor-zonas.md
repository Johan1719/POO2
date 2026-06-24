# Mejoras de venta, PedidoBuilder, filtro CANCELADO y zonas — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Avisar stock al agregar al carrito, mostrar/guardar el total con costo de delivery, extraer el Builder de Pedido a un archivo aparte, sumar el filtro CANCELADO en el monitor y renombrar las zonas de delivery.

**Architecture:** Cambios localizados en la vista de venta (`PanelNuevaVenta`), el modelo (`Pedido` + nuevo `PedidoBuilder`), el controlador (`PedidoController`), el monitor (`PanelMonitorPedidos`) y el seed SQL. Sin cambios de esquema.

**Tech Stack:** Java 21, Swing, SQL Server (mssql-jdbc).

## Global Constraints

- **No se introduce framework de tests.** Verificación **manual**: compilar en el IDE, correr `MainAll`, observar. `mvn` no está en el PATH.
- **No modificar el esquema** de la base de datos (solo datos: renombre de zonas).
- `montoPedido = total de productos + precio de la tarifa` (en recojo la tarifa vale 0).
- Alerta de stock al agregar: validar contra `stock del menú − unidades del mismo producto ya en el carrito`.
- Renombre de zonas: `T02 → 'Lima Metropolitana'`, `T03 → 'Callao'`; mantener precios (5.00 / 8.50) y tiempos.
- Idioma de UI/mensajes: español. Cada commit incluye solo los archivos de su tarea (no `target/`).

---

### Task 1: Extraer `PedidoBuilder` a archivo propio

**Files:**
- Create: `src/main/java/com/laptitefrance/delivery/models/PedidoBuilder.java`
- Modify: `src/main/java/com/laptitefrance/delivery/models/Pedido.java`
- Modify: `src/main/java/com/laptitefrance/delivery/controllers/PedidoController.java`

**Interfaces:**
- Produces: `PedidoBuilder` con métodos fluidos (`codPedido`, `fechaSolicitud`, `montoPedido`, `estado`, `tiempoEntEstimado`, `tiempoEntReal`, `codAsistente`, `codRepartidor`, `idCliente`, `codTarifa`, `codPago`, `direccionEntrega`) y `Pedido build()`.

- [ ] **Step 1: Crear `PedidoBuilder.java`**

```java
package com.laptitefrance.delivery.models;

import java.time.LocalDateTime;

/** Builder (patrón creacional) para construir un Pedido de forma limpia. */
public class PedidoBuilder {
    private String codPedido;
    private LocalDateTime fechaSolicitud;
    private double montoPedido;
    private String estado;
    private LocalDateTime tiempoEntEstimado;
    private LocalDateTime tiempoEntReal;
    private String codAsistente;
    private String codRepartidor;
    private String idCliente;
    private String codTarifa;
    private String codPago;
    private String direccionEntrega;

    public PedidoBuilder codPedido(String codPedido) { this.codPedido = codPedido; return this; }
    public PedidoBuilder fechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; return this; }
    public PedidoBuilder montoPedido(double montoPedido) { this.montoPedido = montoPedido; return this; }
    public PedidoBuilder estado(String estado) { this.estado = estado; return this; }
    public PedidoBuilder tiempoEntEstimado(LocalDateTime tiempoEntEstimado) { this.tiempoEntEstimado = tiempoEntEstimado; return this; }
    public PedidoBuilder tiempoEntReal(LocalDateTime tiempoEntReal) { this.tiempoEntReal = tiempoEntReal; return this; }
    public PedidoBuilder codAsistente(String codAsistente) { this.codAsistente = codAsistente; return this; }
    public PedidoBuilder codRepartidor(String codRepartidor) { this.codRepartidor = codRepartidor; return this; }
    public PedidoBuilder idCliente(String idCliente) { this.idCliente = idCliente; return this; }
    public PedidoBuilder codTarifa(String codTarifa) { this.codTarifa = codTarifa; return this; }
    public PedidoBuilder codPago(String codPago) { this.codPago = codPago; return this; }
    public PedidoBuilder direccionEntrega(String direccionEntrega) { this.direccionEntrega = direccionEntrega; return this; }

    public Pedido build() {
        Pedido p = new Pedido();
        p.setCodPedido(codPedido);
        p.setFechaSolicitud(fechaSolicitud);
        p.setMontoPedido(montoPedido);
        p.setEstado(estado);
        p.setTiempoEntEstimado(tiempoEntEstimado);
        p.setTiempoEntReal(tiempoEntReal);
        p.setCodAsistente(codAsistente);
        p.setCodRepartidor(codRepartidor);
        p.setIdCliente(idCliente);
        p.setCodTarifa(codTarifa);
        p.setCodPago(codPago);
        p.setDireccionEntrega(direccionEntrega);
        return p;
    }
}
```

- [ ] **Step 2: Quitar el Builder anidado y el constructor privado de `Pedido.java`**

En `Pedido.java`, eliminar el constructor privado:

```java
    private Pedido(Builder builder) {
        this.codPedido = builder.codPedido;
        this.fechaSolicitud = builder.fechaSolicitud;
        this.montoPedido = builder.montoPedido;
        this.estado = builder.estado;
        this.tiempoEntEstimado = builder.tiempoEntEstimado;
        this.tiempoEntReal = builder.tiempoEntReal;
        this.codAsistente = builder.codAsistente;
        this.codRepartidor = builder.codRepartidor;
        this.idCliente = builder.idCliente;
        this.codTarifa = builder.codTarifa;
        this.codPago = builder.codPago;
        this.direccionEntrega = builder.direccionEntrega;
    }
```

y eliminar toda la clase anidada `public static class Builder { ... }` (incluido su comentario `/** Builder (patrón creacional) ... */`). Mantener el constructor público vacío `public Pedido() {}` y todos los getters/setters.

- [ ] **Step 3: Usar `PedidoBuilder` en `PedidoController.ensamblarNuevoPedido`**

Agregar el import en `PedidoController.java`:

```java
import com.laptitefrance.delivery.models.PedidoBuilder;
```

Reemplazar el cuerpo de `ensamblarNuevoPedido(...)`:

```java
    private static Pedido ensamblarNuevoPedido(
            Cliente cliente,
            double total,
            String direccionEntrega,
            String codTarifa,
            String codPago,
            String codAsistente
    ) {
        return new PedidoBuilder()
                .codPedido(null)
                .idCliente(cliente.getIdCliente())
                .montoPedido(total)
                .estado("EN ESPERA")
                .fechaSolicitud(LocalDateTime.now())
                .direccionEntrega(direccionEntrega)
                .codAsistente(codAsistente)
                .codTarifa(codTarifa)
                .codPago(codPago)
                .codRepartidor(null)
                .tiempoEntEstimado(null)
                .tiempoEntReal(null)
                .build();
    }
```

- [ ] **Step 4: Compilar en el IDE**

Esperado: compila sin errores. Buscar usos residuales de `Pedido.Builder` en el proyecto: no debe quedar ninguno.

- [ ] **Step 5: Verificación manual**

Correr `MainAll`, generar un pedido normal. Esperado: se crea igual que antes (fila en `Pedido` con los mismos campos). El Builder es un refactor interno, sin cambio de comportamiento.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/models/PedidoBuilder.java src/main/java/com/laptitefrance/delivery/models/Pedido.java src/main/java/com/laptitefrance/delivery/controllers/PedidoController.java
git commit -m "refactor: extraer PedidoBuilder a archivo propio y usarlo en el controlador"
```

---

### Task 2: Alerta de stock al agregar al carrito

**Files:**
- Modify: `src/main/java/com/laptitefrance/delivery/views/PanelNuevaVenta.java`

**Interfaces:**
- Consumes: nada nuevo. Usa `modeloMenu` (col 3 = Stock) y `modeloCarrito` (col 0 = código, col 2 = cantidad).

- [ ] **Step 1: Validar cantidad contra el stock disponible en `agregarAlCarrito`**

Reemplazar el bloque `try { ... }` de `agregarAlCarrito()` (líneas ~236-247) por:

```java
        try {
            int cant = Integer.parseInt(cantStr);
            if (cant <= 0) throw new NumberFormatException();

            // Stock disponible = stock del menú - unidades de este producto ya en el carrito.
            int stockMenu = ((Number) modeloMenu.getValueAt(fila, 3)).intValue();
            int yaEnCarrito = 0;
            for (int i = 0; i < modeloCarrito.getRowCount(); i++) {
                if (cod.equals(modeloCarrito.getValueAt(i, 0))) {
                    yaEnCarrito += ((Number) modeloCarrito.getValueAt(i, 2)).intValue();
                }
            }
            int disponible = stockMenu - yaEnCarrito;
            if (cant > disponible) {
                JOptionPane.showMessageDialog(this,
                        "Stock insuficiente de " + nom + ": disponible " + Math.max(0, disponible) + ".",
                        "Atención", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double sub = pre * cant;
            modeloCarrito.addRow(new Object[]{cod, nom, cant, sub});

            totalCarrito += sub;
            actualizarTotal();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Cantidad inválida. Ingrese un número mayor a 0.", "Error", JOptionPane.ERROR_MESSAGE);
        }
```

- [ ] **Step 2: Compilar en el IDE**

Esperado: compila sin errores.

- [ ] **Step 3: Verificación manual**

Correr `MainAll`, Nueva Venta. Seleccionar un producto con stock conocido (ej. Croissant 50). Intentar agregar 100 → advertencia "Stock insuficiente de Croissant Clásico: disponible 50." y no se agrega. Agregar 30, luego intentar agregar 30 más del mismo → advertencia "disponible 20.".

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/views/PanelNuevaVenta.java
git commit -m "feat: avisar stock insuficiente al agregar al carrito"
```

---

### Task 3: Total con costo de delivery (visible y guardado)

**Files:**
- Modify: `src/main/java/com/laptitefrance/delivery/views/PanelNuevaVenta.java`

**Interfaces:**
- Consumes: `Tarifa.getPrecioTarifa()`; `pedidoController.generarPedido(...)` (sin cambios de firma).

- [ ] **Step 1: Importar `JLabel` (si falta) — ya está importado**

`javax.swing.JLabel` ya está importado en el archivo. No agregar nada.

- [ ] **Step 2: Agregar el label de total en vivo en el formulario de delivery**

En `generarPedido()`, reemplazar el bloque que arma `formulario` según modalidad:

```java
            // Paso 2: datos según modalidad.
            JTextField txtDireccionEntrega = new JTextField(30);
            Object[] formulario;
            String tituloDialogo;
            if (esRecojo) {
                tituloDialogo = "Checkout - Recojo en tienda";
                formulario = new Object[]{
                        "Punto de recojo:", cbxTarifa,
                        "Método de pago:", cbxPago
                };
            } else {
                tituloDialogo = "Checkout - Delivery";
                formulario = new Object[]{
                        "Dirección de entrega:", txtDireccionEntrega,
                        "Zona / Tarifa:", cbxTarifa,
                        "Método de pago:", cbxPago
                };
            }
```

por:

```java
            // Paso 2: datos según modalidad.
            JTextField txtDireccionEntrega = new JTextField(30);
            Object[] formulario;
            String tituloDialogo;
            if (esRecojo) {
                tituloDialogo = "Checkout - Recojo en tienda";
                formulario = new Object[]{
                        "Punto de recojo:", cbxTarifa,
                        "Método de pago:", cbxPago
                };
            } else {
                tituloDialogo = "Checkout - Delivery";

                // Label que muestra Productos / Envío / TOTAL y se actualiza al cambiar la zona.
                JLabel lblTotalCheckout = new JLabel();
                Runnable actualizarTotalCheckout = () -> {
                    Tarifa t = (Tarifa) cbxTarifa.getSelectedItem();
                    double envio = (t == null) ? 0.0 : t.getPrecioTarifa();
                    lblTotalCheckout.setText(String.format(
                            "Productos: S/ %.2f   —   Envío: S/ %.2f   —   TOTAL: S/ %.2f",
                            totalCarrito, envio, totalCarrito + envio));
                };
                actualizarTotalCheckout.run();
                cbxTarifa.addActionListener(e -> actualizarTotalCheckout.run());

                formulario = new Object[]{
                        "Dirección de entrega:", txtDireccionEntrega,
                        "Zona / Tarifa:", cbxTarifa,
                        "Método de pago:", cbxPago,
                        lblTotalCheckout
                };
            }
```

- [ ] **Step 3: Calcular el total con envío y usarlo al generar y mostrar**

En `generarPedido()`, reemplazar la llamada al controlador y el mensaje de éxito (el bloque que arma `items`, llama a `pedidoController.generarPedido(...)` con `totalCarrito` y muestra "¡Pedido generado exitosamente!"):

```java
            // Armar los ítems del carrito (Código, Nombre, Cant) para descontar stock.
            java.util.List<ItemVenta> items = new java.util.ArrayList<>();
            for (int i = 0; i < modeloCarrito.getRowCount(); i++) {
                String codProd = (String) modeloCarrito.getValueAt(i, 0);
                String nomProd = (String) modeloCarrito.getValueAt(i, 1);
                int cant = ((Number) modeloCarrito.getValueAt(i, 2)).intValue();
                items.add(new ItemVenta(codProd, nomProd, cant));
            }

            // Total = productos + costo de envío (en recojo la tarifa vale 0).
            double montoTotal = totalCarrito + tarifaSeleccionada.getPrecioTarifa();

            java.util.List<String> productosEnCero = pedidoController.generarPedido(
                    clienteSeleccionado,
                    items,
                    montoTotal,
                    direccionEntrega,
                    tarifaSeleccionada.getCodTarifa(),
                    pagoSeleccionado.getCodPago(),
                    esRecojo
            );

            JOptionPane.showMessageDialog(
                    this,
                    "¡Pedido generado exitosamente!\nTotal: S/ " + String.format("%.2f", montoTotal),
                    "Transacción Exitosa",
                    JOptionPane.INFORMATION_MESSAGE
            );
```

- [ ] **Step 4: Compilar en el IDE**

Esperado: compila sin errores.

- [ ] **Step 5: Verificación manual**

Correr `MainAll`, Nueva Venta con productos por S/ 40. Generar como **Delivery**, zona con envío S/ 5 → el label muestra "Productos: S/ 40.00 — Envío: S/ 5.00 — TOTAL: S/ 45.00"; al confirmar, el mensaje dice Total S/ 45.00 y en BD `MontoPedido = 45`. Cambiar de zona en el combo actualiza el TOTAL en vivo. En **Recojo**, el total es solo productos.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/views/PanelNuevaVenta.java
git commit -m "feat: mostrar y guardar el total con costo de delivery"
```

---

### Task 4: Filtro "CANCELADO" en el monitor

**Files:**
- Modify: `src/main/java/com/laptitefrance/delivery/views/PanelMonitorPedidos.java`

**Interfaces:**
- Consumes: nada nuevo (el filtrado por estado ya es genérico).

- [ ] **Step 1: Agregar "CANCELADO" al combo de filtro**

En `PanelMonitorPedidos.java` (~línea 53), reemplazar:

```java
        cbxFiltroEstado = new JComboBox<>(new String[]{"TODOS", "EN ESPERA", "EN CAMINO", "ENTREGADO"});
```

por:

```java
        cbxFiltroEstado = new JComboBox<>(new String[]{"TODOS", "EN ESPERA", "EN CAMINO", "ENTREGADO", "CANCELADO"});
```

- [ ] **Step 2: Compilar en el IDE**

Esperado: compila sin errores.

- [ ] **Step 3: Verificación manual**

Correr `MainAll`, Monitor de Pedidos. Cancelar un pedido (Editar Estado → CANCELADO). En el filtro elegir "CANCELADO" → la tabla lista solo los cancelados.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/views/PanelMonitorPedidos.java
git commit -m "feat: agregar filtro CANCELADO en el monitor de pedidos"
```

---

### Task 5: Renombrar zonas de delivery

**Files:**
- Modify: `SQLQuery1.sql`

**Interfaces:**
- Consumes: nada (datos).

- [ ] **Step 1: Actualizar el seed de Tarifa**

En `SQLQuery1.sql` (~línea 138), reemplazar:

```sql
('T01', 'Retiro en Tienda', 0.00, 5), ('T02', 'Huaral Centro', 5.00, 20), ('T03', 'Alrededores Huaral', 8.50, 45);
```

por:

```sql
('T01', 'Retiro en Tienda', 0.00, 5), ('T02', 'Lima Metropolitana', 5.00, 20), ('T03', 'Callao', 8.50, 45);
```

- [ ] **Step 2: Actualizar la base existente (lo ejecuta el usuario)**

El usuario corre en SQL Server contra `LaPtiteFranceDB`:

```sql
UPDATE Tarifa SET NombreZona = 'Lima Metropolitana' WHERE CodTarifa = 'T02';
UPDATE Tarifa SET NombreZona = 'Callao'             WHERE CodTarifa = 'T03';
```

- [ ] **Step 3: Verificación manual**

Tras el UPDATE, correr `MainAll`, Nueva Venta → Delivery: el combo "Zona / Tarifa" muestra "Lima Metropolitana" y "Callao" (ya no "Huaral").

- [ ] **Step 4: Commit**

```bash
git add SQLQuery1.sql
git commit -m "chore: renombrar zonas de delivery a Lima Metropolitana y Callao"
```

---

## Self-Review

- **Cobertura del spec:**
  - Alerta de stock al agregar (considerando carrito): Task 2. ✅
  - Total con delivery visible (label en vivo) y guardado (montoPedido = productos + envío): Task 3. ✅
  - PedidoBuilder en archivo aparte + uso en controlador + quitar Builder anidado: Task 1. ✅
  - Filtro CANCELADO: Task 4. ✅
  - Renombre de zonas (seed + UPDATE): Task 5. ✅
- **Sin placeholders:** todos los pasos incluyen el código exacto. ✅
- **Consistencia de tipos:**
  - `PedidoBuilder` métodos retornan `PedidoBuilder`, `build()` retorna `Pedido`: Task 1, usado en `ensamblarNuevoPedido`. ✅
  - `montoTotal` (double) pasado como `total` a `generarPedido(Cliente, List<ItemVenta>, double, String, String, String, boolean)`: firma existente respetada. ✅
  - Lectura de columnas con `((Number) ...).intValue()` consistente con cómo se cargan (Short/Integer autoboxed). ✅

## Verificación final (todo el flujo)

1. Builder: pedido se crea igual que antes.
2. Stock: advertencia al agregar más que el disponible (mirando el carrito).
3. Delivery: TOTAL en vivo = productos + envío; se guarda ese monto.
4. Monitor: filtro CANCELADO lista los cancelados.
5. Zonas: tras el UPDATE, aparecen Lima Metropolitana y Callao.
