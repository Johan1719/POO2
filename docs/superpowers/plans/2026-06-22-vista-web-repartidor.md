# Vista web del repartidor — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dar a la API una vista web donde el repartidor busca sus pedidos por su código, los ve en una tabla paginada (pedido, nombre, dirección, método de pago, estado) y confirma la entrega con un botón que persiste en la BD; el cambio se refleja solo en el Monitor del asistente.

**Architecture:** Un único ejecutable (`Main`) levanta, en el mismo proceso, el servidor web (Javalin) y el dashboard Swing. Una instancia compartida de `CentroDespacho` (thread-safe) coordina la confirmación de entrega bajo un lock por pedido. La lectura de la tabla y la confirmación se exponen como endpoints REST que sirve una página HTML/JS. El Monitor del asistente se auto-refresca con un `javax.swing.Timer`.

**Tech Stack:** Java 21, Maven (wrapper `./mvnw`), Javalin 6.1.3 (+ static files), Jackson, JDBC (mssql-jdbc, SQL Server `OFFSET/FETCH`), Swing, JUnit 5.

## Global Constraints

- Java 21 (`maven.compiler.source/target=21`), encoding UTF-8.
- Paquete de la API concurrente: `com.laptitefrance.delivery.despacho`.
- Estados de pedido (verbatim): `EN ESPERA`, `EN CAMINO`, `ENTREGADO`.
- Build: usar `./mvnw` (NUNCA `clean` — la extensión de Java de VSCode bloquea `target/`).
- Maven imprime WARNING inocuos (jansi/Unsafe/native-access) en Java 25 — ignorar; juzgar por `BUILD SUCCESS` y `Tests run`.
- Persistencia vía `IRepositorioBase<Pedido,String>` (`findById` → `Optional<Pedido>`, `update`).
- Consultas SQL siempre con `PreparedStatement` y paginación `OFFSET ? ROWS FETCH NEXT ? ROWS ONLY`.
- Servidor web en el puerto 8080, CORS abierto (ejercicio académico local).
- Identificadores y mensajes en español con acentos correctos.
- DTOs de proyección con campos públicos (estilo del proyecto, ver `PedidoMonitorRow`).
- El repartidor solo avanza `EN CAMINO → ENTREGADO`.

---

## File Structure

| Archivo | Responsabilidad |
|---|---|
| `src/main/java/.../dtos/PedidoRepartidorRow.java` (crear) | DTO de fila: codPedido, nombreCliente, direccionEntrega, metodoPago, estado |
| `src/main/java/.../despacho/PaginaRepartidor.java` (crear) | DTO de página: filas + page + totalPaginas + totalItems |
| `src/main/java/.../repositories/PedidoRepartidorRepositoryPagination.java` (crear) | Consulta JOIN Pedido+Cliente+Pago por repartidor, paginada + conteo |
| `src/main/java/.../despacho/CentroDespacho.java` (modificar) | Añadir `pedidosDeRepartidor(...)` y `confirmarEntrega(...)` (bajo lock por pedido) |
| `src/test/java/.../despacho/CentroDespachoTest.java` (modificar) | Tests de `confirmarEntrega` (éxito, pertenencia, estado, concurrencia) |
| `src/main/java/.../despacho/ServidorWebRepartidor.java` (crear) | Servidor Javalin: sirve la web + endpoints (reemplaza `ApiDeliveryServer`) |
| `src/main/java/.../despacho/ApiDeliveryServer.java` (eliminar) | Reemplazado por `ServidorWebRepartidor` |
| `src/main/resources/web/index.html` (crear) | Página del repartidor (buscador + tabla + paginación) |
| `src/main/resources/web/app.js` (crear) | Lógica de la página (fetch, render, confirmar entrega) |
| `src/main/resources/web/styles.css` (crear) | Estilos de la página |
| `src/main/java/.../Main.java` (modificar) | Ejecutable único: arranca servidor web + login |
| `src/main/java/.../views/PanelMonitorPedidos.java` (modificar) | Auto-refresco con `javax.swing.Timer` |

`<...>` = `com/laptitefrance/delivery`.

**Verificación con BD:** las consultas (`pedidosDeRepartidor`) y los endpoints requieren SQL Server poblado (`SQLQuery1.sql`) con un pedido asignado a un repartidor (estado `EN CAMINO`, `CodRepartidor` no nulo). Esa verificación es **manual**; los tests JUnit cubren la lógica concurrente de `confirmarEntrega` con `FakePedidoRepository`.

---

## Task 1: DTOs y repositorio de consulta paginada por repartidor

**Files:**
- Create: `src/main/java/com/laptitefrance/delivery/dtos/PedidoRepartidorRow.java`
- Create: `src/main/java/com/laptitefrance/delivery/despacho/PaginaRepartidor.java`
- Create: `src/main/java/com/laptitefrance/delivery/repositories/PedidoRepartidorRepositoryPagination.java`

