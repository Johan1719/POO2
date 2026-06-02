package com.laptitefrance.delivery.controllers;

import java.util.List;

import com.laptitefrance.delivery.exceptions.ValidationException;
import com.laptitefrance.delivery.models.Cliente;
import com.laptitefrance.delivery.models.Pedido;
import com.laptitefrance.delivery.services.PedidoAsyncService;
import com.laptitefrance.delivery.services.PedidoService;

public class PedidoController {

    private final PedidoService pedidoService;
    private final PedidoAsyncService pedidoAsyncService;
    
    // 👇 ESTADO INYECTADO: El controlador sabe quién opera la caja
    private final String codCajeroActivo;

    public PedidoController(String codCajeroActivo) {
        this.pedidoService = new PedidoService();
        this.pedidoAsyncService = new PedidoAsyncService(this.pedidoService);
        this.codCajeroActivo = codCajeroActivo;
    }

    // 👇 VISTA TONTA: Ya no pedimos el 'codAsistente' en los parámetros
    public void generarPedido(
            Cliente cliente,
            int cantidadProductosEnCarrito,
            double total,
            String direccionEntrega,
            String codTarifa,
            String codPago
    ) {
        if (cliente == null) {
            throw new ValidationException("Debe seleccionar un cliente.");
        }
        if (cantidadProductosEnCarrito == 0) {
            throw new ValidationException("Debe agregar productos al carrito.");
        }
        if (total <= 0) {
            throw new ValidationException("El total del pedido debe ser mayor a 0.");
        }
        if (direccionEntrega == null || direccionEntrega.trim().isEmpty()) {
            throw new ValidationException("La dirección de entrega no puede estar vacía.");
        }
        if (codTarifa == null || codTarifa.trim().isEmpty()) {
            throw new ValidationException("Debe seleccionar una tarifa.");
        }
        if (codPago == null || codPago.trim().isEmpty()) {
            throw new ValidationException("Debe seleccionar un método de pago.");
        }

        // --- TRUCO TEMPORAL DE DEBUGGING ---
        // 1. Apagamos el hilo asíncrono comentando esta línea:
        // pedidoAsyncService.crearPedidoAsync(cliente, total, direccionEntrega, codTarifa, codPago, this.codCajeroActivo);
        
        // 2. Encendemos el guardado directo (Síncrono) inyectando nuestro propio estado (this.codCajeroActivo):
        Pedido pedido = pedidoService.ensamblarNuevoPedido(cliente, total, direccionEntrega, codTarifa, codPago, this.codCajeroActivo);
        pedidoService.guardar(pedido);
    }

    public List<Pedido> listarPedidos() {
        return pedidoService.obtenerTodosLosPedidos();
    }

    public List<Pedido> filtrarPedidosPorEstado(String estado) {
        if (estado == null || estado.trim().isEmpty() || estado.equalsIgnoreCase("TODOS")) {
            return listarPedidos();
        }
        return pedidoService.obtenerPedidosPorEstadoOrdenados(estado.trim());
    }

    public void asignarRepartidor(String codPedido, String codRepartidor) {
        if (codPedido == null || codPedido.trim().isEmpty()) {
            throw new ValidationException("El código del pedido no puede estar vacío.");
        }
        if (codRepartidor == null || codRepartidor.trim().isEmpty()) {
            throw new ValidationException("El código del repartidor no puede estar vacío.");
        }
        pedidoAsyncService.asignarRepartidorAsincrono(codPedido, codRepartidor);
    }

    public void actualizarEstadoPedido(String codPedido, String nuevoEstado) {
        if (codPedido == null || codPedido.trim().isEmpty()) {
            throw new ValidationException("Debe seleccionar un pedido válido.");
        }
        if (nuevoEstado == null || nuevoEstado.trim().isEmpty()) {
            throw new ValidationException("Debe proporcionar un estado válido.");
        }
        pedidoService.actualizarEstado(codPedido, nuevoEstado.trim());
    }
}