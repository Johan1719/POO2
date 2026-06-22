# Diseño — API REST de Delivery con programación concurrente

**Fecha:** 2026-06-21
**Proyecto:** La P'tite France — Sistema de Delivery (POO2)
**Rama:** Project_v2

---

## 1. Objetivo

Construir una **API REST** (Javalin) que sirva a los repartidores y que use **programación
concurrente real en Java** para coordinar el reparto de pedidos. Dos escenarios concurrentes:

1. **Asignación automática** — un proceso en segundo plano reparte los pedidos `EN ESPERA` a los
   repartidores disponibles (patrón productor-consumidor).
2. **Competencia manual por pedidos** — varios repartidores intentan *tomar* el mismo pedido a la
   vez; solo uno gana (exclusión mutua / condición de carrera).

El estado compartido se maneja **en memoria con estructuras thread-safe** y se persiste a
SQL Server mediante el `PedidoRepository` existente.

Además se documenta todo en el `README.md` (arreglo de bugs visuales + API), explicando la lógica
y las buenas prácticas, con el estilo *qué hace / por qué importa* ya presente en el README.

---

## 2. Alcance

**Incluye:**
- Paquete nuevo `com.laptitefrance.delivery.despacho` con la lógica concurrente.
- Servidor REST independiente (`ApiDeliveryServer`, puerto 8080) que reemplaza al actual
  `ApiRepartidor` (esqueleto con datos simulados).
- Carga inicial de pedidos `EN ESPERA` desde la BD y persistencia de cada transición.
- Clase `SimuladorConcurrencia` para demostrar en vivo la competencia por un pedido.
- Documentación completa en `README.md`.

**No incluye (YAGNI):**
- Autenticación/tokens en la API (sigue siendo ejercicio académico local).
- Interfaz gráfica para el repartidor (se consume con `curl`/navegador/Postman).
- Persistencia del pool de repartidores conectados (vive solo en memoria del servidor).
- Cambios en el esquema de la BD.

---

## 3. Contexto del código existente (lo que se reutiliza)

- **Estados de un pedido:** `EN ESPERA` → `EN CAMINO` → `ENTREGADO`
  (ver `PedidoController` y `SQLQuery1.sql`).
- **`PedidoRepository`** ya ofrece `findAll`, `findById`, `update` con `PreparedStatement`.
- **`Pedido`** tiene `estado`, `codRepartidor`, `tiempoEntEstimado`, `tiempoEntReal`, `horaEnvio`.
- **Repartidores sembrados:** `E004`, `E005` (tabla `Repartidor`).
- **Javalin 6.1.3 + Jackson** ya están como dependencias en `pom.xml`.

---

## 4. Arquitectura

Proceso **independiente** del dashboard Swing. Encaja en la arquitectura por capas añadiendo un
paquete de coordinación concurrente que se apoya en la capa de repositorios existente.

```
com.laptitefrance.delivery.despacho/        ← NUEVO
├── CentroDespacho.java        Singleton thread-safe: estado compartido + operaciones
├── Despachador.java           Runnable: hilo de asignación automática (consumidor de la cola)
├── RepartidorEnLinea.java     Estado en memoria de un repartidor conectado (LIBRE/OCUPADO)
├── EstadoRepartidor.java      enum { LIBRE, OCUPADO }
├── ResultadoOperacion.java    Resultado de tomar/entregar (enum Tipo + mensaje) → mapea a HTTP
├── ApiDeliveryServer.java     Endpoints REST Javalin + main (reemplaza ApiRepartidor)
└── SimuladorConcurrencia.java main de demostración: N hilos compiten por el mismo pedido
```

Dependencias: `despacho` → `repositories` (`PedidoRepository`) → `config` (`DBConnection`) → BD.
Reutiliza `models.Pedido`. No introduce dependencias hacia las capas superiores (views/controllers).

---

## 5. Componentes en detalle

### 5.1 `EstadoRepartidor` (enum)
```
LIBRE, OCUPADO
```

