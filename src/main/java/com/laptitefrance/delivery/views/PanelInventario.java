package com.laptitefrance.delivery.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
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
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import com.laptitefrance.delivery.controllers.ProductoController;
import com.laptitefrance.delivery.exceptions.ValidationException;
import com.laptitefrance.delivery.models.Producto;

public class PanelInventario extends JPanel {

    private final ProductoController productoController;
    private JTable tablaProductos;
    private DefaultTableModel modeloProductos;
    private JTextField txtBuscar;

    private int page = 1;
    private final int pageSize = 10;
    private int totalPages = 1;
    private PaginatorPanel paginator;

    public PanelInventario(ProductoController productoController) {
        this.productoController = productoController;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        inicializarComponentes();
        cargarInventario("");
    }

    private void inicializarComponentes() {
        // --- PANEL NORTE (Búsqueda) ---
        JPanel panelNorte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelNorte.setBorder(BorderFactory.createTitledBorder("Buscar Producto"));

        panelNorte.add(new JLabel("Nombre o Código:"));
        txtBuscar = new JTextField(20);
        panelNorte.add(txtBuscar);

        JButton btnBuscar = new JButton("🔍 Buscar");
        btnBuscar.addActionListener(e -> {
            page = 1;
            cargarInventario(txtBuscar.getText());
        });
        panelNorte.add(btnBuscar);

        JButton btnRefrescar = new JButton("🔄 Todo");
        btnRefrescar.addActionListener(e -> {
            txtBuscar.setText("");
            page = 1;
            cargarInventario("");
        });
        panelNorte.add(btnRefrescar);

        add(panelNorte, BorderLayout.NORTH);

        // --- PANEL CENTRAL (Tabla) ---
        modeloProductos = new DefaultTableModel(
                new Object[] { "Código", "Nombre", "Categoría", "Precio (S/)", "Stock Actual", "Estado" }, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaProductos = new JTable(modeloProductos);
        tablaProductos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(tablaProductos), BorderLayout.CENTER);

        // --- PANEL SUR (Acciones) + PAGINADOR ---
        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnEditarPrecioStock = new JButton("✏️ Editar Precio y Stock");
        btnEditarPrecioStock.setBackground(new Color(52, 152, 219));
        btnEditarPrecioStock.setForeground(Color.WHITE);
        btnEditarPrecioStock.addActionListener(e -> modalEditarPrecioStock());
        panelSur.add(btnEditarPrecioStock);

        JButton btnEditarEstado = new JButton("⏻ Cambiar Estado");
        btnEditarEstado.setBackground(new Color(155, 89, 182));
        btnEditarEstado.setForeground(Color.WHITE);
        btnEditarEstado.addActionListener(e -> modalCambiarEstado());
        panelSur.add(btnEditarEstado);

        JButton btnNuevoProducto = new JButton("➕ Nuevo Producto");
        btnNuevoProducto.setBackground(new Color(46, 204, 113));
        btnNuevoProducto.setForeground(Color.WHITE);
        btnNuevoProducto.addActionListener(e -> modalCrearProducto());
        panelSur.add(btnNuevoProducto);

        JButton btnEliminarProducto = new JButton("🗑 Eliminar");
        btnEliminarProducto.setBackground(new Color(231, 76, 60));
        btnEliminarProducto.setForeground(Color.WHITE);
        btnEliminarProducto.addActionListener(e -> modalEliminarProducto());
        panelSur.add(btnEliminarProducto);


        paginator = new PaginatorPanel(page, totalPages, new PaginatorPanel.PageChangeListener() {
            @Override
            public void onPageChange(int newPage) {
                page = newPage;
                cargarInventario(txtBuscar.getText());
            }
        });

        JPanel panelAbajo = new JPanel(new BorderLayout(5, 5));
        panelAbajo.add(panelSur, BorderLayout.CENTER);
        panelAbajo.add(paginator, BorderLayout.PAGE_END);
        add(panelAbajo, BorderLayout.SOUTH);

        revalidate();
        repaint();
    }

