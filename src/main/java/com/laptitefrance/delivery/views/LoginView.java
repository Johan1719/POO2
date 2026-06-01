package com.laptitefrance.delivery.views;

import com.laptitefrance.delivery.controllers.LoginController;
import com.laptitefrance.delivery.exceptions.NotFoundException;
import com.laptitefrance.delivery.exceptions.ValidationException;
import com.laptitefrance.delivery.models.Empleado;




import javax.swing.*;
import java.awt.*;


public class LoginView extends JFrame {

    private final LoginController loginController;
    private JTextField txtCodigoEmpleado;
    private JButton btnIngresar;


    public LoginView() {
        this.loginController = new LoginController();
        configurarVentana();
        inicializarComponentes();
    }


    private void configurarVentana() {
        setTitle("La P'tite France - Acceso al Sistema");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centra la ventana en la pantalla
        setResizable(false);
        setLayout(null); // Usamos posicionamiento absoluto para un diseño rápido
        getContentPane().setBackground(new Color(240, 242, 245));
    }

    private void inicializarComponentes() {
        // Título principal
        JLabel lblTitulo = new JLabel("PUNTO DE VENTA", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 22));
        lblTitulo.setBounds(0, 30, 400, 30);
        add(lblTitulo);

        // Subtítulo
        JLabel lblSub = new JLabel("Ingrese su código de empleado:", SwingConstants.CENTER);
        lblSub.setFont(new Font("Arial", Font.PLAIN, 14));
        lblSub.setBounds(0, 70, 400, 20);
        add(lblSub);

        // Caja de texto para el código (Ej: E001)
        txtCodigoEmpleado = new JTextField();
        txtCodigoEmpleado.setFont(new Font("Arial", Font.BOLD, 16));
        txtCodigoEmpleado.setHorizontalAlignment(JTextField.CENTER);
        txtCodigoEmpleado.setBounds(100, 110, 200, 40);
        add(txtCodigoEmpleado);

        // Botón de Ingreso
        btnIngresar = new JButton("Ingresar");
        btnIngresar.setFont(new Font("Arial", Font.BOLD, 14));
        btnIngresar.setBackground(new Color(46, 204, 113)); // Verde moderno
        btnIngresar.setForeground(Color.WHITE);
        btnIngresar.setFocusPainted(false);
        btnIngresar.setBounds(100, 170, 200, 40);
        
        // ACCIÓN DEL BOTÓN (Uso de Lambda)
        btnIngresar.addActionListener(e -> validarLogin());
        
        add(btnIngresar);
    }

    private void validarLogin() {
        String codigoIngresado = txtCodigoEmpleado.getText().trim();

        if (codigoIngresado.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, ingrese un código.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Empleado emp = loginController.login(codigoIngresado);
            JOptionPane.showMessageDialog(this, "¡Bienvenido, " + emp.getNombre() + "!", "Acceso Concedido", JOptionPane.INFORMATION_MESSAGE);

            this.dispose();
            new DashboardAsistenteView(emp).setVisible(true);
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Atención", JOptionPane.WARNING_MESSAGE);
        } catch (NotFoundException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de Acceso", JOptionPane.ERROR_MESSAGE);
            txtCodigoEmpleado.setText("");
        }

    }

    // Método Main para probar la ventana directamente
    public static void main(String[] args) {
        // Aseguramos que la ventana se cree en el hilo correcto de Swing
        SwingUtilities.invokeLater(() -> {
            new LoginView().setVisible(true);
        });
    }
}