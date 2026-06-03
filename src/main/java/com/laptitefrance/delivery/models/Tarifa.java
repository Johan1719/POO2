package com.laptitefrance.delivery.models;

public class Tarifa {
    private String codTarifa; // CHAR(3)
    private String nombreZona; // NombreZona VARCHAR(30)
    private double precioTarifa;
    private int tiempoPromedio;

    public Tarifa() {
    }

    public Tarifa(String codTarifa, String nombreZona, double precioTarifa, int tiempoPromedio) {
        this.codTarifa = codTarifa;
        this.nombreZona = nombreZona;
        this.precioTarifa = precioTarifa;
        this.tiempoPromedio = tiempoPromedio;
    }

    public String getCodTarifa() {
        return codTarifa;
    }

    public void setCodTarifa(String codTarifa) {
        this.codTarifa = codTarifa;
    }

    public String getNombreZona() {
        return nombreZona;
    }

    public void setNombreZona(String nombreZona) {
        this.nombreZona = nombreZona;
    }

    public double getPrecioTarifa() {
        return precioTarifa;
    }

    public void setPrecioTarifa(double precioTarifa) {
        this.precioTarifa = precioTarifa;
    }

    public int getTiempoPromedio() {
        return tiempoPromedio;
    }

    public void setTiempoPromedio(int tiempoPromedio) {
        this.tiempoPromedio = tiempoPromedio;
    }

    @Override
    public String toString() {
        return codTarifa + " - " + nombreZona + " (S/ " + precioTarifa + ")";
    }
}
