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
