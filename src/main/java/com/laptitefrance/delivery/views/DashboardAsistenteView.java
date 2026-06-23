package com.laptitefrance.delivery.views;

import com.laptitefrance.delivery.controllers.ClienteController;
import com.laptitefrance.delivery.controllers.ProductoController;
import com.laptitefrance.delivery.controllers.PedidoController;

import com.laptitefrance.delivery.models.Empleado;

import javax.swing.*;
import java.awt.*;

public class DashboardAsistenteView extends JFrame {

    private Empleado asistenteActual;

    public DashboardAsistenteView(Empleado empleado) {
        this.asistenteActual = empleado;

        configurarVentana();
        inicializarComponentes();
    }

    private void configurarVentana() {
        setTitle("Dashboard - Asistente: " + asistenteActual.getNombre());
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(240, 242, 245));
    }

    private void inicializarComponentes() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));

        // 👇 1. MAGIA: Creamos un ÚNICO controlador inyectándole el código del empleado que inició sesión
        PedidoController pedidoControllerGlobal = new PedidoController(asistenteActual.getCodEmpleado());

        // 👇 2. Inyectamos ese mismo controlador a la vista de Nueva Venta
        PanelNuevaVenta panelNuevaVenta = new PanelNuevaVenta(pedidoControllerGlobal);
        tabbedPane.addTab("🛒 Nueva Venta", panelNuevaVenta);

        // 👇 3. Conectamos la vista real del Monitor (le pasamos el mismo controlador)
        PanelMonitorPedidos panelMonitorPedidos = new PanelMonitorPedidos(pedidoControllerGlobal);
        tabbedPane.addTab("📺 Monitor de Pedidos", panelMonitorPedidos);

        // Pestañas Clientes / Inventario (YA implementadas)
        PanelClientes panelClientes = new PanelClientes(new ClienteController());
        tabbedPane.addTab("👥 Clientes", panelClientes);

        PanelInventario panelInventario = new PanelInventario(new ProductoController());
        tabbedPane.addTab("📦 Inventario", panelInventario);

        // Al cambiar de pestaña, refrescar el panel que se muestra para reflejar
        // cambios de stock hechos en otra pestaña (ventas <-> inventario).
        tabbedPane.addChangeListener(e -> {
            Component seleccionado = tabbedPane.getSelectedComponent();
            if (seleccionado == panelInventario) {
                panelInventario.refrescar();
            } else if (seleccionado == panelNuevaVenta) {
                panelNuevaVenta.recargarMenu();
            }
        });

        add(tabbedPane, BorderLayout.CENTER);
    }
}