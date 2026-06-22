package com.laptitefrance.delivery;

import com.laptitefrance.delivery.views.LoginView;

import javax.swing.SwingUtilities;

/**
 * Punto de entrada principal de la aplicación de escritorio.
 *
 * Arranca la interfaz gráfica (Swing) mostrando la ventana de Login.
 * Tras un inicio de sesión correcto, el flujo continúa hacia el
 * DashboardAsistenteView con sus distintas pestañas (Nueva Venta,
 * Monitor de Pedidos, Clientes e Inventario).
 *
 * NOTA: se usa el Look & Feel multiplataforma por defecto (Metal) de forma
 * intencional. El L&F del sistema (Windows) no pinta el color de fondo de los
 * JButton que usan setBackground()+setForeground(WHITE), dejando los botones
 * con el texto invisible. El L&F por defecto sí respeta esos colores.
 */
public class Main {

    public static void main(String[] args) {
        // Toda la interfaz de Swing debe construirse en el Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> new LoginView().setVisible(true));
    }
}
