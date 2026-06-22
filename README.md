# 🥖 La P'tite France — Sistema de Delivery (POO2)

Aplicación de una panadería/restaurante con servicio de delivery. Combina una **interfaz de
escritorio (Swing)** para el asistente que atiende y despacha los pedidos, y una **vista web** para
que cada repartidor consulte los pedidos que le asignaron y **confirme la entrega** desde su
navegador. Desarrollada en **Java 21**, con persistencia en **SQL Server** y una **API web (Javalin)**
que comparte proceso con el escritorio. Aplica **arquitectura por capas**, varios **patrones de
diseño** y **programación concurrente** real.

---

## 📑 Tabla de contenido
1. [Tecnologías](#-tecnologías)
2. [Cómo ejecutar](#-cómo-ejecutar)
3. [Flujo completo (asistente ↔ repartidor)](#-flujo-completo-asistente--repartidor)
4. [Vista web del repartidor](#-vista-web-del-repartidor)
5. [Programación concurrente](#-programación-concurrente)
6. [Estructura del proyecto](#-estructura-del-proyecto)
7. [Arquitectura por capas](#-arquitectura-por-capas)
8. [Patrones de diseño](#-patrones-de-diseño)
9. [Notas y decisiones](#-notas-y-decisiones)

---

## 🛠 Tecnologías

| Componente        | Detalle                                                                 |
|-------------------|-------------------------------------------------------------------------|
| Lenguaje          | Java 21                                                                  |
| Escritorio        | Java Swing (`javax.swing`)                                               |
| Web / API REST    | [Javalin](https://javalin.io/) 6 + Jackson (sirve HTML + endpoints JSON) |
| Concurrencia      | `java.util.concurrent` (`ConcurrentHashMap`, `ReentrantLock`, `AtomicReference`, `ExecutorService`, `BlockingQueue`) |
| Base de datos     | SQL Server (driver `mssql-jdbc`, paginación `OFFSET/FETCH`)             |
| Pruebas           | JUnit 5                                                                  |
| Build             | Maven (con wrapper `./mvnw`)                                             |

---

## ▶ Cómo ejecutar

1. **Base de datos:** crear la BD `LaPtiteFranceDB` en SQL Server y ejecutar el script
   [`SQLQuery1.sql`](SQLQuery1.sql) (crea tablas y datos de prueba, incluidos empleados y pedidos).
2. **Credenciales:** revisar [`src/main/resources/application.properties`](src/main/resources/application.properties):
   ```properties
   db.url=jdbc:sqlserver://localhost:1433;databaseName=LaPtiteFranceDB;encrypt=true;trustServerCertificate=true;
   db.user=sa
   db.password=123456789
   ```
3. **Ejecutar la aplicación (un solo ejecutable):** punto de entrada
   [`com.laptitefrance.delivery.Main`](src/main/java/com/laptitefrance/delivery/Main.java).
   Al arrancar levanta, en el mismo proceso, **el servidor web (puerto 8080)** y **la ventana de
   login del asistente**.
   - Desde VSCode: botón **▶ Run** sobre `Main.java`.
   - Por línea de comandos:
     ```bash
     ./mvnw exec:java
     ```
   En la consola aparece `✅ Servidor web del repartidor en http://localhost:8080/`.
4. **Pruebas:**
   ```bash
   ./mvnw test
   ```

> Códigos de empleado de ejemplo (tabla `Empleado`): asistentes `E001`, `E002`, `E003`;
> repartidores `E004`, `E005`.

> ⚠️ En este entorno se usa el **Maven Wrapper** (`./mvnw`). No usar `mvn clean` mientras VSCode está
> abierto: la extensión de Java bloquea `target/` y `clean` falla; basta con compilar sin `clean`.

---

## 🔄 Flujo completo (asistente ↔ repartidor)

```
ASISTENTE (escritorio Swing)                      REPARTIDOR (navegador web)
─────────────────────────────                     ──────────────────────────
1. Inicia sesión (E001)
2. Monitor de Pedidos
3. "Asignar Repartidor" → E004
   (el pedido pasa a EN CAMINO,                    4. Abre http://localhost:8080
    se guarda en la BD)                            5. Escribe su código (E004)
                                                   6. Ve su tabla de pedidos asignados
                                                      (pedido, nombre, dirección,
                                                       método de pago, estado)
                                                   7. "Confirmar entrega"
                                                      → estado ENTREGADO + persistencia BD
8. El Monitor se auto-refresca (cada 3 s)
   y muestra el pedido como ENTREGADO  ◄───────────────  (sincronización vía BD)
```

El **asistente asigna** los pedidos; el **repartidor solo ejecuta** (ve lo suyo y confirma la
entrega). Ambos lados corren en el **mismo proceso** y comparten la base de datos.

---

## 🚚 Vista web del repartidor

Servida por [`ServidorWebRepartidor`](src/main/java/com/laptitefrance/delivery/despacho/ServidorWebRepartidor.java)
(Javalin, puerto 8080). La página ([`src/main/resources/web/`](src/main/resources/web/)) es una tabla
**paginada** a la que el repartidor entra **buscando por su código**.

### Endpoints

| Método | Ruta | Acción |
|--------|------|--------|
| `GET`  | `/` | Sirve la página HTML del repartidor (estáticos del classpath `/web`) |
| `GET`  | `/api/repartidor/{cod}/pedidos?page=1&size=10` | Página de pedidos asignados a ese repartidor: código, nombre del cliente, dirección, método de pago, estado |
| `POST` | `/api/pedidos/{cod}/entregar?repartidor={cod}` | Confirma la entrega (estado → `ENTREGADO`), bajo lock por pedido |

La consulta de pedidos
([`PedidoRepartidorRepositoryPagination`](src/main/java/com/laptitefrance/delivery/repositories/PedidoRepartidorRepositoryPagination.java))
hace un `JOIN` de **Pedido + Cliente + Pago** filtrado por `CodRepartidor`, con paginación
`OFFSET ? ROWS FETCH NEXT ? ROWS ONLY`. El método de pago sale de la tabla `Pago`; el nombre, de
`Cliente`.

### Confirmación de entrega (con concurrencia)

```
Repartidor (web)            Servidor (hilo Javalin)        CentroDespacho            BD
  click "Confirmar" P0001 ─► POST .../entregar ──────────► confirmarEntrega(P0001,E004)
                                                            │ lock(P0001)
                                                            │ valida: existe + es de E004 + EN CAMINO
                                                            │ estado = ENTREGADO; persiste ──────────► update
                                                            │ unlock(P0001)
  200 OK ◄──────────────────────────────────────────────── ResultadoOperacion(OK)
```

Si el pedido no es de ese repartidor → `400 REPARTIDOR_NO_DISPONIBLE`; si no está `EN CAMINO`
(p. ej. ya entregado) → `409 YA_TOMADO`; si no existe → `404`.

---

## 🧵 Programación concurrente

La API comparte proceso con el escritorio: los **hilos del servidor web** (varios repartidores) y el
**hilo de la interfaz** (asistente) pueden tocar pedidos a la vez. La coordinación vive en
[`CentroDespacho`](src/main/java/com/laptitefrance/delivery/despacho/CentroDespacho.java), thread-safe.

| Mecanismo | Dónde | Qué hace / por qué importa |
|-----------|-------|----------------------------|
| `ReentrantLock` por pedido | `confirmarEntrega`, vía `lockDe(codPedido)` (`ConcurrentHashMap.computeIfAbsent`) | Exclusión mutua **fina**: una edición del asistente y la confirmación del repartidor sobre el **mismo** pedido no se pisan; pedidos distintos avanzan en paralelo. La validación y la persistencia ocurren **dentro** del lock. |
| `ConcurrentHashMap` | mapa de locks por pedido y pool de repartidores | Acceso concurrente seguro sin bloquear todo el mapa. |
| `AtomicReference` + `compareAndSet` | [`RepartidorEnLinea`](src/main/java/com/laptitefrance/delivery/despacho/RepartidorEnLinea.java) | Transición de estado atómica sin bloqueos (usado por el modo de competencia/demostración). |
| `javax.swing.Timer` | [`PanelMonitorPedidos`](src/main/java/com/laptitefrance/delivery/views/PanelMonitorPedidos.java) | Auto-refresco del Monitor cada 3 s; el hilo del timer corre en paralelo a la interfaz y refleja las entregas confirmadas desde la web. |
| `ExecutorService` / `BlockingQueue` | despachador y cola en `CentroDespacho` | Infraestructura de asignación automática (productor-consumidor), conservada como demostración de concurrencia. |

**Buenas prácticas aplicadas:** `lock()`/`unlock()` siempre en `try/finally`; persistencia dentro de
la sección crítica (memoria y BD consistentes); reversión del cambio en memoria si falla la
persistencia; inyección del repositorio en `CentroDespacho` para poder probarlo sin BD real.

### Demostración de concurrencia
[`SimuladorConcurrencia`](src/main/java/com/laptitefrance/delivery/despacho/SimuladorConcurrencia.java)
lanza varios hilos compitiendo por el mismo pedido y comprueba que **exactamente uno gana**. Se
ejecuta sin base de datos:
```bash
./mvnw exec:java -Dexec.mainClass=com.laptitefrance.delivery.despacho.SimuladorConcurrencia
```

---

## 📁 Estructura del proyecto

```
src/main/java/com/laptitefrance/delivery/
├── Main.java            → Ejecutable único: arranca el servidor web + el login del asistente
│
├── config/              → Infraestructura técnica
│   └── DBConnection.java         Provee conexiones JDBC (lee application.properties)
│
├── models/              → Entidades del dominio (Pedido, Cliente, Empleado, Producto, Pago, ...)
│
├── dtos/                → Proyecciones para las vistas (campos públicos)
│   ├── PedidoMonitorRow.java     Fila del Monitor del asistente
│   ├── RepartidorMonitorRow.java Repartidor + estado para asignar
│   └── PedidoRepartidorRow.java  Fila de la tabla web del repartidor
│
├── repositories/        → Acceso a datos (DAO / consultas SQL)
│   ├── PedidoRepository.java, ClienteRepository.java, ProductoRepository.java, ...
│   ├── PedidoMonitorRepositoryPagination.java     Paginación del Monitor
│   └── PedidoRepartidorRepositoryPagination.java  Pedidos por repartidor (JOIN cliente+pago)
│
├── controllers/         → Lógica de negocio y validaciones
│   ├── LoginController.java, PedidoController.java, RepartidorMonitorController.java, ...
│
├── despacho/            → API web concurrente
│   ├── ServidorWebRepartidor.java  Servidor Javalin (web + endpoints), punto de arranque del servidor
│   ├── CentroDespacho.java         Servicio thread-safe (lock por pedido, confirmarEntrega)
│   ├── PaginaRepartidor.java       DTO de página (filas + totalPaginas)
│   ├── ResultadoOperacion.java     Resultado de negocio → código HTTP
│   ├── RepartidorEnLinea.java  EstadoRepartidor.java  Despachador.java   (demo de concurrencia)
│   └── SimuladorConcurrencia.java  Demostración: varios repartidores compiten por un pedido
│
├── views/               → Interfaz Swing del asistente
│   ├── LoginView.java  DashboardAsistenteView.java
│   ├── PanelNuevaVenta.java  PanelMonitorPedidos.java (auto-refresco)  PanelClientes.java  PanelInventario.java
│   └── PaginatorPanel.java
│
└── exceptions/          → Jerarquía de excepciones de dominio
    └── DomainException.java → ValidationException, NotFoundException, DuplicateException

src/main/resources/
├── application.properties     Credenciales/URL de la BD
└── web/                       Vista web del repartidor (servida por Javalin)
    ├── index.html  styles.css  app.js
```

---

## 🏛 Arquitectura por capas

```
┌─────────────────────────────────────────────────────────────┐
│  PRESENTACIÓN                                               │
│   • Swing (asistente): LoginView, DashboardAsistenteView…   │
│   • Web (repartidor): index.html + app.js  ⇄  ServidorWebRepartidor │
└───────────────┬─────────────────────────────────────────────┘
                │ llaman a controladores / al CentroDespacho
┌───────────────▼─────────────────────────────────────────────┐
│  LÓGICA: controllers/  +  despacho/CentroDespacho           │
│   Validan, aplican reglas, coordinan concurrencia (locks).  │
└───────────────┬─────────────────────────────────────────────┘
                │ usan repositorios (DAO)
┌───────────────▼─────────────────────────────────────────────┐
│  DATOS: repositories/  (JDBC / PreparedStatement)           │
└───────────────┬─────────────────────────────────────────────┘
                │ DBConnection.getConexion()
┌───────────────▼─────────────────────────────────────────────┐
│  SQL Server (LaPtiteFranceDB)                               │
└─────────────────────────────────────────────────────────────┘
        MODELS y DTOs atraviesan las capas como transporte de datos.
```

**Por qué importa:** cada capa tiene una responsabilidad única; se puede cambiar el SQL sin tocar la
vista, o probar la lógica con un repositorio falso sin BD real.

---

## 🎯 Patrones de diseño

- **MVC + Repository (DAO):** `models` / `views` / `controllers` + `repositories` aíslan datos,
  presentación, lógica y acceso a BD.
- **Inyección de dependencias por constructor:** los controladores y el `CentroDespacho` reciben sus
  dependencias (`IRepositorioBase`), lo que permite inyectar un repositorio falso en pruebas
  (ver [`FakePedidoRepository`](src/test/java/com/laptitefrance/delivery/despacho/FakePedidoRepository.java)).
- **Builder:** [`Pedido.Builder`](src/main/java/com/laptitefrance/delivery/models/Pedido.java) construye
  un pedido paso a paso, evitando constructores gigantes.
- **DTO / Proyección:** `PedidoRepartidorRow`, `PedidoMonitorRow` transportan solo lo que la vista
  necesita.
- **Singleton técnico / utilitario estático:** `DBConnection` centraliza la conexión; los repositorios
  de paginación exponen consultas estáticas.
- **Excepciones de dominio:** jerarquía propia para que la vista distinga validación de "no encontrado".

---

## 🧩 Notas y decisiones

- **Un solo ejecutable:** `Main` levanta el servidor web y el escritorio juntos. El servidor web **no**
  arranca el despachador automático (`centro.iniciar()`), porque en este flujo **el asistente asigna**
  y el repartidor solo confirma; la asignación automática queda como demostración de concurrencia.
- **Sincronización del Monitor:** por **sondeo** (auto-refresco cada 3 s leyendo la BD), no por push.
  Sencillo y desacoplado; la entrega confirmada aparece en pocos segundos.
- **Acceso desde el celular del repartidor:** `http://<IP-del-asistente>:8080` en la red local
  (requiere que el firewall permita el puerto 8080).
- **Look & Feel:** se usa el multiplataforma por defecto a propósito; el L&F del sistema (Windows) no
  pinta el fondo de los `JButton` y dejaba el texto invisible.
- **Seguridad (académica):** credenciales en texto plano y CORS abierto. Aceptable para un ejercicio
  local; **no usar así en producción**.

---

*Proyecto académico — Programación Orientada a Objetos II (POO2).*