**Interfaces:**
- Produces:
  - `PedidoRepartidorRow` con campos públicos `String codPedido, nombreCliente, direccionEntrega, metodoPago, estado`.
  - `PaginaRepartidor(List<PedidoRepartidorRow> filas, int page, int totalPaginas, long totalItems)` con esos campos públicos `final`.
  - `PedidoRepartidorRepositoryPagination.listar(String codRepartidor, int page, int pageSize) -> List<PedidoRepartidorRow>` (estático).
  - `PedidoRepartidorRepositoryPagination.contar(String codRepartidor) -> int` (estático).

> Verificación de esta tarea: **compila** (`./mvnw test-compile` → BUILD SUCCESS). Las consultas se validan manualmente con BD en la Task 3.

- [ ] **Step 1: Crear el DTO de fila**

Crear `src/main/java/com/laptitefrance/delivery/dtos/PedidoRepartidorRow.java`:

```java
package com.laptitefrance.delivery.dtos;

/** Fila de la tabla web del repartidor: lo mínimo que se muestra y se acciona. */
public class PedidoRepartidorRow {
    public String codPedido;
    public String nombreCliente;
    public String direccionEntrega;
    public String metodoPago;
    public String estado;
}
```

- [ ] **Step 2: Crear el DTO de página**

Crear `src/main/java/com/laptitefrance/delivery/despacho/PaginaRepartidor.java`:

```java
package com.laptitefrance.delivery.despacho;

import com.laptitefrance.delivery.dtos.PedidoRepartidorRow;

import java.util.List;

/** Página de pedidos de un repartidor, con metadatos para "Página X de N". */
public class PaginaRepartidor {
    public final List<PedidoRepartidorRow> filas;
    public final int page;
    public final int totalPaginas;
    public final long totalItems;

    public PaginaRepartidor(List<PedidoRepartidorRow> filas, int page, int totalPaginas, long totalItems) {
        this.filas = filas;
        this.page = page;
        this.totalPaginas = totalPaginas;
        this.totalItems = totalItems;
    }
}
```

- [ ] **Step 3: Crear el repositorio de consulta paginada**

Crear `src/main/java/com/laptitefrance/delivery/repositories/PedidoRepartidorRepositoryPagination.java`:

