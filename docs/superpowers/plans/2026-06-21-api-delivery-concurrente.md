# API de Delivery Concurrente — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construir una API REST (Javalin) que reparte pedidos de delivery usando concurrencia real en Java (asignación automática productor-consumidor + competencia manual con locks por pedido).

**Architecture:** Proceso independiente con servidor Javalin en el puerto 8080. Un `CentroDespacho` (instancia única compartida por el server) coordina el estado en memoria con estructuras thread-safe (`BlockingQueue`, `ConcurrentHashMap`, `ReentrantLock`, `AtomicReference`) y persiste cada transición vía el `PedidoRepository` existente. Un hilo `Despachador` (en un `ExecutorService`) consume la cola y asigna automáticamente; un `ScheduledExecutorService` sondea la BD por pedidos nuevos.

**Tech Stack:** Java 21, Maven, Javalin 6.1.3, Jackson, JDBC (mssql-jdbc), JUnit 5 (nuevo, para pruebas).

## Global Constraints

- Java 21 (`maven.compiler.source/target=21`), encoding UTF-8.
- Paquete nuevo: `com.laptitefrance.delivery.despacho`.
- Estados de pedido (verbatim): `EN ESPERA`, `EN CAMINO`, `ENTREGADO`.
- Persistencia mediante `IRepositorioBase<Pedido, String>` (firmas: `void insert(T)`, `Optional<T> findById(ID)`, `List<T> findAll()`, `void update(T)`, `void deleteById(ID)`).
- Toda sección crítica usa `lock()`/`unlock()` en `try/finally`; la persistencia ocurre dentro del lock.
- Idioma de mensajes y comentarios: español con acentos correctos.
- Puerto del servidor REST: 8080. CORS abierto (ejercicio académico local).

---

## File Structure

| Archivo | Responsabilidad |
|---|---|
| `pom.xml` (modificar) | Añadir JUnit 5 (test) y `maven-surefire-plugin`; añadir `exec-maven-plugin` ya presente |
| `src/main/java/.../despacho/EstadoRepartidor.java` (crear) | enum `{ LIBRE, OCUPADO }` |
| `src/main/java/.../despacho/RepartidorEnLinea.java` (crear) | Estado en memoria de un repartidor; ocupar/liberar atómico (CAS) |
| `src/main/java/.../despacho/ResultadoOperacion.java` (crear) | Resultado de negocio (`Tipo` + mensaje + datos) → se mapea a HTTP |
| `src/main/java/.../despacho/CentroDespacho.java` (crear) | Núcleo concurrente: estado compartido + operaciones thread-safe |
| `src/main/java/.../despacho/Despachador.java` (crear) | Runnable: bucle de asignación automática (consumidor de la cola) |
| `src/main/java/.../despacho/ApiDeliveryServer.java` (crear) | Endpoints REST Javalin + `main` |
| `src/main/java/.../despacho/SimuladorConcurrencia.java` (crear) | `main` de demostración: N hilos compiten por un pedido |
| `src/main/java/.../controllers/ApiRepartidor.java` (eliminar) | Reemplazado por `ApiDeliveryServer` |
| `src/test/java/.../despacho/FakePedidoRepository.java` (crear) | Repo en memoria para pruebas (implementa `IRepositorioBase<Pedido,String>`) |
| `src/test/java/.../despacho/CentroDespachoTest.java` (crear) | Pruebas JUnit de la lógica concurrente |
| `README.md` (modificar) | Documentación de la API, concurrencia y correcciones visuales |

`<...>` = `com/laptitefrance/delivery`.

**Cómo ejecutar los tests** (sin Maven en PATH): usar el "Test Runner for Java" de la extensión de Java de VSCode (botón *Run Test* sobre la clase/método). Con Maven instalado: `mvn test`.

---

## Task 1: Configurar JUnit 5 y el repositorio falso de pruebas

**Files:**
- Modify: `pom.xml`
- Create: `src/test/java/com/laptitefrance/delivery/despacho/FakePedidoRepository.java`
- Test: `src/test/java/com/laptitefrance/delivery/despacho/FakePedidoRepositoryTest.java`

**Interfaces:**
- Consumes: `IRepositorioBase<Pedido,String>`, `Pedido`.
- Produces: `FakePedidoRepository` (repo en memoria thread-safe con `ConcurrentHashMap<String,Pedido>`; `insert` autogenera `CodPedido` `P0001`... si es null).

- [ ] **Step 1: Añadir JUnit 5 y surefire al pom.xml**

En `pom.xml`, dentro de `<dependencies>`, añadir tras la dependencia de `slf4j-simple`:

```xml
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>
```

Y dentro de `<build><plugins>` (junto al `exec-maven-plugin` existente), añadir:

```xml
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>
```

- [ ] **Step 2: Crear el repositorio falso en memoria**

Crear `src/test/java/com/laptitefrance/delivery/despacho/FakePedidoRepository.java`:

```java
package com.laptitefrance.delivery.despacho;

import com.laptitefrance.delivery.models.Pedido;
import com.laptitefrance.delivery.repositories.IRepositorioBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Repositorio en memoria, thread-safe, para pruebas sin base de datos. */
public class FakePedidoRepository implements IRepositorioBase<Pedido, String> {

    private final ConcurrentHashMap<String, Pedido> datos = new ConcurrentHashMap<>();
    private final AtomicInteger secuencia = new AtomicInteger(0);

    @Override
    public void insert(Pedido entity) {
        if (entity.getCodPedido() == null) {
            entity.setCodPedido(String.format("P%04d", secuencia.incrementAndGet()));
        }
        datos.put(entity.getCodPedido(), entity);
    }

    @Override
    public Optional<Pedido> findById(String id) {
        return Optional.ofNullable(datos.get(id));
    }

    @Override
    public List<Pedido> findAll() {
        return new ArrayList<>(datos.values());
    }

    @Override
    public void update(Pedido entity) {
        datos.put(entity.getCodPedido(), entity);
    }

    @Override
    public void deleteById(String id) {
        datos.remove(id);
    }
}
```

- [ ] **Step 3: Escribir el test del repo falso**

Crear `src/test/java/com/laptitefrance/delivery/despacho/FakePedidoRepositoryTest.java`:

