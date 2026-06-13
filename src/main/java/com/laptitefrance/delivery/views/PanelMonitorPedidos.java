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
import com.laptitefrance.delivery.models.Pedido;

public class PanelMonitorPedidos extends JPanel {

    // 👇 Controlador inyectado
    private final PedidoController pedidoController;
    
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
        
        cbxFiltroEstado = new JComboBox<>(new String[]{"TODOS", "EN ESPERA", "EN CAMINO", "ENTREGADO"});

        panelNorte.add(cbxFiltroEstado);

        JButton btnFiltrar = new JButton("🔍 Filtrar");
        btnFiltrar.addActionListener(e -> cargarPedidos((String) cbxFiltroEstado.getSelectedItem()));
        panelNorte.add(btnFiltrar);

        add(panelNorte, BorderLayout.NORTH);

        // ================= PANEL CENTRAL (Tabla No Editable) =================
        JPanel panelCentro = new JPanel(new BorderLayout());
        panelCentro.setBorder(BorderFactory.createTitledBorder("Listado de Pedidos"));

        modeloPedidos = new DefaultTableModel(new Object[]{"Código Pedido", "ID Cliente", "Monto Total", "Estado", "Hora Registro"}, 0) {
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

        // ================= PANEL INFERIOR (Acciones) =================
        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnAsignarRepartidor = new JButton("🛵 Asignar Repartidor");
        btnAsignarRepartidor.setBackground(new Color(52, 152, 219));
        btnAsignarRepartidor.setForeground(Color.WHITE);
        btnAsignarRepartidor.addActionListener(e -> modalAsignarRepartidor());
        panelSur.add(btnAsignarRepartidor);

        JButton btnEditarPedido = new JButton("📝 Editar Estado");
        btnEditarPedido.setBackground(new Color(243, 156, 18));
        btnEditarPedido.setForeground(Color.WHITE);
        btnEditarPedido.addActionListener(e -> modalEditarPedido());
        panelSur.add(btnEditarPedido);

        add(panelSur, BorderLayout.SOUTH);
    }

    private void cargarPedidos(String estado) {
        modeloPedidos.setRowCount(0); 
        try {
            List<Pedido> pedidos = pedidoController.filtrarPedidosPorEstado(estado);
            
            for (Pedido p : pedidos) {
                modeloPedidos.addRow(new Object[]{
                        p.getCodPedido(),
                        p.getIdCliente(),
                        String.format("S/ %.2f", p.getMontoPedido()),
                        p.getEstado(),
                        (p.getFechaSolicitud() != null ? p.getFechaSolicitud().toString() : "")
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
        String codRepartidor = JOptionPane.showInputDialog(this, "Ingrese el Código del Repartidor (Ej: E001):", "Asignar Repartidor", JOptionPane.QUESTION_MESSAGE);

        if (codRepartidor != null && !codRepartidor.trim().isEmpty()) {
            try {
                pedidoController.asignarRepartidor(codPedido, codRepartidor.trim());
                JOptionPane.showMessageDialog(this, "La asignación se está procesando en segundo plano.", "Procesando", JOptionPane.INFORMATION_MESSAGE);
                cargarPedidos((String) cbxFiltroEstado.getSelectedItem());
            } catch (ValidationException ex) {
                mostrarAdvertencia(ex.getMessage());
            } catch (Exception ex) {
                mostrarError("Error inesperado: " + ex.getMessage());
            }
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