```java
package com.laptitefrance.delivery.repositories;

import com.laptitefrance.delivery.config.DBConnection;
import com.laptitefrance.delivery.dtos.PedidoRepartidorRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Consulta paginada de los pedidos asignados a un repartidor, con el nombre del
 * cliente (JOIN Cliente) y el método de pago (JOIN Pago). Sigue el patrón de
 * PedidoMonitorRepositoryPagination (estático, OFFSET/FETCH de SQL Server).
 */
public final class PedidoRepartidorRepositoryPagination {

    private PedidoRepartidorRepositoryPagination() {
    }

    public static List<PedidoRepartidorRow> listar(String codRepartidor, int page, int pageSize) {
        int p = Math.max(1, page);
        int ps = Math.max(1, pageSize);
        int offset = (p - 1) * ps;

        String sql =
                "SELECT p.CodPedido, c.NombreCliente, p.DireccionEntrega, pg.MetodoPago, p.Estado " +
                "FROM Pedido p " +
                "INNER JOIN Cliente c ON c.IDCliente = p.IDCliente " +
                "LEFT JOIN Pago pg ON pg.CodPago = p.CodPago " +
                "WHERE p.CodRepartidor = ? " +
                "ORDER BY p.Fechasolicitud DESC " +
                "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        List<PedidoRepartidorRow> result = new ArrayList<>();

        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps2 = con.prepareStatement(sql)) {

            ps2.setString(1, codRepartidor);
            ps2.setInt(2, offset);
            ps2.setInt(3, ps);

            try (ResultSet rs = ps2.executeQuery()) {
                while (rs.next()) {
                    PedidoRepartidorRow row = new PedidoRepartidorRow();
                    row.codPedido = rs.getString("CodPedido");
                    row.nombreCliente = rs.getString("NombreCliente");
                    row.direccionEntrega = rs.getString("DireccionEntrega");
                    row.metodoPago = rs.getString("MetodoPago");
                    row.estado = rs.getString("Estado");
                    result.add(row);
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar pedidos del repartidor: " + e.getMessage(), e);
        }
    }

    public static int contar(String codRepartidor) {
        String sql = "SELECT COUNT(*) AS total FROM Pedido WHERE CodRepartidor = ?";
        try (Connection con = DBConnection.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codRepartidor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error al contar pedidos del repartidor: " + e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 4: Compilar y verificar**

Run: `./mvnw test-compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/dtos/PedidoRepartidorRow.java src/main/java/com/laptitefrance/delivery/despacho/PaginaRepartidor.java src/main/java/com/laptitefrance/delivery/repositories/PedidoRepartidorRepositoryPagination.java
git commit -m "feat: consulta paginada de pedidos por repartidor (JOIN cliente+pago)"
```

---

## Task 2: `CentroDespacho` — `confirmarEntrega` y `pedidosDeRepartidor`

**Files:**
- Modify: `src/main/java/com/laptitefrance/delivery/despacho/CentroDespacho.java`
- Modify: `src/test/java/com/laptitefrance/delivery/despacho/CentroDespachoTest.java`

**Interfaces:**
- Consumes: `PaginaRepartidor`, `PedidoRepartidorRepositoryPagination` (Task 1), `ResultadoOperacion`, `Pedido`, `IRepositorioBase<Pedido,String>`.
- Produces:
  - `ResultadoOperacion confirmarEntrega(String codPedido, String codRepartidor)` — bajo `lockDe(codPedido)`: valida que el pedido exista, **pertenezca** a ese repartidor (`pedido.getCodRepartidor().equals(codRepartidor)`) y esté `EN CAMINO`; lo pasa a `ENTREGADO`, fija `TiempoEntReal`, persiste con `update`. No toca el pool de repartidores conectados (el flujo web no usa ese pool).
  - `PaginaRepartidor pedidosDeRepartidor(String codRepartidor, int page, int size)` — delega a `PedidoRepartidorRepositoryPagination` y calcula `totalPaginas`.

> `confirmarEntrega` se prueba con TDD (fake + concurrencia). `pedidosDeRepartidor` delega a una consulta BD; se verifica manualmente en la Task 3.

- [ ] **Step 1: Escribir los tests de `confirmarEntrega`**

Añadir a `src/test/java/com/laptitefrance/delivery/despacho/CentroDespachoTest.java` (dentro de la clase, junto a los demás `@Test`). Usa el helper `pedidoEnEspera` ya existente y añade uno nuevo para `EN CAMINO`:

```java
    private Pedido pedidoEnCamino(String cod, String codRepartidor) {
        Pedido p = new Pedido();
        p.setCodPedido(cod);
        p.setEstado("EN CAMINO");
        p.setCodRepartidor(codRepartidor);
        return p;
    }

    @Test
    void confirmarEntregaExitosaPasaAEntregado() {
        FakePedidoRepository repo = new FakePedidoRepository();
        repo.insert(pedidoEnCamino("P0001", "E004"));
        CentroDespacho centro = new CentroDespacho(repo);

        ResultadoOperacion r = centro.confirmarEntrega("P0001", "E004");

        assertEquals(ResultadoOperacion.Tipo.OK, r.getTipo());
        assertEquals("ENTREGADO", repo.findById("P0001").get().getEstado());
    }

    @Test
    void confirmarEntregaDePedidoDeOtroRepartidorEsRechazada() {
        FakePedidoRepository repo = new FakePedidoRepository();
        repo.insert(pedidoEnCamino("P0001", "E004"));
        CentroDespacho centro = new CentroDespacho(repo);

        ResultadoOperacion r = centro.confirmarEntrega("P0001", "E005");

        assertEquals(ResultadoOperacion.Tipo.REPARTIDOR_NO_DISPONIBLE, r.getTipo());
        assertEquals("EN CAMINO", repo.findById("P0001").get().getEstado());
    }

    @Test
    void confirmarEntregaDePedidoNoEnCaminoEsRechazada() {
        FakePedidoRepository repo = new FakePedidoRepository();
        Pedido p = new Pedido();
        p.setCodPedido("P0001");
        p.setEstado("EN ESPERA");
        p.setCodRepartidor("E004");
        repo.insert(p);
        CentroDespacho centro = new CentroDespacho(repo);

        ResultadoOperacion r = centro.confirmarEntrega("P0001", "E004");

        assertEquals(ResultadoOperacion.Tipo.YA_TOMADO, r.getTipo());
    }

    @Test
    void confirmarEntregaConcurrenteSoloUnaGana() throws InterruptedException {
        FakePedidoRepository repo = new FakePedidoRepository();
        repo.insert(pedidoEnCamino("P0001", "E004"));
        CentroDespacho centro = new CentroDespacho(repo);

        int hilos = 12;
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(hilos);
        java.util.concurrent.CountDownLatch listos = new java.util.concurrent.CountDownLatch(hilos);
        java.util.concurrent.CountDownLatch salida = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger oks = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < hilos; i++) {
            pool.submit(() -> {
                listos.countDown();
                try {
                    salida.await();
                    if (centro.confirmarEntrega("P0001", "E004").getTipo() == ResultadoOperacion.Tipo.OK) {
                        oks.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        listos.await();
        salida.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS));

        assertEquals(1, oks.get());
        assertEquals("ENTREGADO", repo.findById("P0001").get().getEstado());
    }
```

- [ ] **Step 2: Ejecutar y verificar que FALLA**

Run: `./mvnw test -Dtest=CentroDespachoTest`
Expected: FAIL — `cannot find symbol: method confirmarEntrega`.

- [ ] **Step 3: Implementar `confirmarEntrega` y `pedidosDeRepartidor`**

En `src/main/java/com/laptitefrance/delivery/despacho/CentroDespacho.java`, añadir al bloque de imports (si no están ya):

```java
import com.laptitefrance.delivery.despacho.PaginaRepartidor;
import com.laptitefrance.delivery.repositories.PedidoRepartidorRepositoryPagination;
```

(El primero está en el mismo paquete `despacho`, por lo que el import es innecesario y puede omitirse; añade solo el de `PedidoRepartidorRepositoryPagination`.)

Añadir estos dos métodos dentro de la clase, antes de la última llave `}`:

```java
    // --- Flujo web del repartidor ------------------------------------------

    /**
     * Confirma la entrega de un pedido asignado a un repartidor. Bajo el lock del
     * pedido: valida existencia, pertenencia y estado EN CAMINO; lo pasa a
     * ENTREGADO y persiste. No usa el pool de repartidores conectados.
     */
    public ResultadoOperacion confirmarEntrega(String codPedido, String codRepartidor) {
        if (codPedido == null || codPedido.isBlank()) {
            return ResultadoOperacion.noEncontrado("Código de pedido vacío.");
        }
        if (codRepartidor == null || codRepartidor.isBlank()) {
            return ResultadoOperacion.repartidorNoDisponible("Código de repartidor vacío.");
        }

        ReentrantLock lock = lockDe(codPedido);
        lock.lock();
        try {
            Optional<Pedido> opt = pedidoRepository.findById(codPedido);
            if (opt.isEmpty()) {
                return ResultadoOperacion.noEncontrado("No existe el pedido: " + codPedido);
            }
            Pedido pedido = opt.get();
            if (!codRepartidor.equals(pedido.getCodRepartidor())) {
                return ResultadoOperacion.repartidorNoDisponible(
                        "El pedido " + codPedido + " no está asignado a " + codRepartidor);
            }
            if (!EN_CAMINO.equalsIgnoreCase(pedido.getEstado())) {
                return ResultadoOperacion.yaTomado("El pedido no está EN CAMINO: " + codPedido);
            }

            pedido.setEstado(ENTREGADO);
            pedido.setTiempoEntReal(LocalDateTime.now());
            try {
                pedidoRepository.update(pedido);
            } catch (RuntimeException ex) {
                pedido.setEstado(EN_CAMINO); // revertir el cambio en memoria
                return ResultadoOperacion.errorInterno("Error al persistir la entrega: " + ex.getMessage());
            }
            return ResultadoOperacion.ok("Pedido " + codPedido + " entregado.", null);
        } finally {
            lock.unlock();
        }
    }

    /** Página de los pedidos asignados a un repartidor (para la tabla web). */
    public PaginaRepartidor pedidosDeRepartidor(String codRepartidor, int page, int size) {
        int p = Math.max(1, page);
        int s = Math.max(1, size);
        var filas = PedidoRepartidorRepositoryPagination.listar(codRepartidor, p, s);
        int total = PedidoRepartidorRepositoryPagination.contar(codRepartidor);
        int totalPaginas = (int) Math.ceil(total / (double) s);
        if (totalPaginas < 1) totalPaginas = 1;
        return new PaginaRepartidor(filas, p, totalPaginas, total);
    }
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `./mvnw test -Dtest=CentroDespachoTest`
Expected: PASS (los 7 tests previos + 4 nuevos = 11).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/despacho/CentroDespacho.java src/test/java/com/laptitefrance/delivery/despacho/CentroDespachoTest.java
git commit -m "feat: confirmarEntrega (lock por pedido) y pedidosDeRepartidor en CentroDespacho"
```

---

## Task 3: `ServidorWebRepartidor` (Javalin) — endpoints + estáticos

**Files:**
- Create: `src/main/java/com/laptitefrance/delivery/despacho/ServidorWebRepartidor.java`
- Delete: `src/main/java/com/laptitefrance/delivery/despacho/ApiDeliveryServer.java`

**Interfaces:**
- Consumes: `CentroDespacho` (`pedidosDeRepartidor`, `confirmarEntrega`, `iniciar`, `detener`), `ResultadoOperacion`, `PedidoRepository`.
- Produces:
  - `ServidorWebRepartidor(CentroDespacho centro)` con `void iniciar(int puerto)` y `void detener()`.
  - `main(String[])` autónomo (arranca un `CentroDespacho` propio) — útil para correr solo el servidor.

> Verificación: **compila** + **manual con BD** (curl). No hay test JUnit del servidor (capa fina), igual que `ApiDeliveryServer` no lo tenía.

- [ ] **Step 1: Implementar el servidor web**

Crear `src/main/java/com/laptitefrance/delivery/despacho/ServidorWebRepartidor.java`:

```java
package com.laptitefrance.delivery.despacho;

import com.laptitefrance.delivery.repositories.PedidoRepository;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;

import java.util.Map;

/**
 * Servidor web del repartidor. Sirve la página HTML (recursos en /web del
 * classpath) y los endpoints REST que la página consume. Delega en una única
 * instancia compartida de CentroDespacho (thread-safe). Reemplaza al antiguo
 * ApiDeliveryServer.
 */
public class ServidorWebRepartidor {

    public static final int PUERTO_DEFECTO = 8080;

    private final CentroDespacho centro;
    private Javalin app;

    public ServidorWebRepartidor(CentroDespacho centro) {
        this.centro = centro;
    }

    public void iniciar(int puerto) {
        app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
            config.staticFiles.add(staticFiles -> {
                staticFiles.directory = "/web";
                staticFiles.location = Location.CLASSPATH;
            });
        }).start(puerto);

        // Pedidos asignados a un repartidor, paginados.
        app.get("/api/repartidor/{cod}/pedidos", ctx -> {
            int page = parseIntOr(ctx.queryParam("page"), 1);
            int size = parseIntOr(ctx.queryParam("size"), 10);
            ctx.json(centro.pedidosDeRepartidor(ctx.pathParam("cod"), page, size));
        });

        // Confirmar entrega de un pedido.
        app.post("/api/pedidos/{cod}/entregar", ctx ->
                responder(ctx, centro.confirmarEntrega(ctx.pathParam("cod"), ctx.queryParam("repartidor"))));

        System.out.println("✅ Servidor web del repartidor en http://localhost:" + puerto + "/");
    }

    public void detener() {
        if (app != null) {
            app.stop();
        }
    }

    private static int parseIntOr(String valor, int defecto) {
        if (valor == null || valor.isBlank()) return defecto;
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return defecto;
        }
    }

    private static void responder(Context ctx, ResultadoOperacion r) {
        ctx.status(r.httpStatus());
        ctx.json(Map.of(
                "tipo", r.getTipo().name(),
                "mensaje", r.getMensaje() == null ? "" : r.getMensaje()));
    }

    /** Permite arrancar solo el servidor web (sin el dashboard). */
    public static void main(String[] args) {
        CentroDespacho centro = new CentroDespacho(new PedidoRepository());
        ServidorWebRepartidor servidor = new ServidorWebRepartidor(centro);
        servidor.iniciar(PUERTO_DEFECTO);
        Runtime.getRuntime().addShutdownHook(new Thread(servidor::detener));
    }
}
```

- [ ] **Step 2: Eliminar el servidor anterior**

```bash
git rm src/main/java/com/laptitefrance/delivery/despacho/ApiDeliveryServer.java
```

- [ ] **Step 3: Compilar (nada debe referenciar la clase borrada)**

Run: `./mvnw test-compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Verificación manual (requiere BD y un pedido asignado a E004)**

