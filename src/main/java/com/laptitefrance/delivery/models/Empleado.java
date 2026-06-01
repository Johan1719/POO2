package com.laptitefrance.delivery.models;

public class Empleado {
    private String codEmpleado; // CHAR(4)
    private String nombre;
    private String numero; // CHAR(9)
    private String direccion;
    private String correoElec;
    private Short aniosExp;

    public Empleado() {
    }

    public Empleado(String codEmpleado, String nombre, String numero, String direccion, String correoElec, Short aniosExp) {
        this.codEmpleado = codEmpleado;
        this.nombre = nombre;
        this.numero = numero;
        this.direccion = direccion;
        this.correoElec = correoElec;
        this.aniosExp = aniosExp;
    }

    public String getCodEmpleado() {
        return codEmpleado;
    }

    public void setCodEmpleado(String codEmpleado) {
        this.codEmpleado = codEmpleado;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getCorreoElec() {
        return correoElec;
    }

    public void setCorreoElec(String correoElec) {
        this.correoElec = correoElec;
    }

    public Short getAniosExp() {
        return aniosExp;
    }

    public void setAniosExp(Short aniosExp) {
        this.aniosExp = aniosExp;
    }
}

