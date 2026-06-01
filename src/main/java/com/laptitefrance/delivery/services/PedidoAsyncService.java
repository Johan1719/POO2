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
     * @param cliente cliente que realiza el pedido
     * @param total monto total del pedido
     * @param codEmpleado empleado responsable (para auditoría)
     * @return CompletableFuture que completa cuando finaliza el guardado
     */
    public CompletableFuture<Void> crearPedidoAsync(com.laptitefrance.delivery.models.Cliente cliente, double total, String codEmpleado) {
        return CompletableFuture.runAsync(() -> {
            Pedido pedido = pedidoService.ensamblarNuevoPedido(cliente, total);
            pedidoService.guardar(pedido);
            
            String empleadoAuditoria = (codEmpleado == null || codEmpleado.isBlank()) ? "SISTEMA" : codEmpleado;
            AuditoriaLog.registrarAccion(empleadoAuditoria, "Registró pedido asíncrono " + pedido.getCodPedido());
        });
    }

    /**
     * Asigna un repartidor a un pedido de forma asíncrona y actualiza el estado.
     */
    public CompletableFuture<Void> asignarRepartidorAsincrono(String codPedido, String codRepartidor) {
        return CompletableFuture.runAsync(() -> {
            // Delegamos la actualización del estado al servicio principal
            // (En un sistema real también se grabaría el ID del repartidor en la BD)
            pedidoService.actualizarEstado(codPedido, "EN CAMINO");
            AuditoriaLog.registrarAccion("SISTEMA", "Asignado repartidor " + codRepartidor + " a pedido " + codPedido);
        });
    }
}