Con `SQLQuery1.sql` cargado y un pedido en `EN CAMINO` asignado a `E004`
(p. ej. el `P0001` del script, que ya tiene `CodRepartidor='E004'`):

```bash
./mvnw exec:java -Dexec.mainClass=com.laptitefrance.delivery.despacho.ServidorWebRepartidor
# en otra terminal:
curl "http://localhost:8080/api/repartidor/E004/pedidos?page=1&size=10"
# Esperado: JSON { "filas":[{ "codPedido":"P0001","nombreCliente":"...","direccionEntrega":"...","metodoPago":"...","estado":"EN CAMINO"}], "page":1, "totalPaginas":1, "totalItems":1 }

curl -X POST "http://localhost:8080/api/pedidos/P0001/entregar?repartidor=E004"
# Esperado: 200 {"tipo":"OK","mensaje":"Pedido P0001 entregado."}

curl -X POST "http://localhost:8080/api/pedidos/P0001/entregar?repartidor=E004"
# Esperado: 409 {"tipo":"YA_TOMADO",...}  (ya no está EN CAMINO)
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/despacho/ServidorWebRepartidor.java
git commit -m "feat: ServidorWebRepartidor (Javalin) con vista web y endpoints; retira ApiDeliveryServer"
```

---

## Task 4: Página web del repartidor (HTML + CSS + JS)