### 5.2 `RepartidorEnLinea`
POJO de estado en memoria. Campos:
- `String codRepartidor`
- `volatile EstadoRepartidor estado`
- `volatile String codPedidoActual` (null si LIBRE)

Métodos simples de transición. El acceso se coordina desde `CentroDespacho`; `volatile` garantiza
visibilidad entre hilos para lecturas/escrituras simples.

### 5.3 `ResultadoOperacion`
Representa el resultado de una operación de negocio para mapearlo a un código HTTP.
```
enum Tipo { OK, YA_TOMADO, NO_ENCONTRADO, REPARTIDOR_NO_DISPONIBLE, ERROR_INTERNO }
- Tipo tipo
- String mensaje
- (opcional) Object datos
```
Mapeo a HTTP en la capa Javalin: OK→200, YA_TOMADO→409, NO_ENCONTRADO→404,
REPARTIDOR_NO_DISPONIBLE→400, ERROR_INTERNO→500.

### 5.4 `CentroDespacho` (singleton thread-safe) — el núcleo

Estado compartido:
| Campo | Tipo | Propósito |
|---|---|---|
| `colaPendientes` | `BlockingQueue<String>` (`LinkedBlockingQueue`) | Códigos de pedidos `EN ESPERA` esperando asignación automática (productor-consumidor) |
| `repartidoresDisponibles` | `ConcurrentHashMap<String, RepartidorEnLinea>` | Pool de repartidores conectados |
| `locksPorPedido` | `ConcurrentHashMap<String, ReentrantLock>` | Un candado por pedido para la competencia manual |
| `despachadorPool` | `ExecutorService` (`newSingleThreadExecutor`) | Ejecuta el `Despachador` en segundo plano |
| `sondeoPool` | `ScheduledExecutorService` (`newSingleThreadScheduledExecutor`) | Cada N segundos busca nuevos pedidos `EN ESPERA` en la BD y los encola (ver §11) |
| `codigosEncolados` | `Set<String>` (`ConcurrentHashMap.newKeySet()`) | Evita encolar dos veces el mismo pedido entre sondeos |
| `pedidoRepository` | `IRepositorioBase<Pedido,String>` | Persistencia (inyectable para pruebas) |

Operaciones públicas (todas thread-safe):
- `void iniciar()` — carga pedidos `EN ESPERA` desde BD a la cola, arranca el `Despachador` y
  programa el sondeo periódico (`sondeoPool`) que encola nuevos pedidos `EN ESPERA`.
- `void detener()` — apaga ambos executors ordenadamente (`shutdown` + `awaitTermination`).
- `ResultadoOperacion conectarRepartidor(String cod)` — agrega `RepartidorEnLinea` al pool (LIBRE).
- `ResultadoOperacion desconectarRepartidor(String cod)` — lo quita del pool.
- `ResultadoOperacion tomarPedido(String codPedido, String codRepartidor)` — **competencia manual**.
- `ResultadoOperacion entregarPedido(String codPedido, String codRepartidor)` — marca `ENTREGADO`,
  libera al repartidor y, si quedó libre, el `Despachador` podrá usarlo para la cola.
- `void encolarPedido(String codPedido)` — `put` en la cola (lo usa la carga inicial y se puede
  exponer para nuevos pedidos).
- `List<...> listarPendientes()` / `listarDisponibles()` / `Map estadoDespacho()` — lecturas.

Helper interno:
- `ReentrantLock lockDe(String codPedido)` → `locksPorPedido.computeIfAbsent(cod, k -> new ReentrantLock())`.

#### Algoritmo de `tomarPedido` (competencia manual)
```
1. Validar que el repartidor está conectado y LIBRE → si no, REPARTIDOR_NO_DISPONIBLE.
2. lock = lockDe(codPedido); lock.lock();
   try {
     3. pedido = pedidoRepository.findById(cod) → si vacío, NO_ENCONTRADO.
     4. if (!pedido.estado == "EN ESPERA") return YA_TOMADO;   // alguien ganó antes
     5. pedido.setEstado("EN CAMINO");
        pedido.setCodRepartidor(codRepartidor);
        pedido.setHoraEnvio(now);
        pedidoRepository.update(pedido);                        // persistencia dentro del lock
     6. marcar RepartidorEnLinea OCUPADO con codPedidoActual = cod;
     7. return OK;
   } finally { lock.unlock(); }
```
La sección crítica (validar estado + persistir) está protegida por el `ReentrantLock` del pedido,
garantizando que **solo un repartidor** complete la transición; el resto ve `YA_TOMADO`.

