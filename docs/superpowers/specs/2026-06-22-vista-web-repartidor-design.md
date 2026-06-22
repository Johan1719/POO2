# Diseño — Vista web del repartidor (entrega de pedidos)

**Fecha:** 2026-06-22
**Proyecto:** La P'tite France — Sistema de Delivery (POO2)
**Rama:** Project_v2

---

## 1. Objetivo

Dar a la API una **cara web** para el repartidor. El asistente asigna pedidos a los repartidores
desde el dashboard de escritorio (Swing); cada repartidor abre una **página web** donde ve, en una
**tabla paginada**, los pedidos que le fueron asignados y **confirma la entrega** con un botón. La
confirmación cambia el estado del pedido a `ENTREGADO`, lo **persiste en la base de datos** y se
**refleja automáticamente** en el Monitor del asistente.

Todo arranca desde un **único ejecutable**: el dashboard del asistente (Swing) y el servidor web
se inicializan juntos en el mismo proceso.

---

## 2. Decisiones tomadas (aprobadas)

1. **Solo el asistente asigna** los pedidos a los repartidores (desde el Monitor Swing). La web del
   repartidor solo muestra lo asignado y permite confirmar la entrega. Se deja de lado la
   competencia/auto-asignación como flujo principal.
2. **Un único ejecutable** inicializa todo: al arrancar la app se levanta el dashboard Swing y el
   servidor web en el **mismo proceso** (memoria compartida).
3. **Auto-refresco** del Monitor del asistente cada ~3 segundos (timer en segundo plano) para
   reflejar las entregas confirmadas desde la web.
4. La tabla de la web está **paginada** (Anterior / Siguiente, "Página X de N").
5. El repartidor solo avanza **EN CAMINO → ENTREGADO** (botón "Confirmar entrega"). El asistente
   sigue manejando el resto de estados desde Swing.

---

## 3. Alcance

**Incluye:**
- Ejecutable único (`AppLauncher`) que arranca Swing + servidor web en el mismo proceso.
- Servidor web (Javalin) que sirve la página HTML del repartidor + endpoints REST.
- Página web (HTML/CSS/JS): pantalla de ingreso de código de repartidor + tabla paginada de pedidos
  con botón "Confirmar entrega" por fila.
- Servicio de despacho compartido en memoria, thread-safe (`CentroDespacho` ampliado), usado por el
  servidor web y por el Swing, con cambios de estado **bajo lock por pedido**.
- DTO `PedidoRepartidorRow` y consulta `JOIN` Pedido + Cliente + Pago, paginada y filtrada por
  repartidor.
- Auto-refresco del Monitor del asistente (`javax.swing.Timer`).

**No incluye (YAGNI):**
- Autenticación real del repartidor (solo ingresa su código; sin contraseña/token).
- Notificaciones push/WebSocket (la sincronización del Monitor es por refresco periódico).
- Cambios de estado distintos de `ENTREGADO` desde la web.
- Eliminación del código de competencia/auto-asignación: se **conserva** como demostración de
  concurrencia (`CentroDespacho` cola/despachador, `SimuladorConcurrencia`).
- Cambios en el esquema de la base de datos.

---

## 4. Contexto del código existente (lo que se reutiliza)

- **Estados de un pedido:** `EN ESPERA` → `EN CAMINO` → `ENTREGADO`.
- **Asignación (asistente):** `PanelMonitorPedidos.modalAsignarRepartidor` →
  `PedidoController.asignarRepartidor(codPedido, codRepartidor)` → BD (estado `EN CAMINO`,
  `CodRepartidor` asignado).
- **Servicio concurrente:** `com.laptitefrance.delivery.despacho.CentroDespacho` ya coordina cambios
  de estado bajo un `ReentrantLock` por pedido (`lockDe`, `entregarPedido`) y persiste vía
  `PedidoRepository`.
- **Esquema (SQLQuery1.sql):**
  - `Pedido(CodPedido, DireccionEntrega, IDCliente, CodRepartidor, CodPago, Estado, ...)`
  - `Cliente(IDCliente, NombreCliente, ...)`
  - `Pago(CodPago, MetodoPago, ...)`
- **Javalin 6.1.3 + Jackson** ya son dependencias.
- **Paginación:** patrón `page`/`pageSize` ya usado en repositorios y `PaginatorPanel` (Swing).

---

## 5. Arquitectura

Un solo proceso. El ejecutable crea **una** instancia del servicio de despacho compartido y la
inyecta tanto al servidor web como al dashboard Swing.

