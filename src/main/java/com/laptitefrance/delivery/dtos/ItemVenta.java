package com.laptitefrance.delivery.dtos;

/** Ítem del carrito que viaja de la vista al repository para registrar una venta. */
public class ItemVenta {
    private final String codProducto;
    private final String nombreProducto;
    private final int cantidad;

    public ItemVenta(String codProducto, String nombreProducto, int cantidad) {
        this.codProducto = codProducto;
        this.nombreProducto = nombreProducto;
        this.cantidad = cantidad;
    }

    public String getCodProducto() {
        return codProducto;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public int getCantidad() {
        return cantidad;
    }
}
