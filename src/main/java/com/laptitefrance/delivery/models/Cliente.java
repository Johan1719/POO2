package com.laptitefrance.delivery.models;

import java.time.LocalDateTime;

public class Cliente {
    private String idCliente; // CHAR(4)
    private LocalDateTime fechaRegistro;
    private String nombreCliente;
    private String nrocelular; // CHAR(9) UNIQUE

    public Cliente() {
    }

    public Cliente(String idCliente, LocalDateTime fechaRegistro, String nombreCliente, String nrocelular) {
        this.idCliente = idCliente;
        this.fechaRegistro = fechaRegistro;
        this.nombreCliente = nombreCliente;
        this.nrocelular = nrocelular;
    }

    public String getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(String idCliente) {
        this.idCliente = idCliente;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getNrocelular() {
        return nrocelular;
    }

    public void setNrocelular(String nrocelular) {
        this.nrocelular = nrocelular;
    }
}