```
                    ┌──────────────────────────────────────────────┐
                    │              AppLauncher (main)               │
                    │   crea CentroDespacho (servicio compartido)   │
                    └───────────────┬───────────────┬──────────────┘
                                    │               │
              inyecta el mismo servicio             │ inyecta el mismo servicio
                                    │               │
            ┌───────────────────────▼──┐        ┌───▼──────────────────────────┐
            │   Servidor web (Javalin)  │        │   Dashboard Swing (asistente)│
            │   - sirve página HTML     │        │   - asigna repartidores      │
            │   - endpoints REST        │        │   - Monitor con auto-refresco│
            └───────────┬──────────────┘        └───────────┬──────────────────┘
                        │  hilos de Javalin                  │  EDT + Timer
                        └───────────────┬────────────────────┘
                                        ▼
                         CentroDespacho (thread-safe)
                         - lock por pedido en cambios de estado
                                        │
                                        ▼
                                 PedidoRepository / BD
```

**Concurrencia (el corazón se mantiene):** los hilos del servidor web (varios repartidores) y el
hilo de la interfaz (asistente) pueden tocar pedidos a la vez. El cambio de estado se hace **bajo el
lock del pedido**, evitando que la edición del asistente y la confirmación del repartidor se pisen.

---

## 6. Componentes en detalle

### 6.1 `AppLauncher` (nuevo punto de entrada)
- `main`: crea `CentroDespacho` (con `PedidoRepository` real), arranca el servidor web
  (`ServidorWebRepartidor`) pasándole el servicio, y abre el flujo Swing (login → dashboard)
  pasándole el mismo servicio. Registra un shutdown hook para detener el servidor.
- El `Main` actual (solo Swing) se conserva o se reemplaza por `AppLauncher`; decisión menor del plan.

### 6.2 `ServidorWebRepartidor` (servidor web Javalin)
- Configura Javalin (puerto 8080, CORS abierto) y registra:
  - `GET /` → sirve la página HTML del repartidor (recurso estático).
  - `GET /api/repartidor/{cod}/pedidos?page=1&size=10` → delega en el servicio y devuelve JSON con la
    página de `PedidoRepartidorRow` + metadatos de paginación.
  - `POST /api/pedidos/{cod}/entregar?repartidor={cod}` → delega en `cambiar a ENTREGADO`, devuelve
    `ResultadoOperacion` mapeado a HTTP.
- **Reemplaza** al actual `ApiDeliveryServer`: habrá **un solo servidor** en el puerto 8080
  (`ServidorWebRepartidor`). Los endpoints antiguos de competencia (tomar/conectar/pendientes) se
  retiran de la cara web; el `CentroDespacho` conserva esos métodos internamente para el
  `SimuladorConcurrencia`, pero no se exponen por HTTP.

### 6.3 Página web (`src/main/resources/web/`)
- `index.html` + `app.js` + `styles.css` (recursos servidos por Javalin).
- Flujo:
  1. La pantalla inicial es un **buscador por código de repartidor**: el repartidor escribe su
     código (ej. `E004`) y pulsa "Buscar mis pedidos". Ese código es la clave con la que se obtiene
     su lista (no hay otra identificación). El código queda recordado en la página para los refrescos
     y la confirmación de entrega.
  2. `app.js` hace `GET /api/repartidor/E004/pedidos?page=1&size=10` y pinta la tabla:
     **Pedido | Nombre | Dirección | Método de pago | Estado | Acción**.
  3. Controles de paginación *Anterior / Siguiente* con "Página X de N".
  4. Botón **"Confirmar entrega"** por fila → `POST /api/pedidos/{cod}/entregar?repartidor=E004` →
     al recibir OK, recarga la página actual (la fila pasa a `ENTREGADO` o desaparece según filtro).
  5. Refresco periódico opcional cada pocos segundos (la tabla se recarga sola).

### 6.4 Servicio compartido — `CentroDespacho` (ampliado)
Se añaden dos métodos (reutilizando el lock por pedido y el repositorio existentes):
- `PaginaRepartidor pedidosDeRepartidor(String codRepartidor, int page, int size)` — devuelve la
  página de `PedidoRepartidorRow` y el total (para "Página X de N"). Lee vía un repositorio de
  consulta con `JOIN` Pedido + Cliente + Pago.
- `ResultadoOperacion confirmarEntrega(String codPedido, String codRepartidor)` — bajo
  `lockDe(codPedido)`: valida que el pedido exista, esté `EN CAMINO` y pertenezca a ese repartidor,
  lo pasa a `ENTREGADO`, fija `TiempoEntReal`, persiste con `PedidoRepository.update`. Mapea a
  `ResultadoOperacion` (OK / NO_ENCONTRADO / REPARTIDOR_NO_DISPONIBLE / YA_TOMADO / ERROR_INTERNO).
  (Es esencialmente el `entregarPedido` actual, reforzando la validación de pertenencia y estado.)