### 5.5 `Despachador` (Runnable) — asignación automática
Bucle del hilo de fondo:
```
while (!Thread.currentThread().isInterrupted()) {
  codPedido = colaPendientes.take();          // se BLOQUEA si la cola está vacía
  repartidor = buscarRepartidorLibre();        // primer LIBRE del ConcurrentHashMap
  if (repartidor == null) {
      colaPendientes.put(codPedido);           // reencola al final
      Thread.sleep(PAUSA);                     // espera breve para no hacer busy-spin
      continue;
  }
  resultado = centro.asignarAutomatico(codPedido, repartidor); // misma sección crítica con lock
  if (resultado != OK) { /* log; el pedido ya no estaba EN ESPERA, se descarta */ }
}
```
`asignarAutomatico` reutiliza el mismo patrón de lock por pedido que `tomarPedido`, de modo que la
asignación automática y la competencia manual **no pueden pisarse** entre sí.

### 5.6 `ApiDeliveryServer` — capa REST (Javalin) + `main`
- Configura Javalin (puerto 8080, CORS abierto como el actual) y un `CentroDespacho`.
- Llama a `centro.iniciar()` al arrancar y registra un shutdown hook que llama `centro.detener()`.
- Cada endpoint delega en el `CentroDespacho` y traduce `ResultadoOperacion` → HTTP + JSON.

### 5.7 `SimuladorConcurrencia` — demostración (opcional, incluida)
`main` independiente que:
1. Asume el servidor corriendo (o instancia un `CentroDespacho` en memoria con repo de prueba).
2. Lanza N hilos (`ExecutorService` + `CountDownLatch` para soltarlos a la vez) que llaman a
   `tomarPedido` sobre el **mismo** `codPedido`.
3. Imprime el resultado de cada hilo: exactamente **uno** obtiene `OK`, los demás `YA_TOMADO`.
Sirve para evidenciar en la sustentación que la sincronización funciona.

---

## 6. Endpoints REST

| Método | Ruta | Acción | Respuestas |
|---|---|---|---|
| `POST` | `/api/repartidores/{cod}/conectar` | Entra al pool (LIBRE) | 200 / 404 |
| `POST` | `/api/repartidores/{cod}/desconectar` | Sale del pool | 200 / 404 |
| `GET` | `/api/repartidores/disponibles` | Lista pool y estado | 200 |
| `GET` | `/api/pedidos/pendientes` | Pedidos `EN ESPERA` | 200 |
| `POST` | `/api/pedidos/{cod}/tomar?repartidor={cod}` | Competencia manual | 200 / 409 / 404 / 400 |
| `POST` | `/api/pedidos/{cod}/entregar?repartidor={cod}` | Marca `ENTREGADO`, libera | 200 / 404 / 400 |
| `GET` | `/api/despacho/estado` | Métricas de despacho | 200 |

Formato de respuesta JSON uniforme: `{ "tipo": "...", "mensaje": "...", "datos": ... }`.

---

## 7. Mecanismos de concurrencia y por qué se usan

