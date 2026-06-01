package com.laptitefrance.delivery.controllers;

import com.laptitefrance.delivery.exceptions.ValidationException;
import com.laptitefrance.delivery.models.Cliente;
import com.laptitefrance.delivery.models.Pedido;
import com.laptitefrance.delivery.services.PedidoAsyncService;
import com.laptitefrance.delivery.services.PedidoService;

import java.util.List;

public class PedidoController {

    private final PedidoService pedidoService;
    private final PedidoAsyncService pedidoAsyncService;

    public PedidoController() {
        // ESTRICTO: El controlador SÓLO instancia servicios, NUNCA repositorios
        this.pedidoService = new PedidoService();
        this.pedidoAsyncService = new PedidoAsyncService(this.pedidoService);
    }

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

        // Delegamos la creación asíncrona para no bloquear el EDT de Swing
        pedidoAsyncService.crearPedidoAsync(cliente, total, "SISTEMA");
    }

    public List<Pedido> listarPedidos() {
        return pedidoService.obtenerTodosLosPedidos();
    }

    public List<Pedido> filtrarPedidosPorEstado(String estado) {
        if (estado == null || estado.trim().isEmpty() || estado.equalsIgnoreCase("TODOS")) {
            return listarPedidos();
        }
        // El filtro lógico pesado con Streams/Lambdas ocurre dentro del servicio
        return pedidoService.obtenerPedidosPorEstadoOrdenados(estado.trim());
    }

    public void asignarRepartidor(String codPedido, String codRepartidor) {
        if (codPedido == null || codPedido.trim().isEmpty()) {
            throw new ValidationException("El código del pedido no puede estar vacío.");
        }
        if (codRepartidor == null || codRepartidor.trim().isEmpty()) {
            throw new ValidationException("El código del repartidor no puede estar vacío.");
        }
        // Delegamos la tarea asíncrona (CompletableFuture) al servicio concurrente
        pedidoAsyncService.asignarRepartidorAsincrono(codPedido, codRepartidor);
    }

    public void actualizarEstadoPedido(String codPedido, String nuevoEstado) {
        if (codPedido == null || codPedido.trim().isEmpty()) {
            throw new ValidationException("Debe seleccionar un pedido válido.");
        }
        if (nuevoEstado == null || nuevoEstado.trim().isEmpty()) {
            throw new ValidationException("Debe proporcionar un estado válido.");
        }
        // El controlador simplemente pasa la petición limpia al gerente (Servicio)
        pedidoService.actualizarEstado(codPedido, nuevoEstado.trim());
    }
}