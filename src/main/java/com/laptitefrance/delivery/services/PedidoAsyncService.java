package com.laptitefrance.delivery.services;

import com.laptitefrance.delivery.audit.AuditoriaLog;
import com.laptitefrance.delivery.models.Pedido;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Servicios async (concurrencia) para operaciones pesadas.
 *
 * Requisito: usa CompletableFuture para no bloquear el hilo llamador.
 */
public class PedidoAsyncService {

    private final PedidoService pedidoService;

    public PedidoAsyncService(PedidoService pedidoService) {
        this.pedidoService = Objects.requireNonNull(pedidoService);
    }

    /**
     * Guarda un pedido en un hilo secundario.
     *
     * @param pedido pedido a guardar
     * @param codEmpleado empleado responsable (para auditoría)
     * @return CompletableFuture que completa cuando finaliza el guardado
     */
    public CompletableFuture<Void> registrarPedidoAsync(Pedido pedido, String codEmpleado) {
        Objects.requireNonNull(pedido, "pedido no puede ser null");

        return CompletableFuture.runAsync(() -> {
            pedidoService.guardar(pedido);
            AuditoriaLog.registrarAccion(codEmpleado, "Registró pedido " + pedido.getCodPedido());
        });
    }
}