| Mecanismo | Dónde | Por qué |
|---|---|---|
| `BlockingQueue` (`LinkedBlockingQueue`) | `colaPendientes` | Productor-consumidor: el `Despachador` se bloquea sin consumir CPU cuando no hay pedidos (`take()`), y se despierta solo al llegar uno (`put()`). |
| `ExecutorService` (`newSingleThreadExecutor`) | `despachadorPool` | Gestiona el ciclo de vida del hilo de fondo y permite apagado ordenado, mejor que crear `Thread` a mano. |
| `ConcurrentHashMap` | pool de repartidores y mapa de locks | Lecturas/escrituras concurrentes seguras sin bloquear todo el mapa; `computeIfAbsent` crea el lock de un pedido de forma atómica. |
| `ReentrantLock` (uno por pedido) | sección crítica de tomar/asignar | Exclusión mutua **fina**: dos pedidos distintos se procesan en paralelo, pero un mismo pedido nunca se asigna dos veces. |
| `volatile` | campos de `RepartidorEnLinea` | Garantiza visibilidad del estado del repartidor entre hilos. |
| `CountDownLatch` | `SimuladorConcurrencia` | Suelta todos los hilos a la vez para forzar la condición de carrera de forma reproducible. |

**Buenas prácticas aplicadas:**
- Bloqueo de granularidad fina (lock por pedido, no un lock global) → mayor paralelismo.
- `lock()`/`unlock()` siempre en `try/finally`.
- Persistencia dentro de la sección crítica para mantener memoria y BD consistentes.
- Apagado ordenado del `ExecutorService` (`shutdown` + `awaitTermination`) en un shutdown hook.
- Inyección del repositorio en `CentroDespacho` para poder probar sin BD real.
- Sin estado mutable compartido fuera de las estructuras thread-safe.

---

## 8. Manejo de errores

| Situación | `ResultadoOperacion.Tipo` | HTTP |
|---|---|---|
| Operación exitosa | `OK` | 200 |
| Pedido ya tomado / no estaba `EN ESPERA` | `YA_TOMADO` | 409 |
| Pedido o repartidor inexistente | `NO_ENCONTRADO` | 404 |
| Repartidor no conectado u ocupado | `REPARTIDOR_NO_DISPONIBLE` | 400 |
| Fallo de BD u otro error | `ERROR_INTERNO` | 500 |

En fallo de persistencia se revierte el cambio en memoria (no se marca al repartidor OCUPADO si el
`update` lanzó excepción) y se devuelve `ERROR_INTERNO`.

---

## 9. Documentación en el README

Cambios a realizar en `README.md`:
1. **Tecnologías:** actualizar la fila de API REST (de "parcial" a "concurrente").
2. **Cómo ejecutar:** añadir arranque de `ApiDeliveryServer` y ejemplos `curl` de cada endpoint,
   incluyendo el flujo conectar → tomar/entregar.
3. **Nueva sección "API REST de Delivery concurrente":** tabla de endpoints + dos diagramas ASCII
   (flujo de asignación automática y flujo de competencia manual).
4. **Nueva sección "Programación concurrente: mecanismos y buenas prácticas":** la tabla de la
   sección 7 con el estilo *qué hace / por qué importa*.
5. **Nueva sección "Correcciones (bugs visuales)":** explica el problema del Look & Feel del sistema
   (los `JButton` con `setBackground`+`setForeground(WHITE)` perdían el texto) y el nuevo `Main` como
   punto de entrada con L&F multiplataforma.
6. **Estructura del proyecto:** añadir el paquete `despacho/`.

---

## 10. Pruebas / verificación

- **Compilación:** `mvn clean package` (o build automático de la extensión de Java en VSCode).
- **Manual:** arrancar `ApiDeliveryServer`, conectar `E004`/`E005`, crear pedidos desde el dashboard
  y verificar asignación automática (`GET /api/despacho/estado`).
- **Concurrencia:** ejecutar `SimuladorConcurrencia` → exactamente un `OK`, el resto `YA_TOMADO`.
- (Opcional) prueba unitaria de `CentroDespacho.tomarPedido` con un `PedidoRepository` falso
  (mock en memoria) lanzando varios hilos contra el mismo pedido.

---

## 11. Riesgos y notas

- **La API y el dashboard son procesos separados:** no comparten memoria. El pool de repartidores
  conectados vive solo en el servidor REST; si se reinicia, se vacía (aceptable, documentado).
