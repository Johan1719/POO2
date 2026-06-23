# Checkout recojo/delivery + saludo al repartidor — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Que al generar un pedido se elija primero recojo o delivery (la dirección solo se exige en delivery) y que la API del repartidor devuelva y muestre "Bienvenido, [nombre]".

**Architecture:** Sin cambios de esquema. El tipo de entrega se deduce de la `Tarifa` (precio 0 o nombre con "retiro"/"tienda" = recojo). El panel Swing rediseña el diálogo de checkout en dos pasos; el controlador exige dirección solo en delivery. La API suma un lookup del nombre del repartidor (Empleado) y el frontend lo pinta.

**Tech Stack:** Java 21, Swing, Javalin 6, SQL Server (mssql-jdbc), HTML/JS plano.

## Global Constraints

- **No modificar el esquema de la base de datos** (no nuevas tablas/columnas).
- **No se introduce framework de tests.** El proyecto no tiene JUnit ni carpeta de tests, y `mvn` no está en el PATH; se compila y ejecuta desde el IDE. La verificación de cada tarea es **manual**: compilar en el IDE, correr la clase `MainAll`, y observar el resultado (ventana Swing y/o endpoint HTTP).
- Una `Tarifa` es **recojo** si `getPrecioTarifa() == 0.0` **o** `getNombreZona()` contiene (ignore-case) "retiro" o "tienda"; en caso contrario es **delivery**.
- Texto de dirección para recojo: `"RECOJO EN TIENDA: " + tarifa.getNombreZona()`.
- Idioma de la UI y mensajes: español.
- Cada commit incluye solo los archivos de la tarea (no `target/`).

---

### Task 1: Clasificador recojo/delivery en el modelo `Tarifa`

**Files:**
- Modify: `src/main/java/com/laptitefrance/delivery/models/Tarifa.java`

**Interfaces:**
- Produces: `boolean Tarifa.esRecojo()` — `true` si la tarifa es de recojo.

- [ ] **Step 1: Agregar el método `esRecojo()` al modelo**

En `Tarifa.java`, antes del `@Override public String toString()`, agregar:

```java
    /**
     * Una tarifa representa recojo en tienda si no tiene costo de envío
     * (precio 0) o si su nombre de zona menciona "retiro"/"tienda".
     */
    public boolean esRecojo() {
        if (precioTarifa == 0.0) {
            return true;
        }
        String zona = nombreZona == null ? "" : nombreZona.toLowerCase();
        return zona.contains("retiro") || zona.contains("tienda");
    }
```

- [ ] **Step 2: Compilar en el IDE**

Compilar el proyecto (Build / guardar con auto-build). Esperado: sin errores de compilación en `Tarifa.java`.

- [ ] **Step 3: Verificación manual rápida**

Con los datos sembrados: `T01 Retiro en Tienda` (precio 0) → `esRecojo()` debe ser `true`; `T02 Huaral Centro` (5.00) y `T03 Alrededores Huaral` (8.50) → `false`. (Se confirma de hecho en la Task 5 al filtrar los combos.)

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/models/Tarifa.java
git commit -m "feat: add Tarifa.esRecojo() para distinguir recojo de delivery"
```

---

### Task 2: Dirección condicional en `PedidoController`

**Files:**
- Modify: `src/main/java/com/laptitefrance/delivery/controllers/PedidoController.java`

**Interfaces:**
- Consumes: nada nuevo.
- Produces: `PedidoController.generarPedido(Cliente, int, double, String direccionEntrega, String codTarifa, String codPago, boolean esRecojo)` — la dirección solo se exige cuando `esRecojo == false`.

- [ ] **Step 1: Cambiar la firma de `generarPedido` para recibir `esRecojo`**

Reemplazar la firma y la llamada a la validación (líneas ~33-45):

```java
    public void generarPedido(
            Cliente cliente,
            int cantidadProductosEnCarrito,
            double total,
            String direccionEntrega,
            String codTarifa,
            String codPago,
            boolean esRecojo
    ) {
        validarDatosGeneracion(cliente, cantidadProductosEnCarrito, total, direccionEntrega, codTarifa, codPago, esRecojo);

        Pedido pedido = ensamblarNuevoPedido(cliente, total, direccionEntrega, codTarifa, codPago, this.codCajeroActivo);
        pedidoRepository.insert(pedido);
    }
