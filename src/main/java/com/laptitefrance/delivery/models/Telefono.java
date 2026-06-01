package com.laptitefrance.delivery.models;

public class Telefono {
    private Integer idTelefono; // IDENTITY(1,1)
    private String codEmpleado; // CHAR(4)
    private String nroTelefono; // VARCHAR(15)

    public Telefono() {
    }

    public Telefono(Integer idTelefono, String codEmpleado, String nroTelefono) {
        this.idTelefono = idTelefono;
        this.codEmpleado = codEmpleado;
        this.nroTelefono = nroTelefono;
    }

    public Integer getIdTelefono() {
        return idTelefono;
    }

    public void setIdTelefono(Integer idTelefono) {
        this.idTelefono = idTelefono;
    }

    public String getCodEmpleado() {
        return codEmpleado;
    }

    public void setCodEmpleado(String codEmpleado) {
        this.codEmpleado = codEmpleado;
    }

    public String getNroTelefono() {
        return nroTelefono;
    }

    public void setNroTelefono(String nroTelefono) {
        this.nroTelefono = nroTelefono;
    }
}

