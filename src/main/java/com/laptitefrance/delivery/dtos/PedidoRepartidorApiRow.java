package com.laptitefrance.delivery.dtos;

import java.time.LocalDateTime;

public class PedidoRepartidorApiRow {
    public String codPedido;
    public double montoPedido;
    public String estado;

    // Ubicación para la vista (sale de DireccionEntrega)
    public String ubicacionEntrega;

    // Compatibilidad semántica
    public String direccionEntrega;

    public LocalDateTime fechaSolicitud;
    public LocalDateTime tiempoEntEstimado;

    public String nombreCliente;
}

