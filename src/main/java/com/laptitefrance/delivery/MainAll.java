package com.laptitefrance.delivery;

import com.laptitefrance.delivery.controllers.ApiRepartidor;
import com.laptitefrance.delivery.views.LoginView;

import javax.swing.SwingUtilities;


/**
 * Punto de entrada ÚNICO del sistema. Arranca las dos caras de la aplicación:
 *  1) La API REST (Javalin) que consume el repartidor desde el navegador.
 *  2) La aplicación de escritorio Swing (ventana de login) que usa el personal de tienda.
 *
 * Ambas conviven en el mismo proceso: la API queda escuchando en un hilo propio
 * mientras la interfaz gráfica corre en el hilo de eventos de Swing.
 */
public class MainAll {

    /**
     * Lógica: primero levanta el servidor web (no bloquea, queda escuchando en :8080)
     * y luego abre la ventana de login en el Event Dispatch Thread (EDT), que es el
     * hilo obligatorio para crear/mostrar componentes Swing.
     */
    public static void main(String[] args) {
        // 1) API REST + frontend del repartidor en /repartidor (Javalin escucha en :8080).
        ApiRepartidor.main(args);

        // 2) Ventana de login Swing. invokeLater garantiza que la UI se cree en el EDT.
        SwingUtilities.invokeLater(() -> {
            try {
                new LoginView().setVisible(true);
            } catch (Throwable t) {
                // No se silencia: si la ventana falla (BD caída, entorno sin pantalla, etc.)
                // queremos ver el error en consola en vez de un arranque "fantasma".
                t.printStackTrace();
            }
        });
    }
}


