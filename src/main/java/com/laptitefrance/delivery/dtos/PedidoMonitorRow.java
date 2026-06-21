package com.laptitefrance.delivery.dtos;

import java.time.LocalDateTime;

/**
 * DTO/Row de la proyección del monitor de pedidos.
 */
public class PedidoMonitorRow {
    public String codPedido;
    public double montoPedido;
    public String estado;
    public LocalDateTime fechaSolicitud;
    public LocalDateTime tiempoEntEstimado;
    public String nombreCliente;
    public String nombreRepartidor;
}