- **Carga inicial vs. nuevos pedidos:** el `Despachador` reparte lo que esté en la cola al arrancar;
  los pedidos creados *después* desde el dashboard requieren un sondeo periódico a la BD o un
  endpoint para encolarlos. Para el alcance académico se hace un **sondeo periódico** (un
  `ScheduledExecutorService` que cada N segundos busca nuevos `EN ESPERA` y los encola), documentado
  como tal. *(Decisión: incluido en el alcance; mantiene la demo realista sin acoplar los procesos.)*
- **Nota de seguridad académica:** CORS abierto y credenciales en texto plano, como ya advierte el
  README; no apto para producción.

---

## 12. Decisiones de diseño confirmadas

Estas tres decisiones fueron revisadas y aprobadas explícitamente. Se documentan aquí con la
alternativa descartada y el motivo, para que queden trazables en la sustentación.

### 12.1 Visibilidad de pedidos nuevos → sondeo periódico (`ScheduledExecutorService`)
- **Decisión:** el `CentroDespacho` programa un `ScheduledExecutorService` que cada **N segundos**
  (configurable, valor inicial 5 s) consulta la BD por pedidos en estado `EN ESPERA` y encola los
  que aún no estén encolados (control con el `Set` `codigosEncolados`).
- **Por qué:** la API REST y el dashboard Swing son **procesos separados** que no comparten memoria.
  El dashboard inserta el pedido en SQL Server; el sondeo es lo que permite que la asignación
  automática "vea" esos pedidos creados *después* de arrancar el servidor, sin acoplar ambos procesos.
- **Alternativa descartada:** exponer `POST /api/pedidos/{cod}/encolar` para que el dashboard avise
  al crear un pedido. Se descartó porque **acopla** el dashboard a la API (tendría que conocer su URL
  y manejar su caída), mientras que el sondeo mantiene los procesos independientes. Coste asumido:
  un retardo máximo de N segundos entre crear el pedido y encolarlo (irrelevante para la demo).
- **Idempotencia:** el `Set<String> codigosEncolados` (un `ConcurrentHashMap.newKeySet()`) evita
  encolar dos veces el mismo pedido entre sondeos consecutivos; un código se retira del set cuando el
  pedido deja de estar `EN ESPERA` (al ser tomado/asignado).

### 12.2 Granularidad del bloqueo → un `ReentrantLock` por pedido
- **Decisión:** la sección crítica de tomar/asignar se protege con un candado **por `codPedido`**
  (`locksPorPedido.computeIfAbsent(cod, k -> new ReentrantLock())`), no con un único lock global.
- **Por qué:** es el corazón de la solución a la **condición de carrera** y maximiza el paralelismo:
  dos pedidos distintos se procesan a la vez, pero un mismo pedido nunca se asigna dos veces. Tanto la
  competencia manual (`tomarPedido`) como la asignación automática (`asignarAutomatico`) usan el
  **mismo** lock del pedido, por lo que no pueden pisarse entre sí.
- **Alternativa descartada:** un único `synchronized`/lock global sobre todo el `CentroDespacho`.
  Se descartó porque **serializa** todas las operaciones (un solo pedido a la vez en todo el sistema),
  desperdiciando la concurrencia que justamente se quiere demostrar.
- **Buenas prácticas:** `lock()`/`unlock()` siempre en `try/finally`; la persistencia
  (`pedidoRepository.update`) ocurre **dentro** del lock para mantener memoria y BD consistentes.

### 12.3 `ApiDeliveryServer` reemplaza a `ApiRepartidor`
- **Decisión:** el nuevo `ApiDeliveryServer` (en el paquete `despacho`) **sustituye** al actual
  `controllers/ApiRepartidor`, que es solo un esqueleto con datos simulados (`datosSimulados`) y un
  único endpoint de prueba. Se elimina `ApiRepartidor`.
- **Por qué:** mantener dos servidores Javalin sería confuso y redundante; el endpoint simulado ya no
  aporta valor frente a los endpoints reales. La referencia a `ApiRepartidor` en el README se
  actualiza al nuevo server.
- **Alternativa descartada:** conservar `ApiRepartidor` intacto y crear el server aparte. Se descartó
  para evitar código muerto y dos puntos de entrada REST que compiten por el puerto 8080.
