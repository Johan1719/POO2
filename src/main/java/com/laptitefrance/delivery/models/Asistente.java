package com.laptitefrance.delivery.models;

public class Asistente extends Empleado {
    public Asistente() {
        super();
    }

    public Asistente(String codEmpleado, String nombre, String numero, String direccion, String correoElec, Short aniosExp) {
        super(codEmpleado, nombre, numero, direccion, correoElec, aniosExp);
    }
}

