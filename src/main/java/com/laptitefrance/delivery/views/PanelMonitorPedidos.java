package com.laptitefrance.delivery.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import com.laptitefrance.delivery.controllers.PedidoController;
import com.laptitefrance.delivery.exceptions.ValidationException;


public class PanelMonitorPedidos extends JPanel {

    // 👇 Controlador inyectado
    private final PedidoController pedidoController;
    
    private PaginatorPanel paginator;
    
    private JTable tablaPedidos;

    private DefaultTableModel modeloPedidos;
    private JComboBox<String> cbxFiltroEstado;

    // 👇 Constructor modificado para recibir Inyección de Dependencias
    public PanelMonitorPedidos(PedidoController pedidoController) {
        this.pedidoController = pedidoController;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        inicializarComponentes();
        cargarPedidos("TODOS"); 
    }

    private void inicializarComponentes() {
        // ================= PANEL SUPERIOR (Filtros) =================
        JPanel panelNorte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelNorte.setBorder(BorderFactory.createTitledBorder("Filtros de Búsqueda"));

        panelNorte.add(new JLabel("Estado del Pedido:"));
        
        cbxFiltroEstado = new JComboBox<>(new String[]{"TODOS", "EN ESPERA", "EN CAMINO", "ENTREGADO", "CANCELADO"});

        panelNorte.add(cbxFiltroEstado);

        JButton btnFiltrar = new JButton("🔍 Filtrar");
        btnFiltrar.addActionListener(e -> cargarPedidos((String) cbxFiltroEstado.getSelectedItem()));
        panelNorte.add(btnFiltrar);

        add(panelNorte, BorderLayout.NORTH);

        // ================= PANEL CENTRAL (Tabla No Editable) =================
        JPanel panelCentro = new JPanel(new BorderLayout());
        panelCentro.setBorder(BorderFactory.createTitledBorder("Listado de Pedidos"));

        modeloPedidos = new DefaultTableModel(new Object[]{"Código Pedido", "Nombre Cliente", "Monto Total", "Estado", "Repartidor", "Tiempo Aprox", "Hora Registro"}, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };


        tablaPedidos = new JTable(modeloPedidos);
        tablaPedidos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaPedidos.getTableHeader().setReorderingAllowed(false); 
        
        panelCentro.add(new JScrollPane(tablaPedidos), BorderLayout.CENTER);
        add(panelCentro, BorderLayout.CENTER);

        // ================= PANEL INFERIOR (Paginación + Acciones) =================
        JPanel panelSur = new JPanel(new BorderLayout());

        // Paginador reutilizable
        this.paginator = new PaginatorPanel(1, 1, newPage -> {
            paginaActual = newPage;
            cargarPedidos((String) cbxFiltroEstado.getSelectedItem());
        });
        panelSur.add(this.paginator, BorderLayout.CENTER);


        // Acciones
        JPanel panelAcciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnAsignarRepartidor = new JButton("🛵 Asignar Repartidor");
        btnAsignarRepartidor.setBackground(new Color(52, 152, 219));
        btnAsignarRepartidor.setForeground(Color.WHITE);
        btnAsignarRepartidor.addActionListener(e -> modalAsignarRepartidor());
        panelAcciones.add(btnAsignarRepartidor);

        JButton btnEditarPedido = new JButton("📝 Editar Estado");
        btnEditarPedido.setBackground(new Color(243, 156, 18));
        btnEditarPedido.setForeground(Color.WHITE);
        btnEditarPedido.addActionListener(e -> modalEditarPedido());
        panelAcciones.add(btnEditarPedido);

        panelSur.add(panelAcciones, BorderLayout.EAST);

        add(panelSur, BorderLayout.SOUTH);
    }



    private int paginaActual = 1;
    private final int pageSize = 10;

    private void cargarPedidos(String estado) {
        modeloPedidos.setRowCount(0);
        try {
            int total = pedidoController.contarPedidosMonitorFiltrados(estado);
            int totalPaginas = (int) Math.ceil(total / (double) pageSize);
            if (totalPaginas <= 0) totalPaginas = 1;
            if (paginaActual > totalPaginas) paginaActual = totalPaginas;
            if (paginaActual < 1) paginaActual = 1;
            if (paginator != null) {
                paginator.setTotalPages(totalPaginas);
                paginator.setPage(paginaActual);
            }


List<com.laptitefrance.delivery.dtos.PedidoMonitorRow> filas =
                    pedidoController.listarPedidosMonitorPaginado(estado, paginaActual, pageSize);

            for (com.laptitefrance.delivery.dtos.PedidoMonitorRow row : filas) {


                modeloPedidos.addRow(new Object[]{
                        row.codPedido,
                        row.nombreCliente,
                        String.format("S/ %.2f", row.montoPedido),
                        row.estado,
                        row.nombreRepartidor,
                        (row.tiempoEntEstimado != null ? row.tiempoEntEstimado.toString() : ""),
                        (row.fechaSolicitud != null ? row.fechaSolicitud.toString() : "")
                });
            }

        } catch (Exception ex) {
            mostrarError("Error al cargar los pedidos: " + ex.getMessage());
        }
    }