**Files:**
- Create: `src/main/resources/web/index.html`
- Create: `src/main/resources/web/styles.css`
- Create: `src/main/resources/web/app.js`

**Interfaces:**
- Consumes (vía HTTP): `GET /api/repartidor/{cod}/pedidos?page&size` → `PaginaRepartidor`; `POST /api/pedidos/{cod}/entregar?repartidor={cod}` → `{tipo,mensaje}`.

> Verificación: **manual en el navegador** (abrir `http://localhost:8080`).

- [ ] **Step 1: Crear `index.html`**

Crear `src/main/resources/web/index.html`:

```html
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>La P'tite France — Pedidos del repartidor</title>
    <link rel="stylesheet" href="styles.css">
</head>
<body>
    <header>
        <h1>🛵 Mis pedidos</h1>
        <div class="buscador">
            <input id="codRepartidor" type="text" placeholder="Tu código (ej. E004)" />
            <button id="btnBuscar">Buscar mis pedidos</button>
        </div>
    </header>

    <main>
        <p id="mensaje" class="mensaje"></p>
        <table id="tabla">
            <thead>
                <tr>
                    <th>Pedido</th>
                    <th>Nombre</th>
                    <th>Dirección</th>
                    <th>Método de pago</th>
                    <th>Estado</th>
                    <th>Acción</th>
                </tr>
            </thead>
            <tbody id="cuerpoTabla"></tbody>
        </table>

        <div class="paginacion">
            <button id="btnAnterior">&laquo; Anterior</button>
            <span id="infoPagina">Página 0 de 0</span>
            <button id="btnSiguiente">Siguiente &raquo;</button>
        </div>
    </main>

    <script src="app.js"></script>
</body>
</html>
```

