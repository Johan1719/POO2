package com.laptitefrance.delivery.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.laptitefrance.delivery.controllers.ClienteController;
import com.laptitefrance.delivery.dtos.ItemVenta;
import com.laptitefrance.delivery.controllers.PagoController;
import com.laptitefrance.delivery.controllers.PedidoController;
import com.laptitefrance.delivery.controllers.ProductoController;
import com.laptitefrance.delivery.controllers.TarifaController;
import com.laptitefrance.delivery.exceptions.DuplicateException;
import com.laptitefrance.delivery.exceptions.NotFoundException;
import com.laptitefrance.delivery.exceptions.ValidationException;
import com.laptitefrance.delivery.models.Cliente;
import com.laptitefrance.delivery.models.Pago;
import com.laptitefrance.delivery.models.Producto;
import com.laptitefrance.delivery.models.Tarifa;

public class PanelNuevaVenta extends JPanel {

    private final ClienteController clienteController;
    private final ProductoController productoController;
    private final TarifaController tarifaController;
    private final PagoController pagoController;
    
    // 👇 Variable inyectada
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

    // 👇 Constructor modificado para recibir la inyección
    public PanelNuevaVenta(PedidoController pedidoController) {
        this.clienteController = new ClienteController();
        this.productoController = new ProductoController();
        this.tarifaController = new TarifaController();
        this.pagoController = new PagoController();
        
        // El controlador orquestado se asigna aquí
        this.pedidoController = pedidoController;

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

        modeloMenu = new DefaultTableModel(new Object[]{"Código", "Nombre", "Precio", "Stock"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
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

        modeloCarrito = new DefaultTableModel(new Object[]{"Código", "Nombre", "Cant", "Subtotal"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaCarrito = new JTable(modeloCarrito);
        panelSur.add(new JScrollPane(tablaCarrito), BorderLayout.CENTER);

        JPanel panelTotal = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnQuitar = new JButton("❌ Quitar del Carrito");
        btnQuitar.addActionListener(e -> quitarDelCarrito());
        panelTotal.add(btnQuitar);

        lblTotal = new JLabel("   Total: S/ 0.00   ");
        lblTotal.setFont(new Font("Arial", Font.BOLD, 18));
        panelTotal.add(lblTotal);

        JButton btnActualizarStock = new JButton("🔄 Actualizar");
        btnActualizarStock.setBackground(new Color(52, 152, 219));
        btnActualizarStock.setForeground(Color.WHITE);
        btnActualizarStock.addActionListener(e -> actualizarMenuYCarrito());
        panelTotal.add(btnActualizarStock);

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

        if (totalCarrito < 0) totalCarrito = 0.0;

        modeloCarrito.removeRow(fila);
        actualizarTotal();
    }

    private void actualizarTotal() {
        lblTotal.setText(String.format("   Total: S/ %.2f   ", totalCarrito));
    }

    private void generarPedido() {
        try {
            if (clienteSeleccionado == null) {
                throw new ValidationException("Debe seleccionar un cliente.");
            }
            if (modeloCarrito.getRowCount() == 0) {
                throw new ValidationException("Debe agregar productos al carrito.");
            }
            if (totalCarrito <= 0) {
                throw new ValidationException("El total del pedido debe ser mayor a 0.");
            }

            // Paso 1: elegir modalidad de entrega.
            String[] opciones = {"🏪 Recojo en tienda", "🛵 Delivery a domicilio", "Cancelar"};
            int modalidad = JOptionPane.showOptionDialog(
                    this,
                    "¿Cómo se entregará el pedido?",
                    "Modalidad de entrega",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    opciones,
                    opciones[0]
            );

            if (modalidad != 0 && modalidad != 1) {
                return; // Cancelar o cerrar
            }

            boolean esRecojo = (modalidad == 0);

            // Tarifas disponibles, separadas por modalidad.
            List<Tarifa> todasTarifas = tarifaController.obtenerTarifas();
            List<Tarifa> tarifasFiltradas = new java.util.ArrayList<>();
            for (Tarifa t : todasTarifas) {
                if (t.esRecojo() == esRecojo) {
                    tarifasFiltradas.add(t);
                }
            }
            if (tarifasFiltradas.isEmpty()) {
                throw new ValidationException(esRecojo
                        ? "No hay puntos de recojo configurados."
                        : "No hay zonas de delivery configuradas.");
            }
            JComboBox<Tarifa> cbxTarifa = new JComboBox<>(tarifasFiltradas.toArray(new Tarifa[0]));

            List<Pago> pagos = pagoController.obtenerPagos();
            JComboBox<Pago> cbxPago = new JComboBox<>(pagos.toArray(new Pago[0]));

            // Paso 2: datos según modalidad.
            JTextField txtDireccionEntrega = new JTextField(30);
            Object[] formulario;
            String tituloDialogo;
            if (esRecojo) {
                tituloDialogo = "Checkout - Recojo en tienda";
                formulario = new Object[]{
                        "Punto de recojo:", cbxTarifa,
                        "Método de pago:", cbxPago
                };
            } else {
                tituloDialogo = "Checkout - Delivery";
                formulario = new Object[]{
                        "Dirección de entrega:", txtDireccionEntrega,
                        "Zona / Tarifa:", cbxTarifa,
                        "Método de pago:", cbxPago
                };
            }

            int opcion = JOptionPane.showConfirmDialog(
                    this,
                    formulario,
                    tituloDialogo,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );
            if (opcion != JOptionPane.OK_OPTION) {
                return;
            }

            Tarifa tarifaSeleccionada = (Tarifa) cbxTarifa.getSelectedItem();
            Pago pagoSeleccionado = (Pago) cbxPago.getSelectedItem();
            if (tarifaSeleccionada == null || pagoSeleccionado == null) {
                throw new ValidationException("Debe seleccionar una tarifa y un método de pago.");
            }

            // Dirección: en recojo se genera un texto descriptivo; en delivery la escribe el cajero.
            String direccionEntrega;
            if (esRecojo) {
                direccionEntrega = "RECOJO EN TIENDA: " + tarifaSeleccionada.getNombreZona();
            } else {
                direccionEntrega = txtDireccionEntrega.getText() == null ? "" : txtDireccionEntrega.getText().trim();
                if (direccionEntrega.isEmpty()) {
                    throw new ValidationException("La dirección de entrega no puede estar vacía.");
                }
            }

            // Armar los ítems del carrito (Código, Nombre, Cant) para descontar stock.
            java.util.List<ItemVenta> items = new java.util.ArrayList<>();
            for (int i = 0; i < modeloCarrito.getRowCount(); i++) {
                String codProd = (String) modeloCarrito.getValueAt(i, 0);
                String nomProd = (String) modeloCarrito.getValueAt(i, 1);
                int cant = ((Number) modeloCarrito.getValueAt(i, 2)).intValue();
                items.add(new ItemVenta(codProd, nomProd, cant));
            }

            java.util.List<String> productosEnCero = pedidoController.generarPedido(
                    clienteSeleccionado,
                    items,
                    totalCarrito,
                    direccionEntrega,
                    tarifaSeleccionada.getCodTarifa(),
                    pagoSeleccionado.getCodPago(),
                    esRecojo
            );

            JOptionPane.showMessageDialog(
                    this,
                    "¡Pedido generado exitosamente!\nTotal: S/ " + String.format("%.2f", totalCarrito),
                    "Transacción Exitosa",
                    JOptionPane.INFORMATION_MESSAGE
            );

            if (productosEnCero != null && !productosEnCero.isEmpty()) {
                StringBuilder aviso = new StringBuilder();
                for (String nombre : productosEnCero) {
                    aviso.append("⚠️ Producto ").append(nombre).append(" ya no tiene stock, se sugiere reabastecer.\n");
                }
                JOptionPane.showMessageDialog(this, aviso.toString().trim(), "Reabastecer", JOptionPane.WARNING_MESSAGE);
            }

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

    private void actualizarMenuYCarrito() {
        try {
            // Recarga menú desde BD (stock puede haber cambiado)
            cargarMenu();

            // Limpia carrito para evitar inconsistencias (cantidades/precios ya no coinciden con stock)
            modeloCarrito.setRowCount(0);
            totalCarrito = 0.0;
            actualizarTotal();
            JOptionPane.showMessageDialog(this, "Menú actualizado y carrito limpiado.", "Actualizar", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al actualizar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