    private void modalAsignarRepartidor() {
        int filaSeleccionada = tablaPedidos.getSelectedRow();
        if (filaSeleccionada == -1) {
            mostrarAdvertencia("Por favor, seleccione un pedido de la tabla.");
            return;
        }

        String codPedido = (String) modeloPedidos.getValueAt(filaSeleccionada, 0);

        // Los pedidos de recojo en tienda no llevan repartidor.
        if (pedidoController.esPedidoDeRecojo(codPedido)) {
            mostrarAdvertencia("Este pedido es de RECOJO EN TIENDA: el cliente lo retira, no se le puede asignar repartidor.");
            return;
        }

        try {
            com.laptitefrance.delivery.controllers.RepartidorMonitorController repartidorController =
                    new com.laptitefrance.delivery.controllers.RepartidorMonitorController();

            List<com.laptitefrance.delivery.dtos.RepartidorMonitorRow> repartidores =
                    repartidorController.listarRepartidoresConEstado();




            if (repartidores == null || repartidores.isEmpty()) {

                mostrarAdvertencia("No existen repartidores disponibles en la base de datos.");
                return;
            }

            JComboBox<com.laptitefrance.delivery.dtos.RepartidorMonitorRow> cbxRepartidor =
                    new JComboBox<>(repartidores.toArray(new com.laptitefrance.delivery.dtos.RepartidorMonitorRow[0]));



            Object[] formulario = {
                    "Seleccione repartidor:", cbxRepartidor
            };

            int opcion = JOptionPane.showConfirmDialog(
                    this,
                    formulario,
                    "Asignar Repartidor - Pedido " + codPedido,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (opcion != JOptionPane.OK_OPTION) {
                return;
            }

            com.laptitefrance.delivery.dtos.RepartidorMonitorRow sel = (com.laptitefrance.delivery.dtos.RepartidorMonitorRow) cbxRepartidor.getSelectedItem();


            if (sel == null || sel.codRepartidor == null || sel.codRepartidor.trim().isEmpty()) {
                mostrarAdvertencia("Debe seleccionar un repartidor válido.");
                return;
            }

            pedidoController.asignarRepartidor(codPedido, sel.codRepartidor.trim());
            JOptionPane.showMessageDialog(this, "La asignación se está procesando en segundo plano.", "Procesando", JOptionPane.INFORMATION_MESSAGE);
            cargarPedidos((String) cbxFiltroEstado.getSelectedItem());

        } catch (ValidationException ex) {
            mostrarAdvertencia(ex.getMessage());
        } catch (Exception ex) {
            mostrarError("Error inesperado: " + ex.getMessage());
        }
    }

    private void modalEditarPedido() {
        int filaSeleccionada = tablaPedidos.getSelectedRow();
        if (filaSeleccionada == -1) {
            mostrarAdvertencia("Por favor, seleccione un pedido para editar.");
            return;
        }

        String codPedido = (String) modeloPedidos.getValueAt(filaSeleccionada, 0);
        String estadoActual = (String) modeloPedidos.getValueAt(filaSeleccionada, 3);

        // Las opciones deben ser compatibles con los estados que ya maneja el filtrado.
        String[] opciones = {"EN ESPERA", "EN CAMINO", "ENTREGADO", "CANCELADO"};

        String nuevoEstado = (String) JOptionPane.showInputDialog(this, "Modifique el estado del pedido:", "Editar Pedido " + codPedido, JOptionPane.PLAIN_MESSAGE, null, opciones, estadoActual);

        if (nuevoEstado != null && !nuevoEstado.equals(estadoActual)) {
            try {
                pedidoController.actualizarEstadoPedido(codPedido, nuevoEstado);
                JOptionPane.showMessageDialog(this, "Pedido actualizado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                cargarPedidos((String) cbxFiltroEstado.getSelectedItem());
            } catch (ValidationException ex) {
                mostrarAdvertencia(ex.getMessage());
            } catch (Exception ex) {
                mostrarError("Ocurrió un error al actualizar: " + ex.getMessage());
            }
        }
    }

    private void mostrarAdvertencia(String mensaje) { JOptionPane.showMessageDialog(this, mensaje, "Atención", JOptionPane.WARNING_MESSAGE); }
    private void mostrarError(String mensaje) { JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE); }
}