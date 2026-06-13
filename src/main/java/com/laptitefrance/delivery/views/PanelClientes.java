package com.laptitefrance.delivery.views;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import com.laptitefrance.delivery.controllers.ClienteController;
import com.laptitefrance.delivery.exceptions.ValidationException;
import com.laptitefrance.delivery.models.Cliente;
import com.laptitefrance.delivery.views.PaginatorPanel;

public class PanelClientes extends JPanel {

    private final ClienteController clienteController;
    private JTable tablaClientes;
    private DefaultTableModel modeloClientes;
    private JTextField txtBuscarCelular;

    private int page = 1;
    private final int pageSize = 10;
    private int totalPages = 1;
    private PaginatorPanel paginator;

    public PanelClientes(ClienteController clienteController) {
        this.clienteController = clienteController;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        inicializarComponentes();
        cargarClientes("");
    }

    private void inicializarComponentes() {
        // --- PANEL NORTE (Búsqueda) ---
        JPanel panelNorte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelNorte.setBorder(BorderFactory.createTitledBorder("Directorio de Clientes"));

        panelNorte.add(new JLabel("Celular del Cliente:"));
        txtBuscarCelular = new JTextField(15);
        panelNorte.add(txtBuscarCelular);

        JButton btnBuscar = new JButton("🔍 Buscar");
        btnBuscar.addActionListener(e -> {
            page = 1;
            cargarClientes(txtBuscarCelular.getText());
        });
        panelNorte.add(btnBuscar);


        JButton btnRefrescar = new JButton("🔄 Ver Todos");
        btnRefrescar.addActionListener(e -> {
            txtBuscarCelular.setText("");
            page = 1;
            cargarClientes("");
        });
        panelNorte.add(btnRefrescar);


        add(panelNorte, BorderLayout.NORTH);

        // --- PANEL CENTRAL (Tabla) ---
        modeloClientes = new DefaultTableModel(new Object[]{"ID Cliente", "Nombre Completo", "Celular", "Fecha Registro"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaClientes = new JTable(modeloClientes);
        tablaClientes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(tablaClientes), BorderLayout.CENTER);

        // --- PANEL SUR (Acciones de CRM) ---
        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnHistorial = new JButton("📄 Ver Historial de Pedidos");
        btnHistorial.addActionListener(e -> mostrarHistorialRapido());
        panelSur.add(btnHistorial);

        JButton btnEditarCelular = new JButton("📱 Actualizar Celular");
        btnEditarCelular.setBackground(new Color(52, 152, 219));
        btnEditarCelular.setForeground(Color.WHITE);
        btnEditarCelular.addActionListener(e -> modalActualizarCelular());
        panelSur.add(btnEditarCelular);

        JButton btnEditarNombre = new JButton("✏️ Editar Nombre");
        btnEditarNombre.setBackground(new Color(39, 174, 96));
        btnEditarNombre.setForeground(Color.WHITE);
        btnEditarNombre.addActionListener(e -> modalEditarNombre());
        panelSur.add(btnEditarNombre);

        JButton btnEliminar = new JButton("🗑️ Eliminar Cliente");
        btnEliminar.setBackground(new Color(231, 76, 60));
        btnEliminar.setForeground(Color.WHITE);
        btnEliminar.addActionListener(e -> modalEliminarCliente());
        panelSur.add(btnEliminar);

        paginator = new PaginatorPanel(page, totalPages, new PaginatorPanel.PageChangeListener() {

            @Override
            public void onPageChange(int newPage) {
                page = newPage;
                cargarClientes(txtBuscarCelular.getText());
            }
        });

        JPanel panelAbajo = new JPanel(new BorderLayout(5, 5));
        panelAbajo.add(panelSur, BorderLayout.CENTER);
        panelAbajo.add(paginator, BorderLayout.PAGE_END);
        add(panelAbajo, BorderLayout.SOUTH);

        revalidate();
        repaint();
    }

    private void cargarClientes(String celular) {
        modeloClientes.setRowCount(0);
        try {
            int totalItems = clienteController.contarClientesFiltrados(celular);
            totalPages = (int) Math.ceil(totalItems / (double) pageSize);
            if (totalPages < 1) totalPages = 1;
            if (page > totalPages) page = totalPages;
            if (paginator != null) paginator.setTotalPages(totalPages);

            List<Cliente> clientes = clienteController.listarClientesPaginado(celular, page, pageSize);
            for (Cliente c : clientes) {
                modeloClientes.addRow(new Object[]{
                        c.getIdCliente(), c.getNombreCliente(), c.getNrocelular(), 
                        (c.getFechaRegistro() != null ? c.getFechaRegistro().toString() : "")
                });
            }
            if (paginator != null) paginator.setPage(page);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar clientes: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void modalActualizarCelular() {
        int fila = tablaClientes.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un cliente para actualizar.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String idCliente = (String) modeloClientes.getValueAt(fila, 0);
        String celularActual = (String) modeloClientes.getValueAt(fila, 2);

        String nuevoCelular = JOptionPane.showInputDialog(this, "Ingrese el nuevo nmero de celular (+51):", celularActual);
        
        if (nuevoCelular != null && !nuevoCelular.trim().isEmpty() && !nuevoCelular.equals(celularActual)) {
            try {
                // VISTA TONTA: El controller hace las validaciones de los 9 dgitos
                clienteController.actualizarCelular(idCliente, nuevoCelular.trim());
                JOptionPane.showMessageDialog(this, "Celular actualizado exitosamente.", "xito", JOptionPane.INFORMATION_MESSAGE);
                cargarClientes("");
            } catch (ValidationException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de Validacin", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al actualizar.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void modalEditarNombre() {
        int fila = tablaClientes.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un cliente para editar.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String idCliente = (String) modeloClientes.getValueAt(fila, 0);
        String nombreActual = (String) modeloClientes.getValueAt(fila, 1);

        String nuevoNombre = JOptionPane.showInputDialog(this, "Ingrese el nuevo nombre:", nombreActual);
        if (nuevoNombre == null) return;
        if (!nuevoNombre.trim().equals(nombreActual)) {
            try {
                clienteController.actualizarNombre(idCliente, nuevoNombre.trim());
                JOptionPane.showMessageDialog(this, "Nombre actualizado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                cargarClientes(txtBuscarCelular.getText());
            } catch (ValidationException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al actualizar.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void modalEliminarCliente() {
        int fila = tablaClientes.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un cliente para eliminar.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String idCliente = (String) modeloClientes.getValueAt(fila, 0);
        String nombre = (String) modeloClientes.getValueAt(fila, 1);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "¿Seguro que desea eliminar a " + nombre + "?",
                "Confirmar eliminación",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.OK_OPTION) return;

        try {
            clienteController.eliminarCliente(idCliente);
            JOptionPane.showMessageDialog(this, "Cliente eliminado.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            cargarClientes(txtBuscarCelular.getText());
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al eliminar.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarHistorialRapido() {

        int fila = tablaClientes.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un cliente.", "Atencin", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String nombre = (String) modeloClientes.getValueAt(fila, 1);
        // Aqu podras conectar al PedidoController en el futuro para traer una tabla real.
        // Por ahora, un simple mensaje informativo (Mockup).
        JOptionPane.showMessageDialog(this, "Mdulo de historial en construccin.\nCliente seleccionado: " + nombre, "Historial", JOptionPane.INFORMATION_MESSAGE);
    }
}