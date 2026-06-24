package com.laptitefrance.delivery.models;

import java.time.LocalDateTime;

public class Pedido {
    private String codPedido; // CHAR(5)
    private LocalDateTime fechaSolicitud;
    private double montoPedido;
    private String estado;
    private LocalDateTime tiempoEntEstimado;
    private LocalDateTime tiempoEntReal;

    // Hora en la que se despacha (asigna repartidor)
    private LocalDateTime horaEnvio;

    // FK actuales
    private String codAsistente; // FK -> Asistente(CodAsistente)
    private String codRepartidor; // FK -> Repartidor(CodRepartidor)
    private String idCliente; // FK -> Cliente(IDCliente)
    private String codTarifa; // FK -> Tarifa(CodTarifa)
    private String codPago; // FK -> Pago(CodPago)

    // DATOS ABSORBIDOS
    private String direccionEntrega; // DireccionEntrega VARCHAR(100)


    public Pedido() {
    }

    public String getCodPedido() {
        return codPedido;
    }

    public void setCodPedido(String codPedido) {
        this.codPedido = codPedido;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public double getMontoPedido() {
        return montoPedido;
    }

    public void setMontoPedido(double montoPedido) {
        this.montoPedido = montoPedido;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getTiempoEntEstimado() {
        return tiempoEntEstimado;
    }

    public void setTiempoEntEstimado(LocalDateTime tiempoEntEstimado) {
        this.tiempoEntEstimado = tiempoEntEstimado;
    }

    public LocalDateTime getTiempoEntReal() {
        return tiempoEntReal;
    }

    public void setTiempoEntReal(LocalDateTime tiempoEntReal) {
        this.tiempoEntReal = tiempoEntReal;
    }

    public String getCodAsistente() {
        return codAsistente;
    }

    public void setCodAsistente(String codAsistente) {
        this.codAsistente = codAsistente;
    }

    public String getCodRepartidor() {
        return codRepartidor;
    }

    public void setCodRepartidor(String codRepartidor) {
        this.codRepartidor = codRepartidor;
    }


    public String getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(String idCliente) {
        this.idCliente = idCliente;
    }

    public String getCodTarifa() {
        return codTarifa;
    }

    public void setCodTarifa(String codTarifa) {
        this.codTarifa = codTarifa;
    }

    public String getCodPago() {
        return codPago;
    }

    public void setCodPago(String codPago) {
        this.codPago = codPago;
    }

    public String getDireccionEntrega() {
        return direccionEntrega;
    }

    public void setDireccionEntrega(String direccionEntrega) {
        this.direccionEntrega = direccionEntrega;
    }

    public LocalDateTime getHoraEnvio() {
        return horaEnvio;
    }

    public void setHoraEnvio(LocalDateTime horaEnvio) {
        this.horaEnvio = horaEnvio;
    }
}

