package com.laptitefrance.delivery.controllers;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.laptitefrance.delivery.audit.AuditoriaLog;
import com.laptitefrance.delivery.exceptions.ValidationException;
import com.laptitefrance.delivery.models.Cliente;
import com.laptitefrance.delivery.models.Pedido;
import com.laptitefrance.delivery.repositories.IRepositorioBase;
import com.laptitefrance.delivery.repositories.PedidoRepository;

/**
 * CONTROLLER central de pedidos: el cerebro del flujo de delivery.
 *
 * Concentra TODA la lógica de negocio relacionada con pedidos:
 *   - generar un pedido nuevo (validar + ensamblar + guardar),
 *   - listar y filtrar pedidos,
 *   - asignar repartidor (de forma asíncrona),
 *   - cambiar el estado del pedido y dejar rastro en la auditoría.
 *
 * Las vistas (PanelNuevaVenta, PanelMonitorPedidos) son "tontas": solo recogen
 * datos del usuario y llaman a estos métodos. Aquí están las REGLAS.
 *
 * Depende de la interfaz IRepositorioBase (no de la clase concreta), lo que permite
 * inyectar un repositorio falso en pruebas. Patrón: MVC + Inyección de Dependencias.
 */
public class PedidoController {

    // Acceso a datos a través de la ABSTRACCIÓN (interfaz), no de la implementación.
    private final IRepositorioBase<Pedido, String> pedidoRepository;

    // 👇 ESTADO INYECTADO: El controlador sabe quién opera la caja.
    // Se fija al iniciar sesión y se usa para marcar quién creó/modificó cada pedido
    // y para la auditoría. Así cada acción queda atribuida a un empleado real.
    private final String codCajeroActivo;

    /**
     * Constructor de uso real: crea el repositorio que habla con SQL Server.
     *
     * @param codCajeroActivo código (String) del empleado que abrió la caja; se adjunta
     *                        a los pedidos que cree y a los registros de auditoría.
     */
    public PedidoController(String codCajeroActivo) {
        this(codCajeroActivo, new PedidoRepository());
    }

