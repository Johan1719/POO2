package com.laptitefrance.delivery.events;

import com.laptitefrance.delivery.models.Pedido;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observable para cambios de estado de un Pedido.
 */
public class PedidoEstadoObservable {

    private final List<ObservadorPedidoEstado> observadores = new CopyOnWriteArrayList<>();

    public void addObservador(ObservadorPedidoEstado observador) {
        observadores.add(Objects.requireNonNull(observador));
    }

    public void removeObservador(ObservadorPedidoEstado observador) {
        observadores.remove(observador);
    }

    public void notificarEstado(Pedido pedido, String estadoAnterior, String nuevoEstado) {
        for (ObservadorPedidoEstado o : observadores) {
            o.actualizarEstado(pedido, estadoAnterior, nuevoEstado);
        }
    }
}

