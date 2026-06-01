package com.laptitefrance.delivery.views;

import com.laptitefrance.delivery.controllers.ClienteController;
import com.laptitefrance.delivery.controllers.PedidoController;
import com.laptitefrance.delivery.controllers.ProductoController;
import com.laptitefrance.delivery.exceptions.DuplicateException;
import com.laptitefrance.delivery.exceptions.NotFoundException;
import com.laptitefrance.delivery.exceptions.ValidationException;
import com.laptitefrance.delivery.models.Cliente;
import com.laptitefrance.delivery.models.Producto;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class PanelNuevaVenta extends JPanel {

    private final ClienteController clienteController;
    private final ProductoController productoController;
    private final PedidoController pedidoController;

    private Cliente clienteSeleccionado = null;

    private JTextField txtBuscarCliente;
    private JLabel lblNombreCliente;

    private JTable tablaMenu;
    private DefaultTableModel modeloMenu;

    private JTable tablaCarrito;
    private DefaultTableModel modeloCarrito;

    private JLabel lblTotal;
    private double totalCarrito = 0.0;

    public PanelNuevaVenta() {
        // Únicamente instanciamos los Controladores (Cero Repositorios)
        this.clienteController = new ClienteController();
        this.productoController = new ProductoController();
        this.pedidoController = new PedidoController();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        inicializarComponentes();
        cargarMenu();
    }

    private void inicializarComponentes() {
        // ================= PANEL SUPERIOR =================
        JPanel panelNorte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelNorte.setBorder(BorderFactory.createTitledBorder("Datos del Cliente"));

        panelNorte.add(new JLabel("Celular Cliente:"));
        txtBuscarCliente = new JTextField(10);
        panelNorte.add(txtBuscarCliente);

        JButton btnBuscar = new JButton("🔍 Buscar");
        btnBuscar.addActionListener(e -> buscarCliente());
        panelNorte.add(btnBuscar);

        JButton btnNuevo = new JButton("+ Nuevo");
        btnNuevo.setBackground(new Color(52, 152, 219));
        btnNuevo.setForeground(Color.WHITE);
        btnNuevo.addActionListener(e -> registrarNuevoCliente());
        panelNorte.add(btnNuevo);

        // UX Fix: Botón para deseleccionar cliente actual
        JButton btnLimpiarCliente = new JButton("Limpiar");
        btnLimpiarCliente.addActionListener(e -> limpiarCliente());
        panelNorte.add(btnLimpiarCliente);

        lblNombreCliente = new JLabel("  (Ningún cliente seleccionado)");
        lblNombreCliente.setForeground(Color.BLUE);
        panelNorte.add(lblNombreCliente);

        add(panelNorte, BorderLayout.NORTH);

        // ================= PANEL CENTRAL =================
        JPanel panelCentro = new JPanel(new BorderLayout());
        panelCentro.setBorder(BorderFactory.createTitledBorder("Menú Disponible (Solo con Stock)"));

        // UX Fix: Hacemos que la tabla no sea editable desde la vista
        modeloMenu = new DefaultTableModel(new Object[]{"Código", "Nombre", "Precio", "Stock"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        tablaMenu = new JTable(modeloMenu);
        panelCentro.add(new JScrollPane(tablaMenu), BorderLayout.CENTER);

        JButton btnAgregar = new JButton("Agregar al Carrito ⬇");
        btnAgregar.addActionListener(e -> agregarAlCarrito());
        panelCentro.add(btnAgregar, BorderLayout.SOUTH);

        add(panelCentro, BorderLayout.CENTER);

        // ================= PANEL INFERIOR =================
        JPanel panelSur = new JPanel(new BorderLayout());
        panelSur.setPreferredSize(new Dimension(900, 200));
        panelSur.setBorder(BorderFactory.createTitledBorder("Carrito de Compras"));

        // UX Fix: Tabla Carrito no editable
        modeloCarrito = new DefaultTableModel(new Object[]{"Código", "Nombre", "Cant", "Subtotal"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        tablaCarrito = new JTable(modeloCarrito);
        panelSur.add(new JScrollPane(tablaCarrito), BorderLayout.CENTER);

        JPanel panelTotal = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        // UX Fix: Botón de Quitar Fila del Carrito
        JButton btnQuitar = new JButton("❌ Quitar del Carrito");
        btnQuitar.addActionListener(e -> quitarDelCarrito());
        panelTotal.add(btnQuitar);

        lblTotal = new JLabel("   Total: S/ 0.00   ");
        lblTotal.setFont(new Font("Arial", Font.BOLD, 18));
        panelTotal.add(lblTotal);

        JButton btnGenerarPedido = new JButton("✅ Generar Pedido");
        btnGenerarPedido.setBackground(new Color(46, 204, 113));
        btnGenerarPedido.setForeground(Color.WHITE);
        btnGenerarPedido.addActionListener(e -> generarPedido());
        panelTotal.add(btnGenerarPedido);

        panelSur.add(panelTotal, BorderLayout.SOUTH);
        add(panelSur, BorderLayout.SOUTH);
    }

    private void cargarMenu() {
        modeloMenu.setRowCount(0);
        List<Producto> productos = productoController.obtenerProductosConStock();
        
        for (Producto p : productos) {
            modeloMenu.addRow(new Object[]{
                    p.getCodProducto(), 
                    p.getNombreProd(), 
                    p.getPrecioProd(), 
                    p.getStock()
            });
        }
    }

    private void buscarCliente() {
        try {
            clienteSeleccionado = clienteController.buscarClientePorCelular(txtBuscarCliente.getText());
            lblNombreCliente.setText(" Cliente: " + clienteSeleccionado.getNombreCliente());
        } catch (ValidationException | NotFoundException ex) {
            limpiarCliente();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Atención", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void limpiarCliente() {
        txtBuscarCliente.setText("");
        clienteSeleccionado = null;
        lblNombreCliente.setText("  (Ningún cliente seleccionado)");
    }

    private void registrarNuevoCliente() {
        JTextField txtNombre = new JTextField();
        JTextField txtCelular = new JTextField(txtBuscarCliente.getText().trim());

        Object[] formulario = {
                "Nombre Completo:", txtNombre,
                "Número de Celular (+51):", txtCelular
        };

        int opcion = JOptionPane.showConfirmDialog(this, formulario, "Registrar Cliente", JOptionPane.OK_CANCEL_OPTION);

        if (opcion == JOptionPane.OK_OPTION) {
            try {
                clienteSeleccionado = clienteController.registrarCliente(txtNombre.getText(), txtCelular.getText());
                
                txtBuscarCliente.setText(clienteSeleccionado.getNrocelular());
                lblNombreCliente.setText(" Cliente: " + clienteSeleccionado.getNombreCliente());
                
                JOptionPane.showMessageDialog(this, "Cliente registrado exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (ValidationException | DuplicateException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de Validación", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void agregarAlCarrito() {
        int fila = tablaMenu.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto del menú.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String cod = (String) modeloMenu.getValueAt(fila, 0);
        String nom = (String) modeloMenu.getValueAt(fila, 1);
        double pre = (double) modeloMenu.getValueAt(fila, 2);

        String cantStr = JOptionPane.showInputDialog(this, "Cantidad para " + nom + ":");
        if (cantStr == null || cantStr.trim().isEmpty()) return;

        try {
            int cant = Integer.parseInt(cantStr);
            if (cant <= 0) throw new NumberFormatException();

            double sub = pre * cant;
            modeloCarrito.addRow(new Object[]{cod, nom, cant, sub});
            
            totalCarrito += sub;
            actualizarTotal();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Cantidad inválida. Ingrese un número mayor a 0.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void quitarDelCarrito() {
        int fila = tablaCarrito.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto del carrito para quitar.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double subtotal = (double) modeloCarrito.getValueAt(fila, 3);
        totalCarrito -= subtotal;
        
        // Evitar bugs de precisión (ej. -0.0000000001)
        if (totalCarrito < 0) totalCarrito = 0.0;
        
        modeloCarrito.removeRow(fila);
        actualizarTotal();
    }

    private void actualizarTotal() {
        lblTotal.setText(String.format("   Total: S/ %.2f   ", totalCarrito));
    }

    private void generarPedido() {
        try {
            // Pasamos validación de pedido al controlador
            pedidoController.generarPedido(clienteSeleccionado, modeloCarrito.getRowCount(), totalCarrito);
            
            JOptionPane.showMessageDialog(this, 
                "¡Pedido generado exitosamente!\nTotal: S/ " + String.format("%.2f", totalCarrito), 
                "Transacción Exitosa", 
                JOptionPane.INFORMATION_MESSAGE);
            
            // UX Fix: Limpiamos la pantalla y reiniciamos el carrito para el siguiente pedido
            limpiarPantalla();
            
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Atención", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error inesperado al generar pedido: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarPantalla() {
        limpiarCliente();
        modeloCarrito.setRowCount(0);
        totalCarrito = 0.0;
        actualizarTotal();
        tablaMenu.clearSelection();
    }
}