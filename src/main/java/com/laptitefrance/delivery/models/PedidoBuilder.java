package com.laptitefrance.delivery.models;

import java.time.LocalDateTime;

/** Builder (patrón creacional) para construir un Pedido de forma limpia. */
public class PedidoBuilder {
    private String codPedido;
    private LocalDateTime fechaSolicitud;
    private double montoPedido;
    private String estado;
    private LocalDateTime tiempoEntEstimado;
    private LocalDateTime tiempoEntReal;
    private String codAsistente;
    private String codRepartidor;
    private String idCliente;
    private String codTarifa;
    private String codPago;
    private String direccionEntrega;

    public PedidoBuilder codPedido(String codPedido) { this.codPedido = codPedido; return this; }
    public PedidoBuilder fechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; return this; }
    public PedidoBuilder montoPedido(double montoPedido) { this.montoPedido = montoPedido; return this; }
    public PedidoBuilder estado(String estado) { this.estado = estado; return this; }
    public PedidoBuilder tiempoEntEstimado(LocalDateTime tiempoEntEstimado) { this.tiempoEntEstimado = tiempoEntEstimado; return this; }
    public PedidoBuilder tiempoEntReal(LocalDateTime tiempoEntReal) { this.tiempoEntReal = tiempoEntReal; return this; }
    public PedidoBuilder codAsistente(String codAsistente) { this.codAsistente = codAsistente; return this; }
    public PedidoBuilder codRepartidor(String codRepartidor) { this.codRepartidor = codRepartidor; return this; }
    public PedidoBuilder idCliente(String idCliente) { this.idCliente = idCliente; return this; }
    public PedidoBuilder codTarifa(String codTarifa) { this.codTarifa = codTarifa; return this; }
    public PedidoBuilder codPago(String codPago) { this.codPago = codPago; return this; }
    public PedidoBuilder direccionEntrega(String direccionEntrega) { this.direccionEntrega = direccionEntrega; return this; }

    public Pedido build() {
        Pedido p = new Pedido();
        p.setCodPedido(codPedido);
        p.setFechaSolicitud(fechaSolicitud);
        p.setMontoPedido(montoPedido);
        p.setEstado(estado);
        p.setTiempoEntEstimado(tiempoEntEstimado);
        p.setTiempoEntReal(tiempoEntReal);
        p.setCodAsistente(codAsistente);
        p.setCodRepartidor(codRepartidor);
        p.setIdCliente(idCliente);
        p.setCodTarifa(codTarifa);
        p.setCodPago(codPago);
        p.setDireccionEntrega(direccionEntrega);
        return p;
    }
}
