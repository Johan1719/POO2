package com.laptitefrance.delivery.models;

import java.time.LocalDateTime;

public class Pago {
    private String codPago; // CHAR(5)
    private String metodoPago;
    private LocalDateTime fechaPago;
    private double montoTotal;
    private String observaciones;
    private double cantDesc;
    private double igv;
    private double costoTarifa;

    public Pago() {
    }

    public Pago(String codPago, String metodoPago, LocalDateTime fechaPago, double montoTotal, String observaciones, double cantDesc, double igv, double costoTarifa) {
        this.codPago = codPago;
        this.metodoPago = metodoPago;
        this.fechaPago = fechaPago;
        this.montoTotal = montoTotal;
        this.observaciones = observaciones;
        this.cantDesc = cantDesc;
        this.igv = igv;
        this.costoTarifa = costoTarifa;
    }

    public String getCodPago() {
        return codPago;
    }

    public void setCodPago(String codPago) {
        this.codPago = codPago;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public double getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(double montoTotal) {
        this.montoTotal = montoTotal;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public double getCantDesc() {
        return cantDesc;
    }

    public void setCantDesc(double cantDesc) {
        this.cantDesc = cantDesc;
    }

    public double getIgv() {
        return igv;
    }

    public void setIgv(double igv) {
        this.igv = igv;
    }

    public double getCostoTarifa() {
        return costoTarifa;
    }

    public void setCostoTarifa(double costoTarifa) {
        this.costoTarifa = costoTarifa;
    }
}

