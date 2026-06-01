package com.laptitefrance.delivery.controllers;

import io.javalin.Javalin;
import java.util.List;
import java.util.Map;

public class ApiRepartidor {
    public static void main(String[] args) {
        
        // 1. Encendemos el servidor web en el puerto 8080
        Javalin app = Javalin.create(config -> {
            // Esto permite que cualquier dispositivo (como tu celular) se conecte
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost())); 
        }).start(8080);

        // 2. Creamos la ruta web (Endpoint)
        app.get("/api/pedidos-pendientes", ctx -> {
            
            // AQUÍ LUEGO PONDREMOS: pedidoRepo.listarTodos()
            // Por ahora, simulamos lo que devolvería SQL Server para probar la conexión:
            List<Map<String, String>> datosSimulados = List.of(
                Map.of("codPedido", "PD001", "idCliente", "C002", "estado", "EN CAMINO"),
                Map.of("codPedido", "PD002", "idCliente", "C005", "estado", "EN CAMINO")
            );
            
            // Javalin convierte automáticamente esa lista en formato JSON
            ctx.json(datosSimulados); 
        });
        
        System.out.println("✅ Servidor encendido en http://localhost:8080/api/pedidos-pendientes");
    }
}