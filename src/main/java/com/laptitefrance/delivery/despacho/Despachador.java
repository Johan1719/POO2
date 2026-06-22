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
