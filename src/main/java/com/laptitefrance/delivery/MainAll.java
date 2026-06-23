package com.laptitefrance.delivery;

import com.laptitefrance.delivery.controllers.ApiRepartidor;
import com.laptitefrance.delivery.views.LoginView;

import javax.swing.SwingUtilities;


/**
 * Main único para arrancar el sistema (API REST + ventana de login Swing).
 */
public class MainAll {

    public static void main(String[] args) {
        // 1) API REST + frontend en /repartidor (Javalin queda escuchando en :8080).
        ApiRepartidor.main(args);

        // 2) Ventana de login Swing en el Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> {
            try {
                new LoginView().setVisible(true);
            } catch (Throwable t) {
                // No silenciar: si la ventana falla (BD, entorno headless, etc.) hay que verlo.
                t.printStackTrace();
            }
        });
    }
}