- [ ] **Step 2: Crear `styles.css`**

Crear `src/main/resources/web/styles.css`:

```css
* { box-sizing: border-box; }
body {
    font-family: Arial, Helvetica, sans-serif;
    margin: 0;
    background: #f0f2f5;
    color: #1c1e21;
}
header {
    background: #2e3b4e;
    color: #fff;
    padding: 16px 24px;
}
header h1 { margin: 0 0 12px; font-size: 22px; }
.buscador { display: flex; gap: 8px; }
.buscador input {
    flex: 1;
    max-width: 280px;
    padding: 10px;
    border: none;
    border-radius: 6px;
    font-size: 15px;
}
.buscador button, .paginacion button {
    padding: 10px 16px;
    border: none;
    border-radius: 6px;
    background: #2ecc71;
    color: #fff;
    font-weight: bold;
    cursor: pointer;
}
.buscador button:hover, .paginacion button:hover { background: #27ae60; }
main { padding: 24px; }
.mensaje { min-height: 20px; color: #c0392b; font-weight: bold; }
table {
    width: 100%;
    border-collapse: collapse;
    background: #fff;
    box-shadow: 0 1px 3px rgba(0,0,0,.1);
}
th, td { padding: 12px; text-align: left; border-bottom: 1px solid #e4e6eb; }
th { background: #f5f6f7; }
.btn-entregar {
    padding: 8px 12px;
    border: none;
    border-radius: 6px;
    background: #3498db;
    color: #fff;
    cursor: pointer;
    font-weight: bold;
}
.btn-entregar:hover { background: #2980b9; }
.btn-entregar:disabled { background: #95a5a6; cursor: default; }
.paginacion {
    display: flex;
    align-items: center;
    gap: 16px;
    justify-content: center;
    margin-top: 16px;
}
.paginacion button:disabled { background: #95a5a6; cursor: default; }
```

- [ ] **Step 3: Crear `app.js`**

Crear `src/main/resources/web/app.js`:

```javascript
let codActual = "";
let paginaActual = 1;
let totalPaginas = 1;
const TAM_PAGINA = 10;

const $ = (id) => document.getElementById(id);

function mostrarMensaje(texto, esError = true) {
    const m = $("mensaje");
    m.textContent = texto;
    m.style.color = esError ? "#c0392b" : "#27ae60";
}

async function cargarPedidos() {
    if (!codActual) return;
    try {
        const resp = await fetch(`/api/repartidor/${encodeURIComponent(codActual)}/pedidos?page=${paginaActual}&size=${TAM_PAGINA}`);
        if (!resp.ok) { mostrarMensaje("No se pudieron cargar los pedidos."); return; }
        const data = await resp.json();
        totalPaginas = data.totalPaginas || 1;
        paginaActual = data.page || 1;
        pintarTabla(data.filas || []);
        $("infoPagina").textContent = `Página ${paginaActual} de ${totalPaginas}`;
        $("btnAnterior").disabled = paginaActual <= 1;
        $("btnSiguiente").disabled = paginaActual >= totalPaginas;
        if ((data.filas || []).length === 0) mostrarMensaje("No tienes pedidos asignados.", false);
        else mostrarMensaje("");
    } catch (e) {
        mostrarMensaje("Error de conexión con el servidor.");
    }
}

function pintarTabla(filas) {
    const cuerpo = $("cuerpoTabla");
    cuerpo.innerHTML = "";
    for (const f of filas) {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${f.codPedido ?? ""}</td>
            <td>${f.nombreCliente ?? ""}</td>
            <td>${f.direccionEntrega ?? ""}</td>
            <td>${f.metodoPago ?? ""}</td>
            <td>${f.estado ?? ""}</td>`;
        const tdAccion = document.createElement("td");
        if (f.estado === "EN CAMINO") {
            const btn = document.createElement("button");
            btn.className = "btn-entregar";
            btn.textContent = "Confirmar entrega";
            btn.onclick = () => confirmarEntrega(f.codPedido, btn);
            tdAccion.appendChild(btn);
        } else {
            tdAccion.textContent = "—";
        }
        tr.appendChild(tdAccion);
        cuerpo.appendChild(tr);
    }
}

