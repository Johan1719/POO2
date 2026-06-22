package com.laptitefrance.delivery;

import com.laptitefrance.delivery.despacho.CentroDespacho;
import com.laptitefrance.delivery.despacho.ServidorWebRepartidor;
import com.laptitefrance.delivery.repositories.PedidoRepository;
import com.laptitefrance.delivery.views.LoginView;

import javax.swing.SwingUtilities;

/**
 * Punto de entrada único de la aplicación. Inicializa, en el mismo proceso:
 *  - el servidor web del repartidor (Javalin, puerto 8080), y
 *  - la interfaz de escritorio del asistente (Swing), empezando por el login.
 *
 * Ambos comparten la misma base de datos; el servidor web usa su propio
 * CentroDespacho (thread-safe) para coordinar la confirmación de entrega.
 *
 * Se usa el Look & Feel multiplataforma por defecto a propósito: el L&F del
 * sistema (Windows) no pinta el color de fondo de los JButton, dejando el texto
 * invisible.
 */
public class Main {

    public static void main(String[] args) {
        // 1. Servidor web del repartidor (hilos propios de Javalin).
        CentroDespacho centro = new CentroDespacho(new PedidoRepository());
        ServidorWebRepartidor servidor = new ServidorWebRepartidor(centro);
        servidor.iniciar(ServidorWebRepartidor.PUERTO_DEFECTO);
        Runtime.getRuntime().addShutdownHook(new Thread(servidor::detener));

        // 2. Interfaz de escritorio del asistente (Event Dispatch Thread).
        SwingUtilities.invokeLater(() -> new LoginView().setVisible(true));
    }
}
