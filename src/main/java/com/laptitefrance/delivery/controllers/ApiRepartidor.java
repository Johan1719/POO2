package com.laptitefrance.delivery.controllers;

import com.laptitefrance.delivery.dtos.PedidoRepartidorApiRow;
import com.laptitefrance.delivery.repositories.RepartidorPedidosRepository;

import io.javalin.Javalin;

import java.util.List;
import java.util.Map;


/**
 * API simple (Javalin) para que un repartidor/empleado externo vea sus pedidos.
 */
public class ApiRepartidor {

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
        }).start(8080);

        // GET: pedidos asignados a un repartidor (incluye dirección textual y cliente)
        app.get("/api/repartidores/{codRepartidor}/pedidos", ctx -> {
            String codRepartidor = ctx.pathParam("codRepartidor");
            if (codRepartidor == null || codRepartidor.trim().isEmpty()) {
                ctx.status(400).json(Map.of("error", "codRepartidor inválido"));
                return;
            }

            // Mostrar SOLO pedidos pendientes del repartidor (definido como EN CAMINO)
            try {
                RepartidorPedidosRepository repo = new RepartidorPedidosRepository();
                List<PedidoRepartidorApiRow> pedidos = listarPedidosPorRepartidor(repo, codRepartidor.trim(), "EN CAMINO", 1, 1000);


                ctx.json(Map.of("codRepartidor", codRepartidor, "pedidos", pedidos));
            } catch (Throwable ex) {

                // Depuración: devolver el error exacto para corregir el 500.
                String msg = ex.getMessage();
                try {
                    if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                        msg = ex.getCause().getMessage();
                    }
                } catch (Exception ignore) {}

                ctx.status(500).json(Map.of(
                        "error", msg,
                        "estadoFiltro", "EN CAMINO",
                        "codRepartidor", codRepartidor,
                        "exType", ex.getClass().getName()
                ));
            }



        });

        // POST: marcar como entregado (pre-confirmación la hace el frontend)
        app.post("/api/repartidores/{codRepartidor}/pedidos/{codPedido}/entregar", ctx -> {
            String codRepartidor = ctx.pathParam("codRepartidor");
            String codPedido = ctx.pathParam("codPedido");

            if (codRepartidor == null || codRepartidor.trim().isEmpty() || codPedido == null || codPedido.trim().isEmpty()) {
                ctx.status(400).json(Map.of("error", "codRepartidor/codPedido inválidos"));
                return;
            }

            // Validar que pertenece al repartidor
            RepartidorPedidosRepository repo = new RepartidorPedidosRepository();
            boolean actualizado = repo.marcarEntregadoSiPertenece(codPedido.trim(), codRepartidor.trim());

            if (!actualizado) {
                ctx.status(404).json(Map.of("error", "No existe el pedido o no pertenece al repartidor"));
                return;
            }

            ctx.json(Map.of("ok", true, "codPedido", codPedido.trim(), "estado", "ENTREGADO"));
        });

        System.out.println("✅ Servidor encendido en http://localhost:8080");
        System.out.println("✅ GET  /api/repartidores/{codRepartidor}/pedidos");
        System.out.println("✅ POST /api/repartidores/{codRepartidor}/pedidos/{codPedido}/entregar");
    }

    // ===== API mapping (sin SQL) =====

    private static List<PedidoRepartidorApiRow> listarPedidosPorRepartidor(

            RepartidorPedidosRepository repo,
            String codRepartidor,
            String estadoFiltro,
            int page,
            int pageSize
    ) {
        return repo.listarPedidosPorRepartidor(codRepartidor, estadoFiltro, page, pageSize)
                .stream()
                .map(r -> {
                    PedidoRepartidorApiRow row = new PedidoRepartidorApiRow();
                    row.codPedido = r.codPedido;
                    row.montoPedido = r.montoPedido;
                    row.estado = r.estado;
                    row.direccionEntrega = r.direccionEntrega;
                    row.ubicacionEntrega = r.ubicacionEntrega;
                    row.fechaSolicitud = r.fechaSolicitud;
                    row.tiempoEntEstimado = r.tiempoEntEstimado;
                    row.nombreCliente = r.nombreCliente;
                    return row;

                })
                .toList();
    }

    // ===== DTOs API internos =====


}



