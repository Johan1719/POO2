package com.laptitefrance.delivery.models;

public class Repartidor extends Empleado {
    public Repartidor() {
        super();
    }

    public Repartidor(String codEmpleado, String nombre, String numero, String direccion, String correoElec, Short aniosExp) {
        super(codEmpleado, nombre, numero, direccion, correoElec, aniosExp);
    }
}

