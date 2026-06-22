package com.laptitefrance.delivery.despacho;

import com.laptitefrance.delivery.models.Pedido;
import com.laptitefrance.delivery.repositories.IRepositorioBase;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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

    private static final long PERIODO_SONDEO_SEG = 5;

    private final IRepositorioBase<Pedido, String> pedidoRepository;

    private final BlockingQueue<String> colaPendientes = new LinkedBlockingQueue<>();
    private final ConcurrentHashMap<String, RepartidorEnLinea> repartidoresDisponibles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> locksPorPedido = new ConcurrentHashMap<>();
    private final Set<String> codigosEncolados = ConcurrentHashMap.newKeySet();

    private ExecutorService despachadorPool;
    private ScheduledExecutorService sondeoPool;

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
        if (codPedido == null || codPedido.isBlank()) {
            return ResultadoOperacion.noEncontrado("Código de pedido vacío.");
        }
        if (codRepartidor == null || codRepartidor.isBlank()) {
            return ResultadoOperacion.repartidorNoDisponible("Código de repartidor vacío.");
        }
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
}
