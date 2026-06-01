package com.laptitefrance.delivery.events;

import com.laptitefrance.delivery.models.Pedido;

/**
 * Observador que reacciona a cambios de estado de un Pedido.
 */
public interface ObservadorPedidoEstado {
    void actualizarEstado(Pedido pedido, String estadoAnterior, String nuevoEstado);
}

