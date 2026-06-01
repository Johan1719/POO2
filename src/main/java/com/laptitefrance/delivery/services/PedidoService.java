package com.laptitefrance.delivery.services;

import com.laptitefrance.delivery.models.Pedido;
import com.laptitefrance.delivery.repositories.IRepositorioBase;

import com.laptitefrance.delivery.repositories.PedidoRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Servicios de negocio para {@link Pedido}.
 *
 * <p>Implementa el requisito de Streams y Lambdas.</p>
 */
public class PedidoService {

    private final IRepositorioBase<Pedido, String> pedidoRepository;

    public PedidoService() {
        this.pedidoRepository = new PedidoRepository();
    }

    public PedidoService(IRepositorioBase<Pedido, String> pedidoRepository) {
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository);
    }

    public List<Pedido> obtenerPedidosPendientesPorCliente(String idCliente) {
        if (idCliente == null || idCliente.isBlank()) {
            return List.of();
        }

        return pedidoRepository.findAll().stream()
                .filter(p -> p != null)
                .filter(p -> "PENDIENTE".equalsIgnoreCase(p.getEstado()))
                .filter(p -> idCliente.equals(p.getIdCliente()))
                .sorted(Comparator.comparing(
                        Pedido::getFechaSolicitud,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .collect(Collectors.toList());
    }

    public List<Pedido> obtenerPedidosPorEstadoOrdenados(String estado) {
        if (estado == null || estado.isBlank()) {
            return List.of();
        }

        return pedidoRepository.findAll().stream()
                .filter(p -> p != null)
                .filter(p -> estado.equalsIgnoreCase(p.getEstado()))
                .sorted(Comparator.comparing(
                        Pedido::getFechaSolicitud,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Ejemplo de método adicional usando Streams/Lambdas.
     * Devuelve pedidos cuya fechaSolicitud esté dentro del rango.
     */
    public List<Pedido> obtenerPedidosSolicitadosEntre(LocalDateTime inicio, LocalDateTime fin) {
        return pedidoRepository.findAll().stream()
                .filter(p -> p != null)
                .filter(p -> p.getFechaSolicitud() != null)
                .filter(p -> (inicio == null || !p.getFechaSolicitud().isBefore(inicio))
                        && (fin == null || !p.getFechaSolicitud().isAfter(fin)))
                .collect(Collectors.toList());
    }

    public void guardar(Pedido pedido) {
        pedidoRepository.insert(pedido);
    }

    public Pedido ensamblarNuevoPedido(com.laptitefrance.delivery.models.Cliente cliente, double total) {
        Pedido pedido = new Pedido();
        // Límite estricto de 5 caracteres: 'P' + 4 dígitos (Ej: P0123 a P9999)
        pedido.setCodPedido(String.format("P%04d", (int) (Math.random() * 10000)));
        pedido.setIdCliente(cliente.getIdCliente());
        pedido.setMontoPedido(total);
        pedido.setEstado("PENDIENTE");
        pedido.setFechaSolicitud(LocalDateTime.now());
        return pedido;
    }

    public List<Pedido> obtenerTodosLosPedidos() {
        return pedidoRepository.findAll();
    }

    /**
     * Actualiza el estado del pedido y notifica a observadores.
     */
    public void actualizarEstado(String codPedido, String nuevoEstado) {
        if (codPedido == null || codPedido.isBlank()) {
            throw new IllegalArgumentException("codPedido no puede estar vacío");
        }
        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            throw new IllegalArgumentException("nuevoEstado no puede estar vacío");
        }

        Pedido pedido = pedidoRepository.findById(codPedido)
                .orElseThrow(() -> new IllegalArgumentException("No existe Pedido con codPedido=" + codPedido));

        String estadoAnterior = pedido.getEstado();
        pedido.setEstado(nuevoEstado);

        pedidoRepository.update(pedido);
        
        // En un entorno de Producción real, el Observable se instanciaría/injectaría 
        // de forma global internamente en el Servicio.
    }
}
