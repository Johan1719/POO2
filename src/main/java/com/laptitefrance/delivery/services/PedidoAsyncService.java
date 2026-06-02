package com.laptitefrance.delivery.services;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.laptitefrance.delivery.audit.AuditoriaLog;
import com.laptitefrance.delivery.models.Pedido;

public class PedidoAsyncService {

    private final PedidoService pedidoService;

    public PedidoAsyncService(PedidoService pedidoService) {
        this.pedidoService = Objects.requireNonNull(pedidoService);
    }

    public CompletableFuture<Void> crearPedidoAsync(
            com.laptitefrance.delivery.models.Cliente cliente,
            double total,
            String direccionEntrega,
            String codTarifa,
            String codPago,
            String codAsistente
    ) {
        return CompletableFuture.runAsync(() -> {
            Pedido pedido = pedidoService.ensamblarNuevoPedido(cliente, total, direccionEntrega, codTarifa, codPago, codAsistente);
            pedidoService.guardar(pedido);

            String auditoriaActor = (codAsistente == null || codAsistente.isBlank()) ? "SISTEMA" : codAsistente;
            AuditoriaLog.registrarAccion(auditoriaActor, "Registró pedido asíncrono " + pedido.getCodPedido());
            
        }).exceptionally(ex -> {
            // Desenmascara el error en la consola
            System.err.println("\n==========================================");
            System.err.println("❌ ERROR GRAVE AL GUARDAR EN BASE DE DATOS:");
            System.err.println(ex.getMessage());
            ex.printStackTrace();
            System.err.println("==========================================\n");
            return null;
        });
    }

    public CompletableFuture<Void> asignarRepartidorAsincrono(String codPedido, String codRepartidor) {
        return CompletableFuture.runAsync(() -> {
            pedidoService.actualizarEstado(codPedido, "EN CAMINO");
            AuditoriaLog.registrarAccion("SISTEMA", "Asignado repartidor " + codRepartidor + " a pedido " + codPedido);
        });
    }
}