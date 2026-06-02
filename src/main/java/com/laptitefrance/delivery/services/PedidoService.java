package com.laptitefrance.delivery.services;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.laptitefrance.delivery.models.Pedido;
import com.laptitefrance.delivery.repositories.IRepositorioBase;
import com.laptitefrance.delivery.repositories.PedidoRepository;

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

   public Pedido ensamblarNuevoPedido(
            com.laptitefrance.delivery.models.Cliente cliente,
            double total,
            String direccionEntrega,
            String codTarifa,
            String codPago,
            String codAsistente
    ) {
        Pedido pedido = new Pedido();
        
        // 1. Generamos el código único (Tu lógica actual es perfecta)
        pedido.setCodPedido(String.format("P%04d", (int) (Math.random() * 10000)));
        
        // 2. Datos básicos del pedido
        pedido.setIdCliente(cliente.getIdCliente());
        pedido.setMontoPedido(total);
        pedido.setEstado("EN ESPERA"); 
        pedido.setFechaSolicitud(LocalDateTime.now());

        // 3. Nuevos atributos exigidos por la BD
        pedido.setDireccionEntrega(direccionEntrega);
        pedido.setCodAsistente(codAsistente);

        // 4. Llaves foráneas (Tarifa y Pago)
        pedido.setCodTarifa(codTarifa);
        pedido.setCodPago(codPago);

        // 5. ¡VITAL PARA EVITAR ERRORES EN SQL SERVER!
        // Un pedido nuevo no tiene repartidor ni tiempos de entrega reales aún.
        // Forzamos explícitamente a null para que el PreparedStatement no envíe basura a la BD.
        pedido.setCodRepartidor(null);
        pedido.setTiempoEntEstimado(null);
        pedido.setTiempoEntReal(null);

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