```java
package com.laptitefrance.delivery.despacho;

import com.laptitefrance.delivery.models.Pedido;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FakePedidoRepositoryTest {

    @Test
    void insertAutogeneraCodigoYFindByIdLoEncuentra() {
        FakePedidoRepository repo = new FakePedidoRepository();
        Pedido p = new Pedido();
        p.setEstado("EN ESPERA");
        repo.insert(p);

        assertEquals("P0001", p.getCodPedido());
        assertTrue(repo.findById("P0001").isPresent());
        assertEquals("EN ESPERA", repo.findById("P0001").get().getEstado());
    }
}
```

- [ ] **Step 4: Ejecutar el test y verificar que pasa**

Run (VSCode Test Runner sobre `FakePedidoRepositoryTest`, o `mvn test -Dtest=FakePedidoRepositoryTest`)
Expected: PASS (1 test).

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/test/java/com/laptitefrance/delivery/despacho/FakePedidoRepository.java src/test/java/com/laptitefrance/delivery/despacho/FakePedidoRepositoryTest.java
git commit -m "test: configurar JUnit 5 y repositorio falso en memoria"
```

---

## Task 2: `EstadoRepartidor` y `RepartidorEnLinea`

**Files:**
- Create: `src/main/java/com/laptitefrance/delivery/despacho/EstadoRepartidor.java`
- Create: `src/main/java/com/laptitefrance/delivery/despacho/RepartidorEnLinea.java`
- Test: `src/test/java/com/laptitefrance/delivery/despacho/RepartidorEnLineaTest.java`

**Interfaces:**
- Produces:
  - `enum EstadoRepartidor { LIBRE, OCUPADO }`
  - `RepartidorEnLinea(String codRepartidor)`; `String getCodRepartidor()`; `EstadoRepartidor getEstado()`; `String getCodPedidoActual()`; `boolean intentarOcupar(String codPedido)` (CAS atómico LIBRE→OCUPADO, devuelve true solo si ganó); `void liberar()`.

- [ ] **Step 1: Escribir el test (la concurrencia del CAS)**

Crear `src/test/java/com/laptitefrance/delivery/despacho/RepartidorEnLineaTest.java`:

```java
package com.laptitefrance.delivery.despacho;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RepartidorEnLineaTest {

    @Test
    void soloUnHiloLograOcuparElRepartidor() throws InterruptedException {
        RepartidorEnLinea rep = new RepartidorEnLinea("E004");
        int hilos = 20;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch listos = new CountDownLatch(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger(0);

        for (int i = 0; i < hilos; i++) {
            pool.submit(() -> {
                listos.countDown();
                try {
                    salida.await();
                    if (rep.intentarOcupar("P0001")) {
                        exitos.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        listos.await();
        salida.countDown(); // soltar todos a la vez
        pool.shutdown();
        pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        assertEquals(1, exitos.get());
        assertEquals(EstadoRepartidor.OCUPADO, rep.getEstado());
        assertEquals("P0001", rep.getCodPedidoActual());
    }
}
```

- [ ] **Step 2: Ejecutar el test y verificar que FALLA**

Run: VSCode Test Runner sobre `RepartidorEnLineaTest` (o `mvn test -Dtest=RepartidorEnLineaTest`)
Expected: FAIL con error de compilación ("cannot find symbol RepartidorEnLinea / EstadoRepartidor").

- [ ] **Step 3: Crear el enum**

Crear `src/main/java/com/laptitefrance/delivery/despacho/EstadoRepartidor.java`:

```java
package com.laptitefrance.delivery.despacho;

/** Estado de un repartidor conectado a la API de despacho. */
public enum EstadoRepartidor {
    LIBRE,
    OCUPADO
}
```

- [ ] **Step 4: Crear `RepartidorEnLinea`**

Crear `src/main/java/com/laptitefrance/delivery/despacho/RepartidorEnLinea.java`:

```java
package com.laptitefrance.delivery.despacho;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Estado en memoria de un repartidor conectado. La transición LIBRE -> OCUPADO
 * se hace con compareAndSet (CAS) para que, si varios hilos intentan ocupar el
 * mismo repartidor a la vez, solo uno gane sin necesidad de bloqueos.
 */
public class RepartidorEnLinea {

    private final String codRepartidor;
    private final AtomicReference<EstadoRepartidor> estado =
            new AtomicReference<>(EstadoRepartidor.LIBRE);
    private volatile String codPedidoActual;

    public RepartidorEnLinea(String codRepartidor) {
        this.codRepartidor = codRepartidor;
    }

    public String getCodRepartidor() {
        return codRepartidor;
    }

    public EstadoRepartidor getEstado() {
        return estado.get();
    }

    public String getCodPedidoActual() {
        return codPedidoActual;
    }

    /** Intenta pasar de LIBRE a OCUPADO de forma atómica. */
    public boolean intentarOcupar(String codPedido) {
        boolean gano = estado.compareAndSet(EstadoRepartidor.LIBRE, EstadoRepartidor.OCUPADO);
        if (gano) {
            this.codPedidoActual = codPedido;
        }
        return gano;
    }

    /** Devuelve el repartidor al estado LIBRE. */
    public void liberar() {
        this.codPedidoActual = null;
        estado.set(EstadoRepartidor.LIBRE);
    }
}
```

- [ ] **Step 5: Ejecutar el test y verificar que pasa**

Run: VSCode Test Runner sobre `RepartidorEnLineaTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/despacho/EstadoRepartidor.java src/main/java/com/laptitefrance/delivery/despacho/RepartidorEnLinea.java src/test/java/com/laptitefrance/delivery/despacho/RepartidorEnLineaTest.java
git commit -m "feat: RepartidorEnLinea con ocupar/liberar atómico (CAS)"
```

---

## Task 3: `ResultadoOperacion`

**Files:**
- Create: `src/main/java/com/laptitefrance/delivery/despacho/ResultadoOperacion.java`
- Test: `src/test/java/com/laptitefrance/delivery/despacho/ResultadoOperacionTest.java`

**Interfaces:**
- Produces:
  - `enum ResultadoOperacion.Tipo { OK, YA_TOMADO, NO_ENCONTRADO, REPARTIDOR_NO_DISPONIBLE, ERROR_INTERNO }`
  - `ResultadoOperacion(Tipo, String mensaje, Object datos)`; getters `getTipo()`, `getMensaje()`, `getDatos()`.
  - Fábricas estáticas: `ok(String, Object)`, `yaTomado(String)`, `noEncontrado(String)`, `repartidorNoDisponible(String)`, `errorInterno(String)`.
  - `int httpStatus()` → OK=200, YA_TOMADO=409, NO_ENCONTRADO=404, REPARTIDOR_NO_DISPONIBLE=400, ERROR_INTERNO=500.

- [ ] **Step 1: Escribir el test**

Crear `src/test/java/com/laptitefrance/delivery/despacho/ResultadoOperacionTest.java`:

```java
package com.laptitefrance.delivery.despacho;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultadoOperacionTest {

    @Test
    void fabricasYMapeoHttp() {
        assertEquals(200, ResultadoOperacion.ok("listo", null).httpStatus());
        assertEquals(409, ResultadoOperacion.yaTomado("ya fue tomado").httpStatus());
        assertEquals(404, ResultadoOperacion.noEncontrado("no existe").httpStatus());
        assertEquals(400, ResultadoOperacion.repartidorNoDisponible("ocupado").httpStatus());
        assertEquals(500, ResultadoOperacion.errorInterno("fallo BD").httpStatus());
        assertEquals(ResultadoOperacion.Tipo.OK, ResultadoOperacion.ok("x", null).getTipo());
        assertEquals("ya fue tomado", ResultadoOperacion.yaTomado("ya fue tomado").getMensaje());
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que FALLA**

Run: VSCode Test Runner sobre `ResultadoOperacionTest`
Expected: FAIL ("cannot find symbol ResultadoOperacion").

- [ ] **Step 3: Implementar `ResultadoOperacion`**

Crear `src/main/java/com/laptitefrance/delivery/despacho/ResultadoOperacion.java`:

```java
package com.laptitefrance.delivery.despacho;

/** Resultado de una operación de despacho; se mapea a un código HTTP en la capa REST. */
public class ResultadoOperacion {

    public enum Tipo {
        OK, YA_TOMADO, NO_ENCONTRADO, REPARTIDOR_NO_DISPONIBLE, ERROR_INTERNO
    }

    private final Tipo tipo;
    private final String mensaje;
    private final Object datos;

    public ResultadoOperacion(Tipo tipo, String mensaje, Object datos) {
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.datos = datos;
    }

    public static ResultadoOperacion ok(String mensaje, Object datos) {
        return new ResultadoOperacion(Tipo.OK, mensaje, datos);
    }

    public static ResultadoOperacion yaTomado(String mensaje) {
        return new ResultadoOperacion(Tipo.YA_TOMADO, mensaje, null);
    }

    public static ResultadoOperacion noEncontrado(String mensaje) {
        return new ResultadoOperacion(Tipo.NO_ENCONTRADO, mensaje, null);
    }

    public static ResultadoOperacion repartidorNoDisponible(String mensaje) {
        return new ResultadoOperacion(Tipo.REPARTIDOR_NO_DISPONIBLE, mensaje, null);
    }

    public static ResultadoOperacion errorInterno(String mensaje) {
        return new ResultadoOperacion(Tipo.ERROR_INTERNO, mensaje, null);
    }

    public Tipo getTipo() {
        return tipo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public Object getDatos() {
        return datos;
    }

    public int httpStatus() {
        switch (tipo) {
            case OK:                        return 200;
            case YA_TOMADO:                 return 409;
            case NO_ENCONTRADO:             return 404;
            case REPARTIDOR_NO_DISPONIBLE:  return 400;
            case ERROR_INTERNO:
            default:                        return 500;
        }
    }
}
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: VSCode Test Runner sobre `ResultadoOperacionTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/despacho/ResultadoOperacion.java src/test/java/com/laptitefrance/delivery/despacho/ResultadoOperacionTest.java
git commit -m "feat: ResultadoOperacion con mapeo a códigos HTTP"
```

---

## Task 4: `CentroDespacho` — conectar/desconectar y competencia manual (`tomarPedido`)

**Files:**
- Create: `src/main/java/com/laptitefrance/delivery/despacho/CentroDespacho.java`
- Test: `src/test/java/com/laptitefrance/delivery/despacho/CentroDespachoTest.java`

**Interfaces:**
- Consumes: `IRepositorioBase<Pedido,String>`, `Pedido`, `RepartidorEnLinea`, `EstadoRepartidor`, `ResultadoOperacion`.
- Produces (parte 1 de `CentroDespacho`):
  - `CentroDespacho(IRepositorioBase<Pedido,String> repo)` (constructor inyectable para pruebas).
  - `ResultadoOperacion conectarRepartidor(String cod)` / `ResultadoOperacion desconectarRepartidor(String cod)`.
  - `List<RepartidorEnLinea> listarDisponibles()`.
  - `ResultadoOperacion tomarPedido(String codPedido, String codRepartidor)`.
  - Helper interno `ReentrantLock lockDe(String codPedido)`.
  - Campos: `colaPendientes` (`BlockingQueue<String>`), `repartidoresDisponibles` (`ConcurrentHashMap`), `locksPorPedido` (`ConcurrentHashMap`), `codigosEncolados` (`Set`). (Los executors y la asignación automática llegan en Task 5.)

- [ ] **Step 1: Escribir los tests de conexión y competencia manual**

Crear `src/test/java/com/laptitefrance/delivery/despacho/CentroDespachoTest.java`:

```java
package com.laptitefrance.delivery.despacho;

import com.laptitefrance.delivery.models.Pedido;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CentroDespachoTest {

    private Pedido pedidoEnEspera(String cod) {
        Pedido p = new Pedido();
        p.setCodPedido(cod);
        p.setEstado("EN ESPERA");
        return p;
    }

    @Test
    void conectarYDesconectarRepartidor() {
        CentroDespacho centro = new CentroDespacho(new FakePedidoRepository());

        assertEquals(ResultadoOperacion.Tipo.OK, centro.conectarRepartidor("E004").getTipo());
        assertEquals(1, centro.listarDisponibles().size());

        assertEquals(ResultadoOperacion.Tipo.OK, centro.desconectarRepartidor("E004").getTipo());
        assertEquals(0, centro.listarDisponibles().size());
    }

    @Test
    void tomarPedidoInexistenteDevuelveNoEncontrado() {
        CentroDespacho centro = new CentroDespacho(new FakePedidoRepository());
        centro.conectarRepartidor("E004");

        ResultadoOperacion r = centro.tomarPedido("P9999", "E004");
        assertEquals(ResultadoOperacion.Tipo.NO_ENCONTRADO, r.getTipo());
    }

    @Test
    void variosRepartidoresCompitenSoloUnoGana() throws InterruptedException {
        FakePedidoRepository repo = new FakePedidoRepository();
        repo.insert(pedidoEnEspera("P0001"));
        CentroDespacho centro = new CentroDespacho(repo);

        int hilos = 12;
        for (int i = 0; i < hilos; i++) {
            centro.conectarRepartidor("E" + String.format("%03d", i));
        }

        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch listos = new CountDownLatch(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger oks = new AtomicInteger(0);
        AtomicInteger yaTomados = new AtomicInteger(0);

        for (int i = 0; i < hilos; i++) {
            final String cod = "E" + String.format("%03d", i);
            pool.submit(() -> {
                listos.countDown();
                try {
                    salida.await();
                    ResultadoOperacion r = centro.tomarPedido("P0001", cod);
                    if (r.getTipo() == ResultadoOperacion.Tipo.OK) oks.incrementAndGet();
                    else if (r.getTipo() == ResultadoOperacion.Tipo.YA_TOMADO) yaTomados.incrementAndGet();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        listos.await();
        salida.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(1, oks.get());
        assertEquals(hilos - 1, yaTomados.get());
        assertEquals("EN CAMINO", repo.findById("P0001").get().getEstado());
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que FALLA**

Run: VSCode Test Runner sobre `CentroDespachoTest`
Expected: FAIL ("cannot find symbol CentroDespacho").

- [ ] **Step 3: Implementar `CentroDespacho` (parte 1)**

Crear `src/main/java/com/laptitefrance/delivery/despacho/CentroDespacho.java`:

```java
package com.laptitefrance.delivery.despacho;

import com.laptitefrance.delivery.models.Pedido;
import com.laptitefrance.delivery.repositories.IRepositorioBase;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Núcleo concurrente del despacho de pedidos. Mantiene el estado compartido en
 * memoria con estructuras thread-safe y persiste cada transición en la BD.
 *
 * Granularidad de bloqueo fina: un ReentrantLock por pedido (no un lock global),
 * de modo que dos pedidos distintos se procesan en paralelo pero un mismo pedido
 * nunca se asigna dos veces.
 */
public class CentroDespacho {

    static final String EN_ESPERA = "EN ESPERA";
    static final String EN_CAMINO = "EN CAMINO";
    static final String ENTREGADO = "ENTREGADO";

    private final IRepositorioBase<Pedido, String> pedidoRepository;

    private final BlockingQueue<String> colaPendientes = new LinkedBlockingQueue<>();
    private final ConcurrentHashMap<String, RepartidorEnLinea> repartidoresDisponibles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> locksPorPedido = new ConcurrentHashMap<>();
    private final Set<String> codigosEncolados = ConcurrentHashMap.newKeySet();

    public CentroDespacho(IRepositorioBase<Pedido, String> pedidoRepository) {
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository);
    }

    // --- Repartidores -------------------------------------------------------

    public ResultadoOperacion conectarRepartidor(String cod) {
        if (cod == null || cod.isBlank()) {
            return ResultadoOperacion.noEncontrado("Código de repartidor vacío.");
        }
        repartidoresDisponibles.putIfAbsent(cod, new RepartidorEnLinea(cod));
        return ResultadoOperacion.ok("Repartidor conectado: " + cod, null);
    }

    public ResultadoOperacion desconectarRepartidor(String cod) {
        RepartidorEnLinea quitado = repartidoresDisponibles.remove(cod);
        if (quitado == null) {
            return ResultadoOperacion.noEncontrado("El repartidor no estaba conectado: " + cod);
        }
        return ResultadoOperacion.ok("Repartidor desconectado: " + cod, null);
    }

    public List<RepartidorEnLinea> listarDisponibles() {
        return new ArrayList<>(repartidoresDisponibles.values());
    }

    // --- Competencia manual -------------------------------------------------

    /** Varios repartidores pueden llamar a la vez; solo el primero completa la toma. */
    public ResultadoOperacion tomarPedido(String codPedido, String codRepartidor) {
        RepartidorEnLinea rep = repartidoresDisponibles.get(codRepartidor);
        if (rep == null) {
            return ResultadoOperacion.repartidorNoDisponible("Repartidor no conectado: " + codRepartidor);
        }

        ReentrantLock lock = lockDe(codPedido);
        lock.lock();
        try {
            Optional<Pedido> opt = pedidoRepository.findById(codPedido);
            if (opt.isEmpty()) {
                return ResultadoOperacion.noEncontrado("No existe el pedido: " + codPedido);
            }
            Pedido pedido = opt.get();
            if (!EN_ESPERA.equalsIgnoreCase(pedido.getEstado())) {
                return ResultadoOperacion.yaTomado("El pedido ya fue tomado: " + codPedido);
            }
            if (!rep.intentarOcupar(codPedido)) {
                return ResultadoOperacion.repartidorNoDisponible("El repartidor ya está ocupado: " + codRepartidor);
            }
            try {
                aplicarAsignacion(pedido, codRepartidor);
            } catch (RuntimeException ex) {
                rep.liberar();
                return ResultadoOperacion.errorInterno("Error al persistir la asignación: " + ex.getMessage());
            }
            codigosEncolados.remove(codPedido);
            return ResultadoOperacion.ok("Pedido " + codPedido + " asignado a " + codRepartidor, null);
        } finally {
            lock.unlock();
        }
    }

    // --- Helpers ------------------------------------------------------------

    private void aplicarAsignacion(Pedido pedido, String codRepartidor) {
        pedido.setEstado(EN_CAMINO);
        pedido.setCodRepartidor(codRepartidor);
        pedido.setHoraEnvio(LocalDateTime.now());
        pedidoRepository.update(pedido);
    }

    ReentrantLock lockDe(String codPedido) {
        return locksPorPedido.computeIfAbsent(codPedido, k -> new ReentrantLock());
    }
}
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: VSCode Test Runner sobre `CentroDespachoTest` (los 3 tests de esta tarea)
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/despacho/CentroDespacho.java src/test/java/com/laptitefrance/delivery/despacho/CentroDespachoTest.java
git commit -m "feat: CentroDespacho con conexión de repartidores y toma manual con lock por pedido"
```

---

## Task 5: `CentroDespacho` — entregar + asignación automática (`Despachador`)

**Files:**
- Modify: `src/main/java/com/laptitefrance/delivery/despacho/CentroDespacho.java`
- Create: `src/main/java/com/laptitefrance/delivery/despacho/Despachador.java`
- Test: `src/test/java/com/laptitefrance/delivery/despacho/CentroDespachoTest.java` (añadir tests)

**Interfaces:**
- Produces (parte 2 de `CentroDespacho`):
  - `ResultadoOperacion entregarPedido(String codPedido, String codRepartidor)`.
  - `void encolarPedido(String codPedido)` (idempotente vía `codigosEncolados`).
  - `String tomarSiguientePendiente()` (bloqueante, usado por el `Despachador`: `colaPendientes.take()`).
  - `ResultadoOperacion asignarAutomatico(String codPedido)` (busca repartidor libre, lo ocupa con CAS y asigna bajo el lock del pedido; si no hay libre devuelve `REPARTIDOR_NO_DISPONIBLE` para que el `Despachador` reencole).
  - `void reencolar(String codPedido)`.
  - `void iniciar()` / `void detener()` (arrancan/paran el `ExecutorService` del `Despachador` y el `ScheduledExecutorService` de sondeo; `iniciar` además carga los `EN ESPERA` actuales).
  - `int pendientesEnCola()`.
- `Despachador implements Runnable`, construido con `Despachador(CentroDespacho centro)`.

- [ ] **Step 1: Añadir los tests de entrega y asignación automática**

Añadir a `CentroDespachoTest.java` estos métodos (dentro de la clase):

```java
    @Test
    void entregarLiberaAlRepartidor() {
        FakePedidoRepository repo = new FakePedidoRepository();
        repo.insert(pedidoEnEspera("P0001"));
        CentroDespacho centro = new CentroDespacho(repo);
        centro.conectarRepartidor("E004");

        assertEquals(ResultadoOperacion.Tipo.OK, centro.tomarPedido("P0001", "E004").getTipo());
        assertEquals(EstadoRepartidor.OCUPADO, centro.listarDisponibles().get(0).getEstado());

        ResultadoOperacion entrega = centro.entregarPedido("P0001", "E004");
        assertEquals(ResultadoOperacion.Tipo.OK, entrega.getTipo());
        assertEquals("ENTREGADO", repo.findById("P0001").get().getEstado());
        assertEquals(EstadoRepartidor.LIBRE, centro.listarDisponibles().get(0).getEstado());
    }

    @Test
    void asignacionAutomaticaAsignaPedidoEncolado() throws InterruptedException {
        FakePedidoRepository repo = new FakePedidoRepository();
        repo.insert(pedidoEnEspera("P0001"));
        CentroDespacho centro = new CentroDespacho(repo);
        centro.conectarRepartidor("E004");
        centro.encolarPedido("P0001");

        centro.iniciar();
        try {
            // Esperar (con timeout) a que el hilo despachador asigne.
            long limite = System.currentTimeMillis() + 3000;
            while (!"EN CAMINO".equals(repo.findById("P0001").get().getEstado())
                    && System.currentTimeMillis() < limite) {
                Thread.sleep(50);
            }
        } finally {
            centro.detener();
        }

        assertEquals("EN CAMINO", repo.findById("P0001").get().getEstado());
        assertEquals("E004", repo.findById("P0001").get().getCodRepartidor());
    }
```

- [ ] **Step 2: Ejecutar y verificar que FALLA**

Run: VSCode Test Runner sobre `CentroDespachoTest`
Expected: FAIL ("cannot find symbol: entregarPedido / encolarPedido / iniciar / detener").

- [ ] **Step 3: Crear el `Despachador`**

Crear `src/main/java/com/laptitefrance/delivery/despacho/Despachador.java`:

```java
package com.laptitefrance.delivery.despacho;

/**
 * Hilo consumidor de la cola de pedidos pendientes. Toma un pedido (se bloquea
 * si la cola está vacía), intenta asignarlo a un repartidor libre y, si no hay
 * ninguno disponible, lo reencola tras una breve pausa.
 */
public class Despachador implements Runnable {

    private static final long PAUSA_SIN_REPARTIDOR_MS = 500;

    private final CentroDespacho centro;

    public Despachador(CentroDespacho centro) {
        this.centro = centro;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            String codPedido;
            try {
                codPedido = centro.tomarSiguientePendiente(); // bloqueante
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            ResultadoOperacion r = centro.asignarAutomatico(codPedido);
            if (r.getTipo() == ResultadoOperacion.Tipo.REPARTIDOR_NO_DISPONIBLE) {
                centro.reencolar(codPedido);
                try {
                    Thread.sleep(PAUSA_SIN_REPARTIDOR_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            // OK / YA_TOMADO / NO_ENCONTRADO: el pedido ya no sigue en cola; continuar.
        }
    }
}
```

- [ ] **Step 4: Ampliar `CentroDespacho` con entrega, asignación automática y ciclo de vida**

En `CentroDespacho.java`, añadir estos imports al bloque de imports existente:

```java
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
```

Añadir estos campos junto a los demás campos de instancia:

```java
    private static final long PERIODO_SONDEO_SEG = 5;

    private ExecutorService despachadorPool;
    private ScheduledExecutorService sondeoPool;
```

Añadir estos métodos dentro de la clase (antes del último `}`):

```java
    // --- Ciclo de vida ------------------------------------------------------

    public void iniciar() {
        cargarPendientesDesdeBD();
        despachadorPool = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "despachador");
            t.setDaemon(true);
            return t;
        });
        despachadorPool.submit(new Despachador(this));

        sondeoPool = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sondeo-pedidos");
            t.setDaemon(true);
            return t;
        });
        sondeoPool.scheduleAtFixedRate(this::cargarPendientesDesdeBD,
                PERIODO_SONDEO_SEG, PERIODO_SONDEO_SEG, TimeUnit.SECONDS);
    }

    public void detener() {
        if (despachadorPool != null) despachadorPool.shutdownNow();
        if (sondeoPool != null) sondeoPool.shutdownNow();
    }

    private void cargarPendientesDesdeBD() {
        for (Pedido p : pedidoRepository.findAll()) {
            if (p != null && EN_ESPERA.equalsIgnoreCase(p.getEstado())) {
                encolarPedido(p.getCodPedido());
            }
        }
    }

    // --- Asignación automática ---------------------------------------------

    public void encolarPedido(String codPedido) {
        if (codPedido != null && codigosEncolados.add(codPedido)) {
            colaPendientes.add(codPedido);
        }
    }

    public void reencolar(String codPedido) {
        colaPendientes.add(codPedido); // sigue marcado en codigosEncolados
    }

    /** Bloqueante: lo usa el Despachador para esperar el siguiente pedido. */
    public String tomarSiguientePendiente() throws InterruptedException {
        return colaPendientes.take();
    }

    public int pendientesEnCola() {
        return colaPendientes.size();
    }

    public ResultadoOperacion asignarAutomatico(String codPedido) {
        ReentrantLock lock = lockDe(codPedido);
        lock.lock();
        try {
            Optional<Pedido> opt = pedidoRepository.findById(codPedido);
            if (opt.isEmpty()) {
                codigosEncolados.remove(codPedido);
                return ResultadoOperacion.noEncontrado("No existe el pedido: " + codPedido);
            }
            Pedido pedido = opt.get();
            if (!EN_ESPERA.equalsIgnoreCase(pedido.getEstado())) {
                codigosEncolados.remove(codPedido);
                return ResultadoOperacion.yaTomado("El pedido ya no está en espera: " + codPedido);
            }
            RepartidorEnLinea libre = buscarYOcuparRepartidorLibre(codPedido);
            if (libre == null) {
                return ResultadoOperacion.repartidorNoDisponible("No hay repartidores libres.");
            }
            try {
                aplicarAsignacion(pedido, libre.getCodRepartidor());
            } catch (RuntimeException ex) {
                libre.liberar();
                return ResultadoOperacion.errorInterno("Error al persistir: " + ex.getMessage());
            }
            codigosEncolados.remove(codPedido);
            return ResultadoOperacion.ok(
                    "Pedido " + codPedido + " asignado automáticamente a " + libre.getCodRepartidor(), null);
        } finally {
            lock.unlock();
        }
    }

    private RepartidorEnLinea buscarYOcuparRepartidorLibre(String codPedido) {
        for (RepartidorEnLinea rep : repartidoresDisponibles.values()) {
            if (rep.intentarOcupar(codPedido)) {
                return rep;
            }
        }
        return null;
    }

    // --- Entrega ------------------------------------------------------------

    public ResultadoOperacion entregarPedido(String codPedido, String codRepartidor) {
        ReentrantLock lock = lockDe(codPedido);
        lock.lock();
        try {
            Optional<Pedido> opt = pedidoRepository.findById(codPedido);
            if (opt.isEmpty()) {
                return ResultadoOperacion.noEncontrado("No existe el pedido: " + codPedido);
            }
            Pedido pedido = opt.get();
            if (!EN_CAMINO.equalsIgnoreCase(pedido.getEstado())) {
                return ResultadoOperacion.repartidorNoDisponible(
                        "El pedido no está EN CAMINO: " + codPedido);
            }
            pedido.setEstado(ENTREGADO);
            pedido.setTiempoEntReal(LocalDateTime.now());
            pedidoRepository.update(pedido);

            RepartidorEnLinea rep = repartidoresDisponibles.get(codRepartidor);
            if (rep != null) {
                rep.liberar();
            }
            return ResultadoOperacion.ok("Pedido " + codPedido + " entregado.", null);
        } finally {
            lock.unlock();
        }
    }

    public Map<String, Object> estadoDespacho() {
        long libres = repartidoresDisponibles.values().stream()
                .filter(r -> r.getEstado() == EstadoRepartidor.LIBRE).count();
        return Map.of(
                "pendientesEnCola", colaPendientes.size(),
                "repartidoresConectados", repartidoresDisponibles.size(),
                "repartidoresLibres", libres);
    }
```

- [ ] **Step 5: Ejecutar y verificar que pasa**

Run: VSCode Test Runner sobre `CentroDespachoTest` (todos los tests)
Expected: PASS (5 tests).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/despacho/CentroDespacho.java src/main/java/com/laptitefrance/delivery/despacho/Despachador.java src/test/java/com/laptitefrance/delivery/despacho/CentroDespachoTest.java
git commit -m "feat: asignación automática (Despachador + cola) y entrega de pedidos"
```

---

## Task 6: `ApiDeliveryServer` (REST) y eliminación de `ApiRepartidor`

**Files:**
- Create: `src/main/java/com/laptitefrance/delivery/despacho/ApiDeliveryServer.java`
- Delete: `src/main/java/com/laptitefrance/delivery/controllers/ApiRepartidor.java`
- Modify: `pom.xml` (cambiar `mainClass` de exec al nuevo server)

**Interfaces:**
- Consumes: `CentroDespacho`, `ResultadoOperacion`, `PedidoRepository`.
- Produces: `ApiDeliveryServer.main(String[])` que levanta Javalin en 8080.

> **Nota de verificación:** esta capa es delgada (delega y mapea a HTTP). Se verifica **manualmente** con `curl` (no con JUnit) para no añadir dependencias de test HTTP.

- [ ] **Step 1: Implementar el servidor REST**

Crear `src/main/java/com/laptitefrance/delivery/despacho/ApiDeliveryServer.java`:

```java
package com.laptitefrance.delivery.despacho;

import com.laptitefrance.delivery.repositories.PedidoRepository;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Map;

/**
 * Servidor REST (Javalin) de la API de delivery concurrente. Mantiene una única
 * instancia de CentroDespacho compartida por todos los endpoints (los hilos de
 * Javalin atienden las peticiones en paralelo; el CentroDespacho es thread-safe).
 */
public class ApiDeliveryServer {

    private static final int PUERTO = 8080;

    public static void main(String[] args) {
        CentroDespacho centro = new CentroDespacho(new PedidoRepository());
        centro.iniciar();
        Runtime.getRuntime().addShutdownHook(new Thread(centro::detener));

        Javalin app = Javalin.create(config ->
                config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()))
        ).start(PUERTO);

        app.post("/api/repartidores/{cod}/conectar", ctx ->
                responder(ctx, centro.conectarRepartidor(ctx.pathParam("cod"))));

        app.post("/api/repartidores/{cod}/desconectar", ctx ->
                responder(ctx, centro.desconectarRepartidor(ctx.pathParam("cod"))));

        app.get("/api/repartidores/disponibles", ctx ->
                ctx.json(centro.listarDisponibles()));

        app.get("/api/pedidos/pendientes", ctx ->
                ctx.json(Map.of("pendientesEnCola", centro.pendientesEnCola())));

        app.post("/api/pedidos/{cod}/tomar", ctx ->
                responder(ctx, centro.tomarPedido(ctx.pathParam("cod"), ctx.queryParam("repartidor"))));

        app.post("/api/pedidos/{cod}/entregar", ctx ->
                responder(ctx, centro.entregarPedido(ctx.pathParam("cod"), ctx.queryParam("repartidor"))));

        app.get("/api/despacho/estado", ctx ->
                ctx.json(centro.estadoDespacho()));

        System.out.println("✅ API de delivery encendida en http://localhost:" + PUERTO + "/api");
    }

    /** Traduce un ResultadoOperacion a estado HTTP + cuerpo JSON uniforme. */
    private static void responder(Context ctx, ResultadoOperacion r) {
        ctx.status(r.httpStatus());
        ctx.json(Map.of(
                "tipo", r.getTipo().name(),
                "mensaje", r.getMensaje() == null ? "" : r.getMensaje()));
    }
}
```

- [ ] **Step 2: Eliminar `ApiRepartidor`**

```bash
git rm src/main/java/com/laptitefrance/delivery/controllers/ApiRepartidor.java
```

- [ ] **Step 3: Apuntar el exec-maven-plugin al nuevo server (opcional, segundo mainClass)**

En `pom.xml`, dejar el `exec-maven-plugin` con `mainClass` apuntando a la app de escritorio
(`com.laptitefrance.delivery.Main`). Para arrancar el server por línea de comandos se usa el
parámetro de Maven, sin tocar el pom:

```bash
mvn exec:java -Dexec.mainClass=com.laptitefrance.delivery.despacho.ApiDeliveryServer
```

(No se requiere edición del pom en este paso; queda documentado el comando.)

- [ ] **Step 4: Verificación manual con curl**

Arrancar el server (VSCode: *Run* sobre `ApiDeliveryServer`, o el comando `mvn exec:java` de arriba).
Con la BD `LaPtiteFranceDB` poblada (`SQLQuery1.sql`) y al menos un pedido `EN ESPERA`:

```bash
curl -X POST "http://localhost:8080/api/repartidores/E004/conectar"
# Esperado: 200 {"tipo":"OK","mensaje":"Repartidor conectado: E004"}

curl "http://localhost:8080/api/despacho/estado"
# Esperado: 200 {"pendientesEnCola":..,"repartidoresConectados":1,"repartidoresLibres":..}

curl -X POST "http://localhost:8080/api/pedidos/P0001/tomar?repartidor=E004"
# Esperado: 200 {"tipo":"OK",...}  (o 409 {"tipo":"YA_TOMADO"} si la auto-asignación lo tomó antes)

curl -X POST "http://localhost:8080/api/pedidos/P0001/entregar?repartidor=E004"
# Esperado: 200 {"tipo":"OK","mensaje":"Pedido P0001 entregado."}
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/despacho/ApiDeliveryServer.java
git commit -m "feat: ApiDeliveryServer (Javalin) y retiro de ApiRepartidor simulado"
```

---

## Task 7: `SimuladorConcurrencia` (demostración)

**Files:**
- Create: `src/main/java/com/laptitefrance/delivery/despacho/SimuladorConcurrencia.java`

**Interfaces:**
- Consumes: `CentroDespacho`, `FakePedidoRepository`... **NO** — `FakePedidoRepository` está en `src/test`. El simulador vive en `src/main`, así que usa un repo en memoria propio mínimo definido inline o `PedidoRepository` real. Para mantenerlo autónomo y sin BD, se define un pequeño repo en memoria dentro del propio archivo.

- [ ] **Step 1: Implementar el simulador**

Crear `src/main/java/com/laptitefrance/delivery/despacho/SimuladorConcurrencia.java`:

```java
package com.laptitefrance.delivery.despacho;

import com.laptitefrance.delivery.models.Pedido;
import com.laptitefrance.delivery.repositories.IRepositorioBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demostración en vivo de la competencia por un pedido: N repartidores intentan
 * tomar el MISMO pedido a la vez y se comprueba que solo uno lo logra.
 *
 * Usa un repositorio en memoria propio (no requiere base de datos).
 */
public class SimuladorConcurrencia {

    /** Repo en memoria mínimo, autónomo para la demo. */
    static class RepoMemoria implements IRepositorioBase<Pedido, String> {
        private final ConcurrentHashMap<String, Pedido> datos = new ConcurrentHashMap<>();
        public void insert(Pedido e) { datos.put(e.getCodPedido(), e); }
        public Optional<Pedido> findById(String id) { return Optional.ofNullable(datos.get(id)); }
        public List<Pedido> findAll() { return new ArrayList<>(datos.values()); }
        public void update(Pedido e) { datos.put(e.getCodPedido(), e); }
        public void deleteById(String id) { datos.remove(id); }
    }

    public static void main(String[] args) throws InterruptedException {
        int repartidores = 10;

        RepoMemoria repo = new RepoMemoria();
        Pedido p = new Pedido();
        p.setCodPedido("P0001");
        p.setEstado("EN ESPERA");
        repo.insert(p);

        CentroDespacho centro = new CentroDespacho(repo);
        for (int i = 0; i < repartidores; i++) {
            centro.conectarRepartidor("E" + String.format("%03d", i));
        }

        ExecutorService pool = Executors.newFixedThreadPool(repartidores);
        CountDownLatch listos = new CountDownLatch(repartidores);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger ganadores = new AtomicInteger(0);

        for (int i = 0; i < repartidores; i++) {
            final String cod = "E" + String.format("%03d", i);
            pool.submit(() -> {
                listos.countDown();
                try {
                    salida.await();
                    ResultadoOperacion r = centro.tomarPedido("P0001", cod);
                    String marca = (r.getTipo() == ResultadoOperacion.Tipo.OK) ? "  <-- GANÓ" : "";
                    if (r.getTipo() == ResultadoOperacion.Tipo.OK) ganadores.incrementAndGet();
                    System.out.printf("Repartidor %s -> %s%s%n", cod, r.getTipo(), marca);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        listos.await();
        System.out.println("== Soltando " + repartidores + " repartidores a la vez ==");
        salida.countDown();
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("------------------------------------------");
        System.out.println("Ganadores (esperado = 1): " + ganadores.get());
        System.out.println("Estado final del pedido: " + repo.findById("P0001").get().getEstado());
    }
}
```

- [ ] **Step 2: Ejecutar el simulador y verificar la salida**

Run: VSCode *Run* sobre `SimuladorConcurrencia` (o `mvn exec:java -Dexec.mainClass=com.laptitefrance.delivery.despacho.SimuladorConcurrencia`)
Expected: la consola imprime exactamente una línea con `<-- GANÓ`, `Ganadores (esperado = 1): 1` y `Estado final del pedido: EN CAMINO`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/despacho/SimuladorConcurrencia.java
git commit -m "feat: SimuladorConcurrencia para demostrar la competencia por un pedido"
```

---

## Task 8: Documentación en el README

**Files:**
- Modify: `README.md`

**Interfaces:** N/A (documentación).

- [ ] **Step 1: Actualizar la tabla de Tecnologías**

En `README.md`, cambiar la fila de la API REST de:

```
| API REST (parcial)| [Javalin](https://javalin.io/) 6 + Jackson           |
```
a:

```
| API REST concurrente | [Javalin](https://javalin.io/) 6 + Jackson           |
| Concurrencia      | `java.util.concurrent` (ExecutorService, BlockingQueue, ConcurrentHashMap, ReentrantLock, AtomicReference) |
| Pruebas           | JUnit 5                                              |
```

- [ ] **Step 2: Actualizar "Cómo ejecutar" (punto de entrada y servidor REST)**

Reemplazar el punto 4 y 5 de la sección "▶ Cómo ejecutar" por:

```markdown
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
```

- [ ] **Step 3: Añadir la sección "API REST de Delivery concurrente"**

Insertar antes de la sección "🎯 Patrones de diseño y su importancia":

```markdown
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
```

- [ ] **Step 4: Añadir la sección "Programación concurrente: mecanismos y buenas prácticas"**

Insertar a continuación de la sección anterior:

```markdown
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
```

- [ ] **Step 5: Añadir la sección "Correcciones (bugs visuales)"**

Insertar antes de "🧩 Decisiones de diseño relevantes":

```markdown
## 🩹 Correcciones (interfaz)

- **Punto de entrada unificado:** se añadió `com.laptitefrance.delivery.Main` como arranque
  oficial de la app de escritorio (antes el `main` vivía dentro de `LoginView`).
- **Botones sin texto (Look & Feel):** al usar el *Look & Feel* del sistema (Windows), los
  `JButton` con `setBackground(color)` + `setForeground(Color.WHITE)` no pintaban su fondo de
  color y el texto blanco quedaba invisible (solo se veían los bordes). La solución fue usar el
  **Look & Feel multiplataforma por defecto** (Metal), que sí respeta esos colores. Por eso `Main`
  no fuerza el L&F del sistema.
```

- [ ] **Step 6: Añadir el paquete `despacho/` a la sección "Estructura del proyecto"**

En el árbol de "📁 Estructura del proyecto", añadir tras el bloque `controllers/`:

```
├── despacho/          → API REST concurrente de delivery (puerto 8080)
│   ├── ApiDeliveryServer.java   Servidor Javalin + endpoints (main)
│   ├── CentroDespacho.java      Núcleo concurrente (cola, locks, pool de repartidores)
│   ├── Despachador.java         Hilo de asignación automática (consumidor)
│   ├── RepartidorEnLinea.java   Estado en memoria del repartidor (ocupar/liberar atómico)
│   ├── EstadoRepartidor.java    enum LIBRE/OCUPADO
│   ├── ResultadoOperacion.java  Resultado de negocio → código HTTP
│   └── SimuladorConcurrencia.java  Demo: N repartidores compiten por un pedido
```

Y eliminar la línea `└── ApiRepartidor.java ...` del bloque `controllers/` (fue reemplazada).

> Nota: el README actual menciona los paquetes `events/` y `audit/`. Si ya no existen en el código
> (fueron removidos en commits previos), eliminar también esas referencias para que la estructura
> documentada coincida con la real. Verificar con `git ls-files src/main/java`.

- [ ] **Step 7: Commit**

```bash
git add README.md
git commit -m "docs: documentar API concurrente, mecanismos de concurrencia y correcciones de UI"
```

---

## Self-Review (completado por el autor del plan)

**1. Cobertura del spec:**
- §3 escenarios (auto + manual) → Tasks 4, 5, 7 ✓
- §5.1–5.3 modelos (Estado, RepartidorEnLinea, ResultadoOperacion) → Tasks 2, 3 ✓
- §5.4 CentroDespacho (estado + ops) → Tasks 4, 5 ✓
- §5.5 Despachador → Task 5 ✓
- §5.6 ApiDeliveryServer → Task 6 ✓
- §5.7 SimuladorConcurrencia → Task 7 ✓
- §6 endpoints → Task 6 ✓
- §7 mecanismos + buenas prácticas → cubiertos en código (Tasks 2–5) y documentados (Task 8) ✓
- §8 manejo de errores (mapeo HTTP) → Task 3 + uso en Tasks 4–6 ✓
- §9 documentación README → Task 8 ✓
- §11 sondeo periódico + apagado ordenado → Task 5 ✓
- §12 decisiones (sondeo, lock por pedido, reemplazo ApiRepartidor) → Tasks 5, 4, 6 ✓

**2. Placeholders:** ninguno; todos los pasos incluyen código/comandos completos.

**3. Consistencia de tipos:** `tomarPedido`, `entregarPedido`, `asignarAutomatico`, `encolarPedido`,
`reencolar`, `tomarSiguientePendiente`, `iniciar`, `detener`, `lockDe`, `intentarOcupar`, `liberar`,
`httpStatus`, `estadoDespacho` se usan con las mismas firmas en todas las tareas. `FakePedidoRepository`
vive en `src/test`; el simulador (en `src/main`) usa su propio `RepoMemoria` para no depender de test.
