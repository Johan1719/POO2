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
import com.laptitefrance.delivery.models.PedidoBuilder;
import com.laptitefrance.delivery.repositories.IRepositorioBase;
import com.laptitefrance.delivery.repositories.PedidoRepository;
import com.laptitefrance.delivery.repositories.VentaRepository;

/**
 * Controlador de pedidos: orquesta la generación de ventas, el listado/paginado para el
 * monitor, la asignación de repartidor y los cambios de estado (incluida la cancelación
 * con devolución de stock). Las vistas le piden acciones; él coordina los repositorios.
 */
public class PedidoController {

    private final IRepositorioBase<Pedido, String> pedidoRepository;

    // Estado inyectado: el código del cajero/asistente que tiene la sesión abierta.
    // Así cada pedido queda asociado a quién lo registró, sin que la vista lo tenga que mandar.
    private final String codCajeroActivo;

    public PedidoController(String codCajeroActivo) {
        this(codCajeroActivo, new PedidoRepository());
    }

    public PedidoController(String codCajeroActivo, IRepositorioBase<Pedido, String> pedidoRepository) {
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository);
        this.codCajeroActivo = codCajeroActivo;
    }

    /**
     * Genera un pedido completo a partir del carrito.
     *
     * Lógica:
     *  1) Valida los datos (cliente, ítems, total, tarifa, pago, y dirección solo si NO es recojo).
     *  2) Consolida los ítems por producto: si el mismo producto vino en dos filas, suma sus
     *     cantidades. Esto evita romper la clave primaria de Pedido_Producto (producto+pedido).
     *  3) Arma el objeto Pedido con el patrón Builder.
     *  4) Delega en VentaRepository, que en UNA transacción inserta el pedido, sus ítems y
     *     descuenta el stock; devuelve los productos que quedaron en 0 para avisar.
     *
     * @return nombres de productos cuyo stock llegó a 0 (para sugerir reabastecer).
     */
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



    /**
     * Indica si el pedido es de recojo en tienda (no requiere repartidor).
     * Se detecta por la dirección descriptiva o, por robustez, por la tarifa.
     */
    public boolean esPedidoDeRecojo(String codPedido) {
        if (codPedido == null || codPedido.trim().isEmpty()) {
            return false;
        }
        Pedido pedido = pedidoRepository.findById(codPedido.trim()).orElse(null);
        if (pedido == null) {
            return false;
        }

        String dir = pedido.getDireccionEntrega();
        if (dir != null && dir.trim().toUpperCase().startsWith("RECOJO EN TIENDA")) {
            return true;
        }

        String codTarifa = pedido.getCodTarifa();
        if (codTarifa != null && !codTarifa.trim().isEmpty()) {
            com.laptitefrance.delivery.models.Tarifa tarifa =
                    new com.laptitefrance.delivery.repositories.TarifaRepository()
                            .findById(codTarifa.trim())
                            .orElse(null);
            if (tarifa != null) {
                return tarifa.esRecojo();
            }
        }
        return false;
    }

    /**
     * Asigna un repartidor a un pedido y lo pone "EN CAMINO".
     *
     * Lógica: se ejecuta de forma ASÍNCRONA (CompletableFuture) para no congelar la ventana
     * mientras se actualiza la base. Además del estado, calcula la hora estimada de entrega
     * sumando el tiempo promedio de la zona/tarifa, y marca la hora de despacho.
     */
    public void asignarRepartidor(String codPedido, String codRepartidor) {
        if (codPedido == null || codPedido.trim().isEmpty()) {
            throw new ValidationException("El código del pedido no puede estar vacío.");
        }
        if (codRepartidor == null || codRepartidor.trim().isEmpty()) {
            throw new ValidationException("El código del repartidor no puede estar vacío.");
        }

        // Se corre en segundo plano para no bloquear la interfaz (la UI sigue respondiendo).
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

    /**
     * Cambia el estado de un pedido y ajusta el stock según corresponda.
     *
     * Lógica clave: el stock se descontó al generar el pedido, así que la cancelación debe
     * DEVOLVERLO y la reactivación volver a descontarlo. Por eso se compara el estado anterior
     * con el nuevo y se decide:
     *  - Activo → CANCELADO  : se repone el stock de los productos del pedido.
     *  - CANCELADO → activo  : se vuelve a descontar (validando que alcance).
     *  - Cualquier otro cambio: solo se actualiza el estado.
     */
    public void actualizarEstadoPedido(String codPedido, String nuevoEstado) {
        if (codPedido == null || codPedido.trim().isEmpty()) {
            throw new ValidationException("Debe seleccionar un pedido válido.");
        }
        if (nuevoEstado == null || nuevoEstado.trim().isEmpty()) {
            throw new ValidationException("Debe proporcionar un estado válido.");
        }

        String cod = codPedido.trim();
        String nuevo = nuevoEstado.trim();

        Pedido pedido = pedidoRepository.findById(cod)
                .orElseThrow(() -> new ValidationException("No existe Pedido con codPedido=" + cod));

        // Detecta si el cambio cruza el límite "CANCELADO" en uno u otro sentido.
        String estadoAnterior = pedido.getEstado() == null ? "" : pedido.getEstado().trim();
        boolean eraCancelado = estadoAnterior.equalsIgnoreCase("CANCELADO");
        boolean seraCancelado = nuevo.equalsIgnoreCase("CANCELADO");

        VentaRepository ventaRepository = new VentaRepository();

        if (!eraCancelado && seraCancelado) {
            // Activo -> CANCELADO: devolver stock.
            ventaRepository.reponerStockPorCancelacion(cod, nuevo);
        } else if (eraCancelado && !seraCancelado) {
            // CANCELADO -> activo: volver a descontar stock (valida y bloquea si falta).
            ventaRepository.descontarStockPorReactivacion(cod, nuevo);
        } else {
            // Transición que no cruza el límite de CANCELADO: solo actualizar estado.
            pedido.setEstado(nuevo);
            pedidoRepository.update(pedido);
        }
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
        // CodPedido se autogenera en SQL (SEQUENCE/DEFAULT); repartidor/tiempos aún desconocidos.
        return new PedidoBuilder()
                .codPedido(null)
                .idCliente(cliente.getIdCliente())
                .montoPedido(total)
                .estado("EN ESPERA")
                .fechaSolicitud(LocalDateTime.now())
                .direccionEntrega(direccionEntrega)
                .codAsistente(codAsistente)
                .codTarifa(codTarifa)
                .codPago(codPago)
                .codRepartidor(null)
                .tiempoEntEstimado(null)
                .tiempoEntReal(null)
                .build();
    }
}
