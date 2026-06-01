package com.laptitefrance.delivery.views;

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

        // Pestaña 1: Nueva Venta (El panel refactorizado)
        PanelNuevaVenta panelNuevaVenta = new PanelNuevaVenta();
        tabbedPane.addTab("🛒 Nueva Venta", panelNuevaVenta);

        // Pestañas futuras (Placeholders para próximas tareas)
        tabbedPane.addTab("📺 Monitor de Pedidos", new JPanel());
        tabbedPane.addTab("👥 Clientes", new JPanel());
        tabbedPane.addTab("📦 Inventario", new JPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }
}