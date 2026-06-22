package com.laptitefrance.delivery.despacho;

import com.laptitefrance.delivery.repositories.PedidoRepository;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Map;

/**
 * Servidor REST (Javalin) de la API de delivery concurrente. Mantiene una única
 * instancia de CentroDespacho compartida por todos los endpoints (los hilos de
 * Javalin atienden las peticiones en paralelo; el CentroDespacho es thread-safe).
 */
public class ApiDeliveryServer {

    private static final int PUERTO = 8080;

    public static void main(String[] args) {
        CentroDespacho centro = new CentroDespacho(new PedidoRepository());
        centro.iniciar();
        Runtime.getRuntime().addShutdownHook(new Thread(centro::detener));

        Javalin app = Javalin.create(config ->
                config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()))
        ).start(PUERTO);

        app.post("/api/repartidores/{cod}/conectar", ctx ->
                responder(ctx, centro.conectarRepartidor(ctx.pathParam("cod"))));

        app.post("/api/repartidores/{cod}/desconectar", ctx ->
                responder(ctx, centro.desconectarRepartidor(ctx.pathParam("cod"))));

        app.get("/api/repartidores/disponibles", ctx ->
                ctx.json(centro.listarDisponibles()));

        app.get("/api/pedidos/pendientes", ctx ->
                ctx.json(Map.of("pendientesEnCola", centro.pendientesEnCola())));

        app.post("/api/pedidos/{cod}/tomar", ctx ->
                responder(ctx, centro.tomarPedido(ctx.pathParam("cod"), ctx.queryParam("repartidor"))));

        app.post("/api/pedidos/{cod}/entregar", ctx ->
                responder(ctx, centro.entregarPedido(ctx.pathParam("cod"), ctx.queryParam("repartidor"))));

        app.get("/api/despacho/estado", ctx ->
                ctx.json(centro.estadoDespacho()));

        System.out.println("✅ API de delivery encendida en http://localhost:" + PUERTO + "/api");
    }

    /** Traduce un ResultadoOperacion a estado HTTP + cuerpo JSON uniforme. */
    private static void responder(Context ctx, ResultadoOperacion r) {
        ctx.status(r.httpStatus());
        ctx.json(Map.of(
                "tipo", r.getTipo().name(),
                "mensaje", r.getMensaje() == null ? "" : r.getMensaje()));
    }
}