### 6.5 DTO y consulta
- `dtos.PedidoRepartidorRow`: `String codPedido, String nombreCliente, String direccionEntrega,
  String metodoPago, String estado`.
- `PaginaRepartidor`: `List<PedidoRepartidorRow> filas; int page; int totalPaginas; long totalItems`.
- Consulta (en `PedidoRepository` o un repositorio de consulta nuevo), con paginación SQL Server
  (`OFFSET/FETCH`):
  ```sql
  SELECT p.CodPedido, c.NombreCliente, p.DireccionEntrega, pg.MetodoPago, p.Estado
  FROM Pedido p
  JOIN Cliente c ON p.IDCliente = c.IDCliente
  LEFT JOIN Pago pg ON p.CodPago = pg.CodPago
  WHERE p.CodRepartidor = ?
  ORDER BY p.Fechasolicitud DESC
  OFFSET ? ROWS FETCH NEXT ? ROWS ONLY;
  ```
  más un `SELECT COUNT(*)` análogo para el total.

### 6.6 Monitor del asistente (`PanelMonitorPedidos`, ampliado)
- Se añade un `javax.swing.Timer` (~3000 ms) que llama a `cargarPedidos(filtroActual)` en el EDT.
- El timer se detiene cuando el panel se descarta (al cerrar el dashboard) para no fugar el hilo.
- No cambia la lógica existente de asignación/edición.

---

## 7. Flujo de datos (confirmación de entrega)

```
Repartidor (navegador)                Servidor web (hilo Javalin)        CentroDespacho           BD
  │  click "Confirmar entrega" P0001        │                                  │                  │
  │  POST /api/pedidos/P0001/entregar ─────►│                                  │                  │
  │                                         │  confirmarEntrega(P0001, E004) ─►│                  │
  │                                         │                                  │ lock(P0001)      │
  │                                         │                                  │ valida EN CAMINO │
  │                                         │                                  │ + pertenencia    │
  │                                         │                                  │ update ENTREGADO ►│ persiste
  │                                         │                                  │ unlock(P0001)    │
  │  200 OK  ◄──────────────────────────────│◄─── ResultadoOperacion(OK) ──────│                  │
  │  recarga su tabla                       │                                  │                  │
                                                                              (≤3 s después)
  Asistente (Swing): el Timer recarga el Monitor desde BD → P0001 aparece como ENTREGADO
```

---

## 8. Manejo de errores

| Situación | `ResultadoOperacion.Tipo` | HTTP |
|---|---|---|
| Entrega confirmada | `OK` | 200 |
| Pedido no existe | `NO_ENCONTRADO` | 404 |
| Pedido no asignado a ese repartidor | `REPARTIDOR_NO_DISPONIBLE` | 400 |
| Pedido no está `EN CAMINO` (ya entregado u otro) | `YA_TOMADO` | 409 |
| Código de pedido/repartidor vacío | `NO_ENCONTRADO` / `REPARTIDOR_NO_DISPONIBLE` | 404 / 400 |
| Fallo de BD | `ERROR_INTERNO` | 500 |

El `app.js` muestra un mensaje breve según el código HTTP (éxito, ya entregado, error).

---

## 9. Pruebas / verificación

- **Unitarias (JUnit):** `confirmarEntrega` con `FakePedidoRepository`:
  - éxito EN CAMINO → ENTREGADO;
  - pedido de otro repartidor → `REPARTIDOR_NO_DISPONIBLE`;
  - pedido ya ENTREGADO → `YA_TOMADO`;
  - concurrencia: varios hilos confirmando el mismo pedido → exactamente uno OK, el resto `YA_TOMADO`.
- **Paginación:** `pedidosDeRepartidor` con un fake que devuelve N filas → páginas correctas y total.
- **Manual (con BD):** arrancar `AppLauncher`, asignar un pedido a `E004` desde el Monitor, abrir
  `http://localhost:8080`, ingresar `E004`, ver el pedido, "Confirmar entrega", y comprobar que el
  Monitor del asistente pasa a `ENTREGADO` solo en ≤3 s.

---

## 10. Riesgos y notas

- **Acceso desde otro dispositivo:** el repartidor entra por `http://<IP-del-asistente>:8080`. Requiere
  red local y que el firewall permita el puerto 8080 (nota de despliegue, no de código).
- **El servidor muere con la app:** al ser el mismo proceso, cerrar el dashboard apaga el servidor web.
  Aceptable para el alcance académico; documentado.
- **Auto-refresco vs. acciones del asistente:** el Timer recarga la tabla; debe preservar el filtro y
  la página actual y no interferir con un modal abierto (recargar solo el modelo de la tabla).
- **Seguridad académica:** sin autenticación real; CORS abierto. No apto para producción (ya advertido
  en el README).