    private void cargarInventario(String filtro) {
        modeloProductos.setRowCount(0);
        try {
            int totalItems = productoController.contarProductosFiltrados(filtro);
            totalPages = (int) Math.ceil(totalItems / (double) pageSize);
            if (totalPages < 1) totalPages = 1;
            if (page > totalPages) page = totalPages;
            if (paginator != null) paginator.setTotalPages(totalPages);

            List<Producto> productos = productoController.listarProductosPaginado(filtro, page, pageSize);
            for (Producto p : productos) {
                boolean activo = p.isActivo();
                modeloProductos.addRow(new Object[] {
                        p.getCodProducto(),
                        p.getNombreProd(),
                        p.getCodCat(),
                        String.format("%.2f", p.getPrecioProd()),
                        p.getStock(),
                        (activo ? "ACTIVO" : "INACTIVO") });
            }


            if (paginator != null) paginator.setPage(page);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar inventario: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void modalCambiarEstado() {
        int fila = tablaProductos.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto primero.", "Atención",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String codProd = (String) modeloProductos.getValueAt(fila, 0);
        String nombreProd = (String) modeloProductos.getValueAt(fila, 1);

        boolean estadoActual = false;
        Object estadoObj = modeloProductos.getValueAt(fila, 5);
        if (estadoObj != null) {
            estadoActual = "ACTIVO".equalsIgnoreCase(String.valueOf(estadoObj));
        }

        String[] opciones = { "ACTIVO", "INACTIVO" };
        String estadoSeleccionado = (String) JOptionPane.showInputDialog(
                this,
                "Estado del producto - " + nombreProd,
                "Cambiar Estado",
                JOptionPane.PLAIN_MESSAGE,
                null,
                opciones,
                estadoActual ? "ACTIVO" : "INACTIVO");

        if (estadoSeleccionado == null) {
            return;
        }

        boolean nuevoEstado = "ACTIVO".equalsIgnoreCase(estadoSeleccionado);

        try {
            productoController.actualizarActivo(codProd, nuevoEstado);
            // Confirmar con un reload inmediato
            cargarInventario(txtBuscar.getText());

            // Debug visual: re-leer el estado para la fila (aprox.)
            for (int i = 0; i < modeloProductos.getRowCount(); i++) {
                Object cod = modeloProductos.getValueAt(i, 0);
                if (cod != null && cod.toString().equalsIgnoreCase(codProd)) {
                    Object est = modeloProductos.getValueAt(i, 5);
                    JOptionPane.showMessageDialog(this,
                            "Estado guardado. Re-cargado = " + String.valueOf(est),
                            "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
            }

            JOptionPane.showMessageDialog(this, "Estado actualizado.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al actualizar estado: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void modalCrearProducto() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));

        JTextField txtNombre = new JTextField();
        JTextField txtStock = new JTextField();
        JTextField txtPrecio = new JTextField();

        // Selector de categorías
        JComboBox<String> cbxCategoria = new JComboBox<>();
        try {
            List<com.laptitefrance.delivery.models.Categoria> categorias = new com.laptitefrance.delivery.controllers.CategoriaController().listarCategorias();
            for (com.laptitefrance.delivery.models.Categoria c : categorias) {
                if (c != null) {
                    cbxCategoria.addItem(c.getCodCat() + " - " + c.getNombreCat());
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error cargando categorías: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JComboBox<String> cbxActivo = new JComboBox<>(new String[] { "ACTIVO", "INACTIVO" });

        panel.add(new JLabel("Nombre del producto:"));
        panel.add(txtNombre);
        panel.add(new JLabel("Stock:"));
        panel.add(txtStock);
        panel.add(new JLabel("Precio (S/):"));
        panel.add(txtPrecio);
        panel.add(new JLabel("Categoría:"));
        panel.add(cbxCategoria);
        panel.add(new JLabel("Estado:"));
        panel.add(cbxActivo);

        int opcion = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Nuevo Producto",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            String nombre = txtNombre.getText();
            int stock = Integer.parseInt(txtStock.getText().trim());
            double precio = Double.parseDouble(txtPrecio.getText().trim().replace(",", "."));

            String catSel = (String) cbxCategoria.getSelectedItem();
            String codCat = catSel == null ? "" : catSel.split(" - ")[0].trim();

            String estSel = (String) cbxActivo.getSelectedItem();
            boolean activo = "ACTIVO".equalsIgnoreCase(estSel);

            productoController.crearProducto(nombre, stock, precio, codCat, activo);
            JOptionPane.showMessageDialog(this, "Producto creado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            cargarInventario(txtBuscar.getText());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Stock y precio deben ser numéricos.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al crear producto: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void modalEliminarProducto() {
        int fila = tablaProductos.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto primero.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String codProd = (String) modeloProductos.getValueAt(fila, 0);
        String nombreProd = (String) modeloProductos.getValueAt(fila, 1);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "¿Seguro que desea eliminar el producto '" + nombreProd + "'?",
                "Confirmar eliminación",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.OK_OPTION) return;

        try {
            productoController.eliminarProducto(codProd);
            JOptionPane.showMessageDialog(this, "Producto eliminado.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            cargarInventario(txtBuscar.getText());
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al eliminar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void modalEditarPrecioStock() {

        int fila = tablaProductos.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un producto primero.", "Atención",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String codProd = (String) modeloProductos.getValueAt(fila, 0);
        String nombreProd = (String) modeloProductos.getValueAt(fila, 1);
        Object precioObj = modeloProductos.getValueAt(fila, 3);
        Object stockObj = modeloProductos.getValueAt(fila, 4);
        int stockActual;
        if (stockObj instanceof Short) {
            stockActual = ((Short) stockObj).intValue();
        } else if (stockObj instanceof Integer) {
            stockActual = (Integer) stockObj;
        } else {
            stockActual = Integer.parseInt(String.valueOf(stockObj));
        }


        double precioActual;
        try {
            precioActual = Double.parseDouble(precioObj.toString().replace(",", "."));
        } catch (Exception ex) {
            precioActual = 0.0;
        }

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        JTextField txtPrecio = new JTextField(String.format("%.2f", precioActual));
        JTextField txtStock = new JTextField(String.valueOf(stockActual));

        panel.add(new JLabel("Precio nuevo (S/):"));
        panel.add(txtPrecio);
        panel.add(new JLabel("Stock nuevo:"));
        panel.add(txtStock);

        int opcion = JOptionPane.showConfirmDialog(this, panel,
                "Editar Precio y Stock - " + nombreProd, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            double nuevoPrecio = Double.parseDouble(txtPrecio.getText().trim().replace(",", "."));
            int nuevoStock = Integer.parseInt(txtStock.getText().trim());

            productoController.actualizarPrecio(codProd, nuevoPrecio);
            productoController.actualizarStock(codProd, nuevoStock);

            cargarInventario(txtBuscar.getText());
            JOptionPane.showMessageDialog(this, "Producto actualizado.", "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Precio o stock inválidos.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al actualizar: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}


