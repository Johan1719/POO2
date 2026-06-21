package com.laptitefrance.delivery.dtos;

public class RepartidorMonitorRow {
    public String codRepartidor;
    public String nombreRepartidor;
    public int pedidosEnCamino;
    public int pedidosEntregados;
    public String estado;

    @Override
    public String toString() {
        return nombreRepartidor + " | " + estado + " | En camino: " + pedidosEnCamino + " | Entregados: " + pedidosEntregados;
    }
}