async function confirmarEntrega(codPedido, btn) {
    btn.disabled = true;
    try {
        const resp = await fetch(`/api/pedidos/${encodeURIComponent(codPedido)}/entregar?repartidor=${encodeURIComponent(codActual)}`, { method: "POST" });
        const data = await resp.json();
        if (resp.ok) {
            mostrarMensaje(`Pedido ${codPedido} entregado.`, false);
        } else {
            mostrarMensaje(data.mensaje || "No se pudo confirmar la entrega.");
        }
    } catch (e) {
        mostrarMensaje("Error de conexión con el servidor.");
    } finally {
        cargarPedidos();
    }
}

$("btnBuscar").onclick = () => {
    const cod = $("codRepartidor").value.trim();
    if (!cod) { mostrarMensaje("Ingresa tu código de repartidor."); return; }
    codActual = cod;
    paginaActual = 1;
    cargarPedidos();
};

$("btnAnterior").onclick = () => { if (paginaActual > 1) { paginaActual--; cargarPedidos(); } };
$("btnSiguiente").onclick = () => { if (paginaActual < totalPaginas) { paginaActual++; cargarPedidos(); } };

// Refresco periódico de la tabla mientras haya un repartidor cargado.
setInterval(() => { if (codActual) cargarPedidos(); }, 5000);
```

- [ ] **Step 4: Compilar (para que los recursos vayan a target/classes)**

Run: `./mvnw test-compile`
Expected: `BUILD SUCCESS` (los recursos de `src/main/resources/web` se copian a `target/classes/web`).

- [ ] **Step 5: Verificación manual en navegador**

Arrancar `ServidorWebRepartidor` (o el `Main` de la Task 5), abrir `http://localhost:8080`, escribir `E004`,
pulsar "Buscar mis pedidos": se ve la tabla con sus pedidos; "Confirmar entrega" en un `EN CAMINO` lo
pasa a `ENTREGADO` y la fila se actualiza.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/web/index.html src/main/resources/web/styles.css src/main/resources/web/app.js
git commit -m "feat: página web del repartidor (tabla paginada + confirmar entrega)"
```

---

## Task 5: `Main` como ejecutable único (servidor web + login)

**Files:**
- Modify: `src/main/java/com/laptitefrance/delivery/Main.java`

**Interfaces:**
- Consumes: `ServidorWebRepartidor`, `CentroDespacho`, `PedidoRepository`, `LoginView`.

> Verificación: **manual** (arrancar y ver que se abren el login y el servidor web).

- [ ] **Step 1: Reescribir `Main` para arrancar todo**

Reemplazar el contenido de `src/main/java/com/laptitefrance/delivery/Main.java` por:

```java
package com.laptitefrance.delivery;

import com.laptitefrance.delivery.despacho.CentroDespacho;
import com.laptitefrance.delivery.despacho.ServidorWebRepartidor;
import com.laptitefrance.delivery.repositories.PedidoRepository;
import com.laptitefrance.delivery.views.LoginView;

import javax.swing.SwingUtilities;

/**
 * Punto de entrada único de la aplicación. Inicializa, en el mismo proceso:
 *  - el servidor web del repartidor (Javalin, puerto 8080), y
 *  - la interfaz de escritorio del asistente (Swing), empezando por el login.
 *
 * Ambos comparten la misma base de datos; el servidor web usa su propio
 * CentroDespacho (thread-safe) para coordinar la confirmación de entrega.
 *
 * Se usa el Look & Feel multiplataforma por defecto a propósito: el L&F del
 * sistema (Windows) no pinta el color de fondo de los JButton, dejando el texto
 * invisible.
 */
public class Main {

    public static void main(String[] args) {
        // 1. Servidor web del repartidor (hilos propios de Javalin).
        CentroDespacho centro = new CentroDespacho(new PedidoRepository());
        ServidorWebRepartidor servidor = new ServidorWebRepartidor(centro);
        servidor.iniciar(ServidorWebRepartidor.PUERTO_DEFECTO);
        Runtime.getRuntime().addShutdownHook(new Thread(servidor::detener));

        // 2. Interfaz de escritorio del asistente (Event Dispatch Thread).
        SwingUtilities.invokeLater(() -> new LoginView().setVisible(true));
    }
}
```

- [ ] **Step 2: Compilar**

Run: `./mvnw test-compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Verificación manual**

Run: `./mvnw exec:java` (mainClass `com.laptitefrance.delivery.Main` ya configurado en el pom).
Expected: en consola aparece "Servidor web del repartidor en http://localhost:8080/" y se abre la
ventana de login. Con `E001` se entra al dashboard; en el navegador, `http://localhost:8080` con `E004`
muestra sus pedidos.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/Main.java
git commit -m "feat: Main como ejecutable único (servidor web + dashboard del asistente)"
```

---

## Task 6: Auto-refresco del Monitor del asistente

**Files:**
- Modify: `src/main/java/com/laptitefrance/delivery/views/PanelMonitorPedidos.java`

**Interfaces:**
- Consumes: el `cargarPedidos(String)` y `cbxFiltroEstado` ya existentes en el panel.

> Verificación: **manual** (el monitor se recarga solo; una entrega confirmada en la web aparece en ≤ unos segundos).

- [ ] **Step 1: Añadir el import de Timer**

En `src/main/java/com/laptitefrance/delivery/views/PanelMonitorPedidos.java`, añadir al bloque de imports:

```java
import javax.swing.Timer;
```

- [ ] **Step 2: Añadir el campo del timer y arrancarlo en el constructor**

En la declaración de campos de la clase (junto a `private JTable tablaPedidos;` etc.), añadir:

```java
    private Timer timerRefresco;
```

Al final del constructor `public PanelMonitorPedidos(PedidoController pedidoController)`, justo después de
`cargarPedidos("TODOS");`, añadir:

```java
        // Auto-refresco: refleja los cambios hechos por los repartidores desde la web.
        timerRefresco = new Timer(3000, e -> cargarPedidos((String) cbxFiltroEstado.getSelectedItem()));
        timerRefresco.start();
```

- [ ] **Step 3: Detener el timer al quitar el panel (evitar fuga del hilo)**

Añadir este método dentro de la clase `PanelMonitorPedidos` (p. ej. tras el constructor):

```java
    @Override
    public void removeNotify() {
        if (timerRefresco != null) {
            timerRefresco.stop();
        }
        super.removeNotify();
    }
```

- [ ] **Step 4: Compilar**

Run: `./mvnw test-compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Verificación manual**

Arrancar `Main`, entrar como asistente (`E001`), abrir el Monitor. Desde la web (`E004`) confirmar la
entrega de un pedido `EN CAMINO`; en ≤3 s el Monitor del asistente muestra ese pedido como `ENTREGADO`
sin intervención.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/views/PanelMonitorPedidos.java
git commit -m "feat: auto-refresco del Monitor del asistente con javax.swing.Timer"
```

---

## Self-Review (completado por el autor del plan)

**1. Cobertura del spec:**
- §6.1 AppLauncher / ejecutable único → Task 5 (Main reescrito) ✓
- §6.2 ServidorWebRepartidor (reemplaza ApiDeliveryServer) → Task 3 ✓
- §6.3 página web (buscador por código, tabla, paginación, confirmar entrega, refresco) → Task 4 ✓
- §6.4 CentroDespacho.pedidosDeRepartidor + confirmarEntrega (lock por pedido) → Task 2 ✓
- §6.5 DTO PedidoRepartidorRow + PaginaRepartidor + consulta JOIN → Tasks 1, 2 ✓
- §6.6 Monitor auto-refresco (Timer) → Task 6 ✓
- §7 flujo de confirmación (lock + persistir + refresco del monitor) → Tasks 2, 6 ✓
- §8 manejo de errores (mapeo HTTP) → Tasks 2 (ResultadoOperacion) y 3 (responder) ✓
- §9 pruebas (unitarias de confirmarEntrega + manual) → Task 2 (TDD) + Tasks 3-6 (manual) ✓

**2. Placeholders:** ninguno; cada paso trae código/comando completo.

**3. Consistencia de tipos:** `confirmarEntrega(String,String)`, `pedidosDeRepartidor(String,int,int)`,
`PaginaRepartidor(filas,page,totalPaginas,totalItems)`, `PedidoRepartidorRow` (campos públicos),
`PedidoRepartidorRepositoryPagination.listar/contar`, `ServidorWebRepartidor(CentroDespacho)` +
`iniciar(int)`/`detener()`/`PUERTO_DEFECTO` — usados con las mismas firmas en todas las tareas.
Endpoints `GET /api/repartidor/{cod}/pedidos` y `POST /api/pedidos/{cod}/entregar?repartidor=` idénticos
entre la página web (Task 4), el servidor (Task 3) y la verificación manual.
