package com.laptitefrance.delivery.controllers;

import com.laptitefrance.delivery.exceptions.ValidationException;
import com.laptitefrance.delivery.models.Cliente;

public class PedidoController {

    public void generarPedido(Cliente cliente, int cantidadProductosEnCarrito, double total) {
        if (cliente == null) {
            throw new ValidationException("Debe seleccionar un cliente.");
        }

        if (cantidadProductosEnCarrito == 0) {
            throw new ValidationException("Debe agregar productos al carrito.");
        }

        if (total <= 0) {
            throw new ValidationException("El total del pedido debe ser mayor a 0.");
        }

        // Aquí en el futuro se llamaría al servicio/repositorio para grabar en la BD.
        // Ej: pedidoRepository.insert(nuevoPedido);
    }
}