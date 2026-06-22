package com.laptitefrance.delivery.despacho;

import com.laptitefrance.delivery.repositories.PedidoRepository;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;

import java.util.Map;

/**
 * Servidor web del repartidor. Sirve la página HTML (recursos en /web del
 * classpath) y los endpoints REST que la página consume. Delega en una única
 * instancia compartida de CentroDespacho (thread-safe). Reemplaza al antiguo
 * ApiDeliveryServer.
 */
public class ServidorWebRepartidor {

    public static final int PUERTO_DEFECTO = 8080;

    private final CentroDespacho centro;
    private Javalin app;

    public ServidorWebRepartidor(CentroDespacho centro) {
        this.centro = centro;
    }

    public void iniciar(int puerto) {
        app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
            config.staticFiles.add(staticFiles -> {
                staticFiles.directory = "/web";
                staticFiles.location = Location.CLASSPATH;
            });
        }).start(puerto);

        // Pedidos asignados a un repartidor, paginados.
        app.get("/api/repartidor/{cod}/pedidos", ctx -> {
            int page = parseIntOr(ctx.queryParam("page"), 1);
            int size = parseIntOr(ctx.queryParam("size"), 10);
            ctx.json(centro.pedidosDeRepartidor(ctx.pathParam("cod"), page, size));
        });

        // Confirmar entrega de un pedido.
        app.post("/api/pedidos/{cod}/entregar", ctx ->
                responder(ctx, centro.confirmarEntrega(ctx.pathParam("cod"), ctx.queryParam("repartidor"))));

        System.out.println("✅ Servidor web del repartidor en http://localhost:" + puerto + "/");
    }

    public void detener() {
        if (app != null) {
            app.stop();
        }
    }

    private static int parseIntOr(String valor, int defecto) {
        if (valor == null || valor.isBlank()) return defecto;
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return defecto;
        }
    }

    private static void responder(Context ctx, ResultadoOperacion r) {
        ctx.status(r.httpStatus());
        ctx.json(Map.of(
                "tipo", r.getTipo().name(),
                "mensaje", r.getMensaje() == null ? "" : r.getMensaje()));
    }

    /** Permite arrancar solo el servidor web (sin el dashboard). */
    public static void main(String[] args) {
        CentroDespacho centro = new CentroDespacho(new PedidoRepository());
        ServidorWebRepartidor servidor = new ServidorWebRepartidor(centro);
        servidor.iniciar(PUERTO_DEFECTO);
        Runtime.getRuntime().addShutdownHook(new Thread(servidor::detener));
    }
}
