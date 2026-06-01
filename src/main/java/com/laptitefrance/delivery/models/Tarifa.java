package com.laptitefrance.delivery.models;

public class Tarifa {
    private String codTarifa; // CHAR(3)
    private String nombreTarifa;
    private double precioTarifa;

    public Tarifa() {
    }

    public Tarifa(String codTarifa, String nombreTarifa, double precioTarifa) {
        this.codTarifa = codTarifa;
        this.nombreTarifa = nombreTarifa;
        this.precioTarifa = precioTarifa;
    }

    public String getCodTarifa() {
        return codTarifa;
    }

    public void setCodTarifa(String codTarifa) {
        this.codTarifa = codTarifa;
    }

    public String getNombreTarifa() {
        return nombreTarifa;
    }

    public void setNombreTarifa(String nombreTarifa) {
        this.nombreTarifa = nombreTarifa;
    }

    public double getPrecioTarifa() {
        return precioTarifa;
    }

    public void setPrecioTarifa(double precioTarifa) {
        this.precioTarifa = precioTarifa;
    }
}

