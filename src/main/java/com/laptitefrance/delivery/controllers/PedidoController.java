package com.laptitefrance.delivery.controllers;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.laptitefrance.delivery.dtos.ItemVenta;
import com.laptitefrance.delivery.exceptions.ValidationException;
import com.laptitefrance.delivery.models.Cliente;
import com.laptitefrance.delivery.models.Pedido;
import com.laptitefrance.delivery.repositories.IRepositorioBase;
import com.laptitefrance.delivery.repositories.PedidoRepository;
import com.laptitefrance.delivery.repositories.VentaRepository;

public class PedidoController {

    private final IRepositorioBase<Pedido, String> pedidoRepository;

    // 👇 ESTADO INYECTADO: El controlador sabe quién opera la caja
    private final String codCajeroActivo;

    public PedidoController(String codCajeroActivo) {
        this(codCajeroActivo, new PedidoRepository());
    }

    public PedidoController(String codCajeroActivo, IRepositorioBase<Pedido, String> pedidoRepository) {
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository);
        this.codCajeroActivo = codCajeroActivo;
    }

    // 👇 VISTA TONTA: Ya no existe capa Service.
    public List<String> generarPedido(
            Cliente cliente,
            List<ItemVenta> items,
            double total,
            String direccionEntrega,
            String codTarifa,
            String codPago,
            boolean esRecojo
    ) {
        validarDatosGeneracion(cliente, items, total, direccionEntrega, codTarifa, codPago, esRecojo);

        // Consolidar ítems por producto (evita violar la PK de Pedido_Producto y suma cantidades).
        Map<String, ItemVenta> consolidados = new LinkedHashMap<>();
        for (ItemVenta it : items) {
            ItemVenta previo = consolidados.get(it.getCodProducto());
            if (previo == null) {
                consolidados.put(it.getCodProducto(), it);
            } else {
                consolidados.put(it.getCodProducto(),
                        new ItemVenta(it.getCodProducto(), it.getNombreProducto(),
                                previo.getCantidad() + it.getCantidad()));
            }
        }

        Pedido pedido = ensamblarNuevoPedido(cliente, total, direccionEntrega, codTarifa, codPago, this.codCajeroActivo);
        return new VentaRepository().registrarVenta(pedido, new java.util.ArrayList<>(consolidados.values()));
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    public List<Pedido> filtrarPedidosPorEstado(String estado) {
        // Se mantiene compatibilidad con el panel viejo (List<Pedido>).
        if (estado == null || estado.trim().isEmpty() || estado.equalsIgnoreCase("TODOS")) {
            return listarPedidos();
        }

        String estadoNormalizado = estado.trim();
        return pedidoRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(p -> estadoNormalizado.equalsIgnoreCase(p.getEstado()))
                .sorted(Comparator.comparing(Pedido::getFechaSolicitud, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    // ===================== Paginación real para el monitor =====================
    // Monitor: devolvemos el DTO de la proyección usada por PanelMonitorPedidos.
    // (PanelMonitorPedidos sigue siendo "vista tonta": solo renderiza lo que recibe.)
    public List<com.laptitefrance.delivery.dtos.PedidoMonitorRow> listarPedidosMonitorPaginado(String estado, int page, int pageSize) {
        return com.laptitefrance.delivery.repositories.PedidoMonitorRepositoryPagination.listarPedidosPaginado(estado, page, pageSize);
    }





    public int contarPedidosMonitorFiltrados(String estado) {
        return com.laptitefrance.delivery.repositories.PedidoMonitorRepositoryPagination.contarPedidosFiltrados(estado);
    }



    public void asignarRepartidor(String codPedido, String codRepartidor) {
        if (codPedido == null || codPedido.trim().isEmpty()) {
            throw new ValidationException("El código del pedido no puede estar vacío.");
        }
        if (codRepartidor == null || codRepartidor.trim().isEmpty()) {
            throw new ValidationException("El código del repartidor no puede estar vacío.");
        }

        // Mantener comportamiento asíncrono, pero sin Service.
        CompletableFuture.runAsync(() -> {
            // 1) Actualizar estado y datos del pedido en BD si aplica.
            Pedido pedido = pedidoRepository.findById(codPedido)
                    .orElseThrow(() -> new ValidationException("No existe Pedido con codPedido=" + codPedido));

            pedido.setEstado("EN CAMINO");
            pedido.setCodRepartidor(codRepartidor);

            // Calcular tiempo estimado en base a la tarifa/zona del pedido.
            // Tarifa tiene tiempoPromedio (minutos).
            if (pedido.getCodTarifa() != null && !pedido.getCodTarifa().trim().isEmpty()) {
                com.laptitefrance.delivery.models.Tarifa tarifa =
                        new com.laptitefrance.delivery.repositories.TarifaRepository()
                                .findById(pedido.getCodTarifa().trim())
                                .orElse(null);

                if (tarifa != null && tarifa.getTiempoPromedio() > 0) {
                    pedido.setTiempoEntEstimado(LocalDateTime.now().plusMinutes(tarifa.getTiempoPromedio()));
                }
            }

            // Hora de despacho (opcional): cuando asignas repartidor
            pedido.setHoraEnvio(LocalDateTime.now());

            pedidoRepository.update(pedido);



        }).exceptionally(ex -> {
            System.err.println("\n==========================================");
            System.err.println("❌ ERROR GRAVE AL ASIGNAR REPARTIDOR:");
            ex.printStackTrace();
            System.err.println("==========================================\n");
            return null;
        });
    }

    public void actualizarEstadoPedido(String codPedido, String nuevoEstado) {
        if (codPedido == null || codPedido.trim().isEmpty()) {
            throw new ValidationException("Debe seleccionar un pedido válido.");
        }
        if (nuevoEstado == null || nuevoEstado.trim().isEmpty()) {
            throw new ValidationException("Debe proporcionar un estado válido.");
        }

        Pedido pedido = pedidoRepository.findById(codPedido.trim())
                .orElseThrow(() -> new ValidationException("No existe Pedido con codPedido=" + codPedido));

        pedido.setEstado(nuevoEstado.trim());
        pedidoRepository.update(pedido);


    }

    private static void validarDatosGeneracion(
            Cliente cliente,
            List<ItemVenta> items,
            double total,
            String direccionEntrega,
            String codTarifa,
            String codPago,
            boolean esRecojo
    ) {
        if (cliente == null) {
            throw new ValidationException("Debe seleccionar un cliente.");
        }
        if (items == null || items.isEmpty()) {
            throw new ValidationException("Debe agregar productos al carrito.");
        }
        if (total <= 0) {
            throw new ValidationException("El total del pedido debe ser mayor a 0.");
        }
        if (!esRecojo && (direccionEntrega == null || direccionEntrega.trim().isEmpty())) {
            throw new ValidationException("La dirección de entrega no puede estar vacía.");
        }
        if (codTarifa == null || codTarifa.trim().isEmpty()) {
            throw new ValidationException("Debe seleccionar una tarifa.");
        }
        if (codPago == null || codPago.trim().isEmpty()) {
            throw new ValidationException("Debe seleccionar un método de pago.");
        }
    }

    private static Pedido ensamblarNuevoPedido(
            Cliente cliente,
            double total,
            String direccionEntrega,
            String codTarifa,
            String codPago,
            String codAsistente
    ) {
        Pedido pedido = new Pedido();

        // 1. Código único -> se autogenera en SQL mediante SEQUENCE/DEFAULT.
        // Importante: el repository insert NO debe setear CodPedido manualmente.
        pedido.setCodPedido(null);


        // 2. Datos básicos
        pedido.setIdCliente(cliente.getIdCliente());
        pedido.setMontoPedido(total);
        pedido.setEstado("EN ESPERA");
        pedido.setFechaSolicitud(LocalDateTime.now());

        // 3. Dirección
        pedido.setDireccionEntrega(direccionEntrega);

        // 4. FKs y actor
        pedido.setCodAsistente(codAsistente);
        pedido.setCodTarifa(codTarifa);
        pedido.setCodPago(codPago);

        // 5. Repartidor/tiempos aún desconocidos
        pedido.setCodRepartidor(null);
        pedido.setTiempoEntEstimado(null);
        pedido.setTiempoEntReal(null);

        return pedido;
    }
}