    /**
     * Constructor para Inyección de Dependencias: recibe el repositorio desde fuera.
     * Útil para pruebas (inyectar un mock) y para reutilizar la misma instancia.
     *
     * @param codCajeroActivo  código (String) del empleado en sesión.
     * @param pedidoRepository implementación de {@link IRepositorioBase} para Pedido;
     *                         no puede ser null (se valida con Objects.requireNonNull).
     * @throws NullPointerException si pedidoRepository es null.
     */
    public PedidoController(String codCajeroActivo, IRepositorioBase<Pedido, String> pedidoRepository) {
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository);
        this.codCajeroActivo = codCajeroActivo;
    }

    /**
     * Genera y persiste un pedido nuevo a partir de lo que tiene la pantalla de venta.
     * 1) valida los datos de entrada, 2) ensambla la entidad Pedido, 3) la guarda.
     * (Antes existía una capa Service intermedia; ahora la vista llama directo al controller.)
     *
     * @param cliente                    cliente (objeto Cliente) al que pertenece el pedido; no null.
     * @param cantidadProductosEnCarrito número de ítems en el carrito (int); debe ser > 0.
     * @param total                      monto total del pedido (double); debe ser > 0.
     * @param direccionEntrega           dirección de entrega (String); no vacía.
     * @param codTarifa                  código (String) de la tarifa de envío seleccionada; no vacío.
     * @param codPago                    código (String) del método de pago seleccionado; no vacío.
     * @return no devuelve nada (void): su efecto es insertar el pedido en la base de datos.
     * @throws ValidationException si alguno de los datos anteriores no cumple las reglas.
     */
    public void generarPedido(
            Cliente cliente,
            int cantidadProductosEnCarrito,
            double total,
            String direccionEntrega,
            String codTarifa,
            String codPago
    ) {
        validarDatosGeneracion(cliente, cantidadProductosEnCarrito, total, direccionEntrega, codTarifa, codPago);

        Pedido pedido = ensamblarNuevoPedido(cliente, total, direccionEntrega, codTarifa, codPago, this.codCajeroActivo);
        pedidoRepository.insert(pedido);
    }

    /**
     * Devuelve todos los pedidos (sin filtrar). Lo usa el Monitor al cargar.
     *
     * @return List&lt;Pedido&gt; con todos los pedidos de la BD; lista vacía si no hay ninguno.
     */
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    /**
     * Filtra los pedidos por estado ("EN ESPERA", "EN CAMINO", "ENTREGADO").
     * Si el estado es nulo/vacío o "TODOS", devuelve la lista completa.
     * El resultado se ordena por fecha de solicitud, del más reciente al más antiguo.
     *
     * @param estado texto (String) del estado a filtrar; admite null, vacío o "TODOS"
     *               como "sin filtro". La comparación ignora mayúsculas/minúsculas.
     * @return List&lt;Pedido&gt; con los pedidos que coinciden, ya ordenada; vacía si ninguno coincide.
     */
    public List<Pedido> filtrarPedidosPorEstado(String estado) {
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

    /**
     * Asigna un repartidor a un pedido y lo pasa a estado "EN CAMINO".
     *
     * Se ejecuta de forma ASÍNCRONA (CompletableFuture) para no congelar la interfaz
     * Swing mientras se escribe en la BD. Si algo falla, el error se reporta por consola
     * sin tumbar la aplicación. Al terminar, deja registro en la auditoría.
     *
     * @param codPedido     código (String) del pedido a despachar; no vacío.
     * @param codRepartidor código (String) del repartidor a asignar; no vacío.
     * @return no devuelve nada (void); el trabajo ocurre en segundo plano. La validación
     *         de pedido inexistente sucede dentro del hilo asíncrono.
     * @throws ValidationException si codPedido o codRepartidor están vacíos (validado antes
     *                             de lanzar la tarea asíncrona).
     */
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
            pedido.setTiempoEntEstimado(pedido.getTiempoEntEstimado());

            pedidoRepository.update(pedido);

            // 2) Auditoría
            AuditoriaLog.registrarAccion("SISTEMA", "Asignado repartidor " + codRepartidor + " a pedido " + codPedido);
        }).exceptionally(ex -> {
            System.err.println("\n==========================================");
            System.err.println("❌ ERROR GRAVE AL ASIGNAR REPARTIDOR:");
            ex.printStackTrace();
            System.err.println("==========================================\n");
            return null;
        });
    }

    /**
     * Cambia el estado de un pedido existente (p. ej. "EN CAMINO" → "ENTREGADO").
     * Busca el pedido, actualiza su estado y registra QUIÉN hizo el cambio en la auditoría.
     * Si no hay cajero activo, la acción se atribuye a "SISTEMA".
     *
     * @param codPedido   código (String) del pedido a modificar; no vacío.
     * @param nuevoEstado nuevo estado (String) a asignar; no vacío.
     * @return no devuelve nada (void): persiste el cambio y escribe en la auditoría.
     * @throws ValidationException si los parámetros están vacíos o el pedido no existe.
     */
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

        String auditoriaActor = (codCajeroActivo == null || codCajeroActivo.isBlank()) ? "SISTEMA" : codCajeroActivo;
        AuditoriaLog.registrarAccion(auditoriaActor, "Actualizó estado del pedido " + codPedido + " a " + pedido.getEstado());
    }

    /**
     * Reglas de negocio para poder generar un pedido. Cada incumplimiento lanza una
     * ValidationException con un mensaje claro que la vista mostrará al usuario.
     *
     * @param cliente                    Cliente; falla si es null.
     * @param cantidadProductosEnCarrito int; falla si es 0.
     * @param total                      double; falla si es &lt;= 0.
     * @param direccionEntrega           String; falla si es null o vacío.
     * @param codTarifa                  String; falla si es null o vacío.
     * @param codPago                    String; falla si es null o vacío.
     * @return no devuelve nada (void): si no lanza excepción, los datos son válidos.
     * @throws ValidationException ante el primer dato inválido encontrado.
     */
    private static void validarDatosGeneracion(
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
    }

    /**
     * Construye la entidad Pedido lista para guardar: genera un código único, fija el
     * estado inicial ("EN ESPERA") y la fecha actual, y enlaza las claves foráneas
     * (cliente, tarifa, pago, asistente). Repartidor y tiempos quedan nulos hasta el despacho.
     *
     * @param cliente          Cliente del pedido (se usa su idCliente).
     * @param total            monto total (double) ya calculado.
     * @param direccionEntrega dirección de entrega (String).
     * @param codTarifa        código (String) de la tarifa.
     * @param codPago          código (String) del método de pago.
     * @param codAsistente     código (String) del cajero/asistente que registra el pedido.
     * @return un objeto {@link Pedido} completamente armado y listo para insertarse en la BD.
     */
    private static Pedido ensamblarNuevoPedido(
            Cliente cliente,
            double total,
            String direccionEntrega,
            String codTarifa,
            String codPago,
            String codAsistente
    ) {
        Pedido pedido = new Pedido();

        // 1. Código único
        pedido.setCodPedido(String.format("P%04d", (int) (Math.random() * 10000)));

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
