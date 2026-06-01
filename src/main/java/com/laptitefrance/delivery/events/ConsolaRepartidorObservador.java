package com.laptitefrance.delivery.events;

import com.laptitefrance.delivery.models.Pedido;

/**
 * Observador de ejemplo (placeholder) para demostrar el Observer.
 */
public class ConsolaRepartidorObservador implements ObservadorPedidoEstado {

    private final String idRepartidor;

    public ConsolaRepartidorObservador(String idRepartidor) {
        this.idRepartidor = idRepartidor;
    }

    @Override
    public void actualizarEstado(Pedido pedido, String estadoAnterior, String nuevoEstado) {
        if (pedido == null) return;
        if (idRepartidor == null) return;

        // si el pedido está asignado a este repartidor, reacciona
        if (idRepartidor.equals(pedido.getCodRepartidor())) {
            System.out.printf(
                    "[Repartidor %s] Pedido %s cambió de %s a %s%n",
                    idRepartidor,
                    pedido.getCodPedido(),
                    estadoAnterior,
                    nuevoEstado
            );
        }
    }
}

