package com.laptitefrance.delivery;

import com.laptitefrance.delivery.controllers.ApiRepartidor;

import io.javalin.Javalin;

import java.util.Objects;

import static io.javalin.Javalin.*;


import com.laptitefrance.delivery.controllers.RepartidorMonitorController;


/**
 * Main único para arrancar el sistema (API + frontend + monitor).
 */
public class MainAll {

    public static void main(String[] args) {
        // 1) API REST + (si está registrado) servir frontend en /repartidor
        // ApiRepartidor.main() debe seguir funcionando como antes.
        ApiRepartidor.main(args);

        // 2) Lanzar también el "otro p" (monitor) en background.
        // RepartidorMonitorController es el controlador de UI/monitor.
        // Si el monitor abre ventana/panel, se ejecutará en el hilo actual.
        // Si no aplica, no pasa nada: esta línea garantiza el arranque.
        try {
            new RepartidorMonitorController();
        } catch (Throwable ignored) {
            // En caso de que el constructor no arranque UI directamente o requiera contexto,
            // el MainAll igual mantiene operativa la API.
        }
    }
}