```

- [ ] **Step 2: Hacer condicional la validación de dirección**

En `validarDatosGeneracion` (líneas ~145-171), agregar el parámetro `boolean esRecojo` a la firma y reemplazar el bloque que valida la dirección:

```java
    private static void validarDatosGeneracion(
            Cliente cliente,
            int cantidadProductosEnCarrito,
            double total,
            String direccionEntrega,
            String codTarifa,
            String codPago,
            boolean esRecojo
    ) {
        if (cliente == null) {
            throw new ValidationException("Debe seleccionar un cliente.");
        }
        if (cantidadProductosEnCarrito == 0) {
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

Esperado: `PedidoController.java` compila. El único llamador (`PanelNuevaVenta`) quedará con error de compilación hasta la Task 5 — es esperado y se resuelve ahí.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/controllers/PedidoController.java
git commit -m "feat: PedidoController exige dirección solo en delivery (esRecojo)"
```

---

### Task 3: Diálogo de checkout recojo/delivery en `PanelNuevaVenta`

**Files:**
- Modify: `src/main/java/com/laptitefrance/delivery/views/PanelNuevaVenta.java`

**Interfaces:**
- Consumes: `Tarifa.esRecojo()` (Task 1); `pedidoController.generarPedido(..., boolean esRecojo)` (Task 2).

- [ ] **Step 1: Reescribir el método `generarPedido()` del panel**

Reemplazar todo el cuerpo del método `private void generarPedido()` (líneas ~269-343) por una versión de dos pasos: primero elige modalidad, luego pide los datos correspondientes.

```java
    private void generarPedido() {
        try {
            if (clienteSeleccionado == null) {
                throw new ValidationException("Debe seleccionar un cliente.");
            }
            if (modeloCarrito.getRowCount() == 0) {
                throw new ValidationException("Debe agregar productos al carrito.");
            }
            if (totalCarrito <= 0) {
                throw new ValidationException("El total del pedido debe ser mayor a 0.");
            }

            // Paso 1: elegir modalidad de entrega.
            String[] opciones = {"🏪 Recojo en tienda", "🛵 Delivery a domicilio", "Cancelar"};
            int modalidad = JOptionPane.showOptionDialog(
                    this,
                    "¿Cómo se entregará el pedido?",
                    "Modalidad de entrega",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    opciones,
                    opciones[0]
            );

            if (modalidad != 0 && modalidad != 1) {
                return; // Cancelar o cerrar
            }

            boolean esRecojo = (modalidad == 0);

            // Tarifas disponibles, separadas por modalidad.
            List<Tarifa> todasTarifas = tarifaController.obtenerTarifas();
            List<Tarifa> tarifasFiltradas = new java.util.ArrayList<>();
            for (Tarifa t : todasTarifas) {
                if (t.esRecojo() == esRecojo) {
                    tarifasFiltradas.add(t);
                }
            }
            if (tarifasFiltradas.isEmpty()) {
                throw new ValidationException(esRecojo
                        ? "No hay puntos de recojo configurados."
                        : "No hay zonas de delivery configuradas.");
            }
            JComboBox<Tarifa> cbxTarifa = new JComboBox<>(tarifasFiltradas.toArray(new Tarifa[0]));

            List<Pago> pagos = pagoController.obtenerPagos();
            JComboBox<Pago> cbxPago = new JComboBox<>(pagos.toArray(new Pago[0]));

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

            int opcion = JOptionPane.showConfirmDialog(
                    this,
                    formulario,
                    tituloDialogo,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );
            if (opcion != JOptionPane.OK_OPTION) {
                return;
            }

            Tarifa tarifaSeleccionada = (Tarifa) cbxTarifa.getSelectedItem();
            Pago pagoSeleccionado = (Pago) cbxPago.getSelectedItem();
            if (tarifaSeleccionada == null || pagoSeleccionado == null) {
                throw new ValidationException("Debe seleccionar una tarifa y un método de pago.");
            }

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

            pedidoController.generarPedido(
                    clienteSeleccionado,
                    modeloCarrito.getRowCount(),
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

            limpiarPantalla();

        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Atención", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error inesperado al generar pedido: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
```

- [ ] **Step 2: Compilar en el IDE**

Esperado: compila sin errores (ya existen los imports de `JComboBox`, `JTextField`, `JOptionPane`, `Tarifa`, `Pago`, `List`, `ValidationException`).

- [ ] **Step 3: Verificación manual — recojo**

Correr `MainAll`, loguear como asistente (E001), abrir Nueva Venta, buscar cliente (cel `987654321`), agregar un producto, "Generar Pedido" → elegir **Recojo en tienda**. Esperado: el formulario **no** muestra campo de dirección; solo punto de recojo y pago. Confirmar → "Pedido generado exitosamente". En la BD el pedido queda con `DireccionEntrega = 'RECOJO EN TIENDA: Retiro en Tienda'`.

- [ ] **Step 4: Verificación manual — delivery**

Repetir, pero elegir **Delivery a domicilio**. Esperado: aparece el campo de dirección. Dejarlo vacío y confirmar → mensaje "La dirección de entrega no puede estar vacía". Escribir una dirección y confirmar → "Pedido generado exitosamente"; queda guardada esa dirección.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/views/PanelNuevaVenta.java
git commit -m "feat: checkout en dos pasos (recojo sin dirección / delivery con dirección)"
```

---

### Task 4: Lookup del nombre del repartidor en el repository

**Files:**
- Modify: `src/main/java/com/laptitefrance/delivery/repositories/RepartidorPedidosRepository.java`

**Interfaces:**
- Produces: `String RepartidorPedidosRepository.obtenerNombreRepartidor(String codRepartidor)` — nombre del empleado, o `null` si el código no existe.

- [ ] **Step 1: Agregar el método de lookup**

En `RepartidorPedidosRepository.java`, agregar antes del método privado `getTimestampAsLocalDateTime`:

```java
    /**
     * Devuelve el nombre del repartidor (Empleado) o null si el código no existe.
     * CodRepartidor es FK a Empleado.CodEmpleado.
     */
    public String obtenerNombreRepartidor(String codRepartidor) {
        String sql =
                "SELECT e.Nombre " +
                        "FROM Repartidor r " +
                        "INNER JOIN Empleado e ON e.CodEmpleado = r.CodRepartidor " +
                        "WHERE r.CodRepartidor = ?";

        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codRepartidor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("Nombre");
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener nombre del repartidor: " + e.getMessage(), e);
        }
    }
```

- [ ] **Step 2: Compilar en el IDE**

Esperado: compila (los imports `Connection`, `PreparedStatement`, `ResultSet`, `SQLException` ya existen en el archivo).

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/repositories/RepartidorPedidosRepository.java
git commit -m "feat: obtenerNombreRepartidor() (JOIN Repartidor-Empleado)"
```

---

### Task 5: La API devuelve `nombreRepartidor` y 404 si no existe

**Files:**
- Modify: `src/main/java/com/laptitefrance/delivery/controllers/ApiRepartidor.java`

**Interfaces:**
- Consumes: `obtenerNombreRepartidor(String)` (Task 4).
- Produces: GET `/api/repartidores/{cod}/pedidos` responde `{"codRepartidor", "nombreRepartidor", "pedidos"}`, o `404 {"error":"Repartidor no encontrado"}`.

- [ ] **Step 1: Validar existencia y agregar el nombre a la respuesta**

En el handler `app.get("/api/repartidores/{codRepartidor}/pedidos", ...)`, dentro del `try` (líneas ~61-66), reemplazar:

```java
            try {
                RepartidorPedidosRepository repo = new RepartidorPedidosRepository();
                List<PedidoRepartidorApiRow> pedidos = listarPedidosPorRepartidor(repo, codRepartidor.trim(), "EN CAMINO", 1, 1000);


                ctx.json(Map.of("codRepartidor", codRepartidor, "pedidos", pedidos));
```

por:

```java
            try {
                RepartidorPedidosRepository repo = new RepartidorPedidosRepository();

                String nombreRepartidor = repo.obtenerNombreRepartidor(codRepartidor.trim());
                if (nombreRepartidor == null) {
                    ctx.status(404).json(Map.of("error", "Repartidor no encontrado", "codRepartidor", codRepartidor));
                    return;
                }

                List<PedidoRepartidorApiRow> pedidos = listarPedidosPorRepartidor(repo, codRepartidor.trim(), "EN CAMINO", 1, 1000);

                ctx.json(Map.of(
                        "codRepartidor", codRepartidor,
                        "nombreRepartidor", nombreRepartidor,
                        "pedidos", pedidos));
```

- [ ] **Step 2: Compilar en el IDE**

Esperado: `ApiRepartidor.java` compila.

- [ ] **Step 3: Verificación manual — repartidor válido**

Correr `MainAll`. En el navegador o consola: `http://localhost:8080/api/repartidores/E004/pedidos`. Esperado: JSON con `"nombreRepartidor":"Johan Vasquez"` y el arreglo `pedidos`.

- [ ] **Step 4: Verificación manual — repartidor inexistente**

Abrir `http://localhost:8080/api/repartidores/Z999/pedidos`. Esperado: HTTP 404 con `{"error":"Repartidor no encontrado", ...}`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/controllers/ApiRepartidor.java
git commit -m "feat: API devuelve nombreRepartidor y 404 si el código no existe"
```

---

### Task 6: El frontend muestra "Bienvenido, [nombre]"

**Files:**
- Modify: `src/main/resources/web-repartidor/index.html`

**Interfaces:**
- Consumes: campo `nombreRepartidor` de la respuesta de la API (Task 5).

- [ ] **Step 1: Agregar el contenedor del saludo en el HTML**

Justo después de `<h1>Repartidor</h1>` (línea ~24), agregar:

```html
  <div id="welcome" style="font-size:18px; font-weight:700; margin:6px 0 14px;"></div>
```

- [ ] **Step 2: Agregar referencia y limpieza del saludo en el JS**

En el bloque de constantes (después de `const tbody = ...`, ~línea 60), agregar:

```javascript
    const welcomeEl = $('welcome');
```

- [ ] **Step 3: Mostrar el nombre al cargar pedidos**

En `cargarYMostrar()`, dentro del `try`, después de `const data = await loadPedidos({ codRepartidor });` (~línea 198), agregar:

```javascript
        const nombre = data && data.nombreRepartidor ? data.nombreRepartidor : '';
        welcomeEl.textContent = nombre ? `Bienvenido, ${nombre} 👋` : '';
```

Y en el `catch` del mismo método (~línea 202), después de `tbody.innerHTML = '';`, agregar:

```javascript
        welcomeEl.textContent = '';
```

- [ ] **Step 4: Verificación manual**

Correr `MainAll`, abrir `http://localhost:8080/repartidor`, ingresar `E004`, "Cargar pedidos". Esperado: arriba de la tabla aparece **"Bienvenido, Johan Vasquez 👋"**. Con un código inexistente (`Z999`) el saludo queda vacío y se ve el error 404 en el status.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/web-repartidor/index.html
git commit -m "feat: frontend repartidor muestra saludo con el nombre"
```

---

## Self-Review

- **Cobertura del spec:**
  - Parte A — dirección condicional: Tasks 1 (clasificación), 2 (controller), 3 (panel). ✅
  - Parte A — elegir recojo vs delivery primero: Task 3 (paso 1 del diálogo). ✅
  - Parte A — texto descriptivo de recojo: Task 3 (`"RECOJO EN TIENDA: ..."`). ✅
  - Parte B — nombre del repartidor: Tasks 4 (repo), 5 (API + 404), 6 (frontend). ✅
- **Sin placeholders:** todos los pasos incluyen el código real a pegar. ✅
- **Consistencia de tipos:** `Tarifa.esRecojo()` (boolean) usado en Tasks 3 y referenciado igual; `generarPedido(..., boolean esRecojo)` definido en Task 2 y llamado con 7 args en Task 3; `obtenerNombreRepartidor(String)→String` definido en Task 4 y consumido en Task 5; `nombreRepartidor` (JSON) producido en Task 5 y consumido en Task 6. ✅

## Notas de verificación final (todo el flujo)

1. Recojo: pedido sin dirección → se guarda `"RECOJO EN TIENDA: Retiro en Tienda"`.
2. Delivery: sin dirección → error; con dirección → se guarda y la API la muestra al repartidor.
3. API `E004` → `nombreRepartidor: "Johan Vasquez"`; `Z999` → 404.
4. Frontend `/repartidor` con `E004` → "Bienvenido, Johan Vasquez 👋".
