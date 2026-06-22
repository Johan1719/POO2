# 🥖 La P'tite France — Sistema de Delivery (POO2)

Aplicación de escritorio (punto de venta + monitor de pedidos) para una panadería/restaurante
con servicio de delivery. Desarrollada en **Java 21** con interfaz **Swing** y persistencia en
y está construido sobre una **arquitectura por capas** que aplica varios **patrones de diseño**.

---

## 📑 Tabla de contenido
1. [Tecnologías](#-tecnologías)
2. [Cómo ejecutar](#-cómo-ejecutar)
3. [Estructura del proyecto](#-estructura-del-proyecto)
4. [Arquitectura por capas](#-arquitectura-por-capas)
5. [Flujo de una operación (ejemplo)](#-flujo-de-una-operación-ejemplo)
6. [API REST de Delivery concurrente](#-api-rest-de-delivery-concurrente)
7. [Programación concurrente: mecanismos y buenas prácticas](#-programación-concurrente-mecanismos-y-buenas-prácticas)
8. [Patrones de diseño y su importancia](#-patrones-de-diseño-y-su-importancia)
9. [Correcciones (interfaz)](#-correcciones-interfaz)
10. [Decisiones de diseño relevantes](#-decisiones-de-diseño-relevantes)

---

## 🛠 Tecnologías

| Componente        | Detalle                                              |
|-------------------|------------------------------------------------------|
| Lenguaje          | Java 21                                              |
| UI                | Java Swing (`javax.swing`)                           |
| Base de datos     | SQL Server (driver `mssql-jdbc`)                     |
| API REST concurrente | [Javalin](https://javalin.io/) 6 + Jackson           |
| Concurrencia      | `java.util.concurrent` (ExecutorService, BlockingQueue, ConcurrentHashMap, ReentrantLock, AtomicReference) |
| Pruebas           | JUnit 5                                              |
| Logging           | SLF4J Simple                                         |
| Build             | Maven                                                |

---

## ▶ Cómo ejecutar

1. **Base de datos:** crear la BD `LaPtiteFranceDB` en SQL Server y ejecutar el script
   [`SQLQuery1.sql`](SQLQuery1.sql).
2. **Credenciales:** revisar/editar [`src/main/resources/application.properties`](src/main/resources/application.properties):
   ```properties
   db.url=jdbc:sqlserver://localhost:1433;databaseName=LaPtiteFranceDB;encrypt=true;trustServerCertificate=true;
   db.user=sa
   db.password=123456789
   ```
3. **Compilar:**
   ```bash
   mvn clean package
   ```
4. **Ejecutar la aplicación de escritorio** (punto de entrada):
   `com.laptitefrance.delivery.Main` → método `main` (abre la ventana de login).
   Usa el Look & Feel multiplataforma por defecto (ver "Correcciones").
5. **Servidor REST de delivery concurrente** (proceso aparte):
   `com.laptitefrance.delivery.despacho.ApiDeliveryServer` → `http://localhost:8080/api`.
   Por línea de comandos:
   ```bash
   mvn exec:java -Dexec.mainClass=com.laptitefrance.delivery.despacho.ApiDeliveryServer
   ```
6. **Demostración de concurrencia** (sin BD):
   `com.laptitefrance.delivery.despacho.SimuladorConcurrencia` → imprime que solo un
   repartidor gana el mismo pedido.

> Código de empleado de ejemplo para el login: debe existir en la tabla `Empleado` (p. ej. `E001`).

---

## 📁 Estructura del proyecto

```
src/main/java/com/laptitefrance/delivery/
│
├── config/            → Configuración e infraestructura técnica
│   └── DBConnection.java        Provee conexiones JDBC a SQL Server (lee application.properties)
│
├── models/            → Entidades del dominio (POJOs: clientes, pedidos, productos...)
│   ├── Pedido.java              Entidad central + Builder
│   ├── Cliente.java  Empleado.java  Producto.java  Pago.java  Tarifa.java ...
│
├── repositories/      → Capa de acceso a datos (DAO). Habla SQL, devuelve objetos del dominio
│   ├── IRepositorioBase.java    Contrato genérico CRUD <T, ID>
│   ├── PedidoRepository.java  ClienteRepository.java  ProductoRepository.java ...
│
├── controllers/       → Lógica de negocio y validaciones. Orquesta repositorios
│   ├── LoginController.java     Autenticación por código de empleado
│   ├── PedidoController.java    Generar / listar / filtrar / asignar repartidor / cambiar estado
│   └── ClienteController.java  ProductoController.java  PagoController.java  TarifaController.java
│
├── despacho/          → API REST concurrente de delivery (puerto 8080)
│   ├── ApiDeliveryServer.java   Servidor Javalin + endpoints (main)
│   ├── CentroDespacho.java      Núcleo concurrente (cola, locks, pool de repartidores)
│   ├── Despachador.java         Hilo de asignación automática (consumidor)
│   ├── RepartidorEnLinea.java   Estado en memoria del repartidor (ocupar/liberar atómico)
│   ├── EstadoRepartidor.java    enum LIBRE/OCUPADO
│   ├── ResultadoOperacion.java  Resultado de negocio → código HTTP
│   └── SimuladorConcurrencia.java  Demo: N repartidores compiten por un pedido
│
├── views/             → Interfaz gráfica Swing (lo que ve el usuario)
│   ├── LoginView.java           Ventana de acceso
│   ├── DashboardAsistenteView.java   Ventana principal con pestañas
│   ├── PanelNuevaVenta.java     Carrito y generación de pedidos
│   └── PanelMonitorPedidos.java Monitoreo y cambio de estado de pedidos
│
└── exceptions/        → Jerarquía de excepciones de dominio
    └── DomainException.java     (base) → ValidationException, NotFoundException, DuplicateException
```

---

## 🏛 Arquitectura por capas

El proyecto sigue el patrón **MVC** reforzado con la **capa de repositorios (DAO)**. La regla de
oro es que **las dependencias apuntan hacia abajo**: una capa solo conoce a la que tiene
inmediatamente debajo, nunca al revés.

```
┌─────────────────────────────────────────────────────────────┐
│  VIEW  (Swing)        LoginView, PanelNuevaVenta, ...        │  ← Interactúa con el usuario
│  "Vista tonta": solo dibuja y delega. No contiene reglas.    │
└───────────────┬─────────────────────────────────────────────┘
                │ llama a métodos del controlador
┌───────────────▼─────────────────────────────────────────────┐
│  CONTROLLER          PedidoController, ClienteController...  │  ← Reglas de negocio + validación
│  Valida datos, arma entidades, decide el flujo.             │
└───────────────┬─────────────────────────────────────────────┘
                │ usa la interfaz IRepositorioBase
┌───────────────▼─────────────────────────────────────────────┐
│  REPOSITORY (DAO)    PedidoRepository, ClienteRepository... │  ← Acceso a datos (SQL/JDBC)
│  Traduce entre objetos del dominio y filas de la BD.        │
└───────────────┬─────────────────────────────────────────────┘
                │ DBConnection.getConexion()
┌───────────────▼─────────────────────────────────────────────┐
│  BASE DE DATOS       SQL Server (LaPtiteFranceDB)           │
└─────────────────────────────────────────────────────────────┘

         MODELS (Pedido, Cliente, ...) atraviesan todas las capas
              como objetos de transporte de datos.
```

**¿Por qué separar en capas?**
- **Mantenibilidad:** si cambia el SQL, solo se toca el repositorio; la vista no se entera.
- **Testeabilidad:** se puede probar un controlador con un repositorio falso (mock) sin BD real.
- **Reemplazabilidad:** se podría cambiar Swing por una web, o SQL Server por otra BD, con impacto mínimo.
- **Claridad:** cada archivo tiene una única responsabilidad (Principio de Responsabilidad Única, SRP).

---

## 🔄 Flujo de una operación (ejemplo)

**Generar un pedido desde "Nueva Venta":**

```
Usuario pulsa "Generar Pedido"
   │
   ▼
PanelNuevaVenta (VIEW)            recoge cliente, carrito, dirección, tarifa, pago
   │  pedidoController.generarPedido(...)
   ▼
PedidoController (CONTROLLER)     1) valida datos  2) ensambla el objeto Pedido
   │  pedidoRepository.insert(pedido)
   ▼
PedidoRepository (DAO)            traduce el Pedido a un INSERT con PreparedStatement
   │  DBConnection.getConexion()
   ▼
SQL Server                       guarda la fila en la tabla Pedido
```

El controlador, además, conoce **quién** opera la caja (`codCajeroActivo`, inyectado al
iniciar sesión) y registra acciones sensibles en `AuditoriaLog`.

---

## 🚚 API REST de Delivery concurrente

Servidor **Javalin** independiente (puerto 8080) que coordina el reparto de pedidos con
**programación concurrente real**. Hay dos escenarios:

- **Asignación automática** (productor-consumidor): un hilo `Despachador` toma pedidos de una
  cola bloqueante y los asigna a un repartidor libre.
- **Competencia manual**: varios repartidores intentan *tomar* el mismo pedido; solo uno gana.

### Endpoints

| Método | Ruta | Acción |
|---|---|---|
| `POST` | `/api/repartidores/{cod}/conectar` | El repartidor entra al pool de disponibles |
| `POST` | `/api/repartidores/{cod}/desconectar` | Sale del pool |
| `GET`  | `/api/repartidores/disponibles` | Lista el pool y su estado (LIBRE/OCUPADO) |
| `GET`  | `/api/pedidos/pendientes` | Pedidos en cola de asignación |
| `POST` | `/api/pedidos/{cod}/tomar?repartidor={cod}` | Competencia manual (el primero gana) |
| `POST` | `/api/pedidos/{cod}/entregar?repartidor={cod}` | Marca ENTREGADO y libera al repartidor |
| `GET`  | `/api/despacho/estado` | Métricas: pendientes, conectados, libres |

### Flujo de asignación automática

```
Pedido EN ESPERA ──put()──> [ BlockingQueue ] ──take()──> Despachador (hilo de fondo)
                                                              │ busca repartidor LIBRE (CAS)
                                                              ▼
                                            lock(pedido) → EN CAMINO + persistir → unlock
```

### Flujo de competencia manual

```
Repartidor A ─┐
Repartidor B ─┼─ POST /pedidos/P1/tomar ─→ lock(P1) ─→ ¿sigue EN ESPERA?
Repartidor C ─┘                                          │ sí → uno gana: EN CAMINO (200)
                                                         │ no → los demás: YA_TOMADO (409)
```

---

## 🧵 Programación concurrente: mecanismos y buenas prácticas

| Mecanismo | Dónde | Qué hace / por qué importa |
|---|---|---|
| `BlockingQueue` (`LinkedBlockingQueue`) | `CentroDespacho.colaPendientes` | Cola productor-consumidor: el `Despachador` se bloquea sin gastar CPU cuando no hay pedidos (`take()`) y despierta solo al llegar uno (`put()`). |
| `ExecutorService` (`newSingleThreadExecutor`) | hilo `Despachador` | Gestiona el ciclo de vida del hilo de fondo y permite apagado ordenado, mejor que crear `Thread` a mano. |
| `ScheduledExecutorService` | sondeo de pedidos | Cada 5 s busca nuevos pedidos `EN ESPERA` en la BD y los encola, manteniendo la API desacoplada del dashboard. |
| `ConcurrentHashMap` | pool de repartidores y mapa de locks | Acceso concurrente seguro; `computeIfAbsent` crea el lock de un pedido de forma atómica. |
| `ReentrantLock` (uno por pedido) | sección crítica de tomar/asignar | Exclusión mutua **fina**: dos pedidos distintos avanzan en paralelo, pero un mismo pedido nunca se asigna dos veces. |
| `AtomicReference` + `compareAndSet` | `RepartidorEnLinea` | Transición LIBRE→OCUPADO atómica sin bloqueos: si dos hilos intentan ocupar el mismo repartidor, solo uno gana. |

**Buenas prácticas aplicadas:**
- Bloqueo de granularidad fina (lock por pedido, no global) → mayor paralelismo.
- `lock()`/`unlock()` siempre en `try/finally`.
- Persistencia dentro de la sección crítica → memoria y BD consistentes.
- Apagado ordenado de los executors (`shutdownNow`) vía *shutdown hook*.
- Inyección del repositorio en `CentroDespacho` → testeable sin BD real (ver `CentroDespachoTest`).
- Sin estado mutable compartido fuera de estructuras thread-safe.

---

## 🎯 Patrones de diseño y su importancia

Este proyecto es, sobre todo, una demostración práctica de patrones de diseño. Estos son los
que se aplican y **por qué importan**:

### 1. MVC (Model–View–Controller)
- **Dónde:** paquetes `models`, `views`, `controllers`.
- **Qué hace:** separa los datos (Model), la presentación (View) y la lógica (Controller).
- **Por qué importa:** evita el "código espagueti" donde la UI mezcla SQL, validaciones y dibujo.
  La vista es "tonta": solo muestra y delega. Esto hace el sistema entendible y modificable.

### 2. Repository / DAO (Data Access Object)
- **Dónde:** paquete `repositories`, contrato [`IRepositorioBase`](src/main/java/com/laptitefrance/delivery/repositories/IRepositorioBase.java).
- **Qué hace:** encapsula todo el acceso a la base de datos detrás de métodos simples
  (`insert`, `findById`, `findAll`, `update`, `deleteById`).
- **Por qué importa:** el resto del código manipula **objetos Java**, no sentencias SQL. Si mañana
  se cambia de SQL Server a PostgreSQL, solo cambian los repositorios.

### 3. Programación contra interfaces + Inyección de Dependencias (DI)
- **Dónde:** los controladores reciben `IRepositorioBase<...>` por **constructor**
  (ver [`PedidoController`](src/main/java/com/laptitefrance/delivery/controllers/PedidoController.java)).
- **Qué hace:** el controlador depende de la *abstracción* (interfaz), no de la implementación concreta.
- **Por qué importa:** permite inyectar un repositorio real en producción y un *mock* en pruebas.
  También es como `DashboardAsistenteView` propaga **un mismo** `PedidoController` (con el cajero ya
  fijado) a varias vistas, garantizando consistencia.

### 4. Builder
- **Dónde:** clase interna [`Pedido.Builder`](src/main/java/com/laptitefrance/delivery/models/Pedido.java).
- **Qué hace:** construye un `Pedido` paso a paso de forma legible y encadenada.
- **Por qué importa:** un `Pedido` tiene muchos campos opcionales (repartidor, tiempos, etc.); el
  Builder evita constructores gigantes y poco claros con muchos parámetros.

### 5. Observer (Observador)
- **Dónde:** paquete `events` (`PedidoEstadoObservable`, `ObservadorPedidoEstado`, `ConsolaRepartidorObservador`).
- **Qué hace:** cuando un pedido cambia de estado, notifica automáticamente a todos los interesados
  (p. ej. la consola del repartidor) sin que el emisor los conozca directamente.
- **Por qué importa:** desacopla "quién avisa" de "quién reacciona". Se pueden añadir nuevos
  observadores (notificación push, dashboard, sonido...) sin tocar el código que genera el evento.

### 6. Singleton técnico / utilitario estático
- **Dónde:** [`DBConnection`](src/main/java/com/laptitefrance/delivery/config/DBConnection.java) y
  [`AuditoriaLog`](src/main/java/com/laptitefrance/delivery/audit/AuditoriaLog.java).
- **Qué hace:** centralizan un recurso único: la lectura de credenciales / entrega de conexiones
  y la escritura del archivo de auditoría (con método `synchronized` para ser *thread-safe*).
- **Por qué importa:** se lee `application.properties` una sola vez (bloque `static`) y se evita
  duplicar la lógica de conexión por todo el código.

### 7. Excepciones de dominio (jerarquía propia)
- **Dónde:** paquete `exceptions` (`DomainException` → `ValidationException`, `NotFoundException`, `DuplicateException`).
- **Qué hace:** distingue tipos de error de negocio para que la vista reaccione distinto
  (un aviso amarillo para validación, un error rojo para "no encontrado").
- **Por qué importa:** comunica intención. La vista hace `catch (ValidationException)` vs
  `catch (NotFoundException)` y muestra el mensaje adecuado, sin exponer detalles técnicos.

---

## 🩹 Correcciones (interfaz)

- **Punto de entrada unificado:** se añadió `com.laptitefrance.delivery.Main` como arranque
  oficial de la app de escritorio (antes el `main` vivía dentro de `LoginView`).
- **Botones sin texto (Look & Feel):** al usar el *Look & Feel* del sistema (Windows), los
  `JButton` con `setBackground(color)` + `setForeground(Color.WHITE)` no pintaban su fondo de
  color y el texto blanco quedaba invisible (solo se veían los bordes). La solución fue usar el
  **Look & Feel multiplataforma por defecto** (Metal), que sí respeta esos colores. Por eso `Main`
  no fuerza el L&F del sistema.

---

## 🧩 Decisiones de diseño relevantes

- **Asincronía con `CompletableFuture`:** asignar repartidor (`PedidoController.asignarRepartidor`)
  corre en segundo plano para no congelar la interfaz Swing mientras se actualiza la BD.
- **Conexión "fresca" por operación:** `DBConnection.getConexion()` devuelve una conexión nueva
  usada con *try-with-resources*, que la cierra sola al terminar (evita fugas de conexiones).
- **`PreparedStatement` siempre:** todas las consultas usan parámetros `?`, lo que previene
  **inyección SQL** y maneja correctamente fechas/nulos.
- **Auditoría thread-safe:** `AuditoriaLog.registrarAccion` es `synchronized` porque puede ser
  llamada desde hilos del `CompletableFuture` simultáneamente.

> ⚠️ **Nota de seguridad (académica):** las credenciales de BD están en texto plano en
> `application.properties` y `ApiRepartidor` habilita CORS para cualquier origen. Es aceptable para
> un ejercicio local, pero **no debe usarse así en producción**.

---

*Proyecto académico — Programación Orientada a Objetos II (POO2).*
