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
