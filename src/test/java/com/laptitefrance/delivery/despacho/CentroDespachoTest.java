package com.laptitefrance.delivery.despacho;

import com.laptitefrance.delivery.models.Pedido;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CentroDespachoTest {

    private Pedido pedidoEnEspera(String cod) {
        Pedido p = new Pedido();
        p.setCodPedido(cod);
        p.setEstado("EN ESPERA");
        return p;
    }

    @Test
    void conectarYDesconectarRepartidor() {
        CentroDespacho centro = new CentroDespacho(new FakePedidoRepository());

        assertEquals(ResultadoOperacion.Tipo.OK, centro.conectarRepartidor("E004").getTipo());
        assertEquals(1, centro.listarDisponibles().size());

        assertEquals(ResultadoOperacion.Tipo.OK, centro.desconectarRepartidor("E004").getTipo());
        assertEquals(0, centro.listarDisponibles().size());
    }

    @Test
    void tomarPedidoInexistenteDevuelveNoEncontrado() {
        CentroDespacho centro = new CentroDespacho(new FakePedidoRepository());
        centro.conectarRepartidor("E004");

        ResultadoOperacion r = centro.tomarPedido("P9999", "E004");
        assertEquals(ResultadoOperacion.Tipo.NO_ENCONTRADO, r.getTipo());
    }

    @Test
    void variosRepartidoresCompitenSoloUnoGana() throws InterruptedException {
        FakePedidoRepository repo = new FakePedidoRepository();
        repo.insert(pedidoEnEspera("P0001"));
        CentroDespacho centro = new CentroDespacho(repo);

        int hilos = 12;
        for (int i = 0; i < hilos; i++) {
            centro.conectarRepartidor("E" + String.format("%03d", i));
        }

        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch listos = new CountDownLatch(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger oks = new AtomicInteger(0);
        AtomicInteger yaTomados = new AtomicInteger(0);

        for (int i = 0; i < hilos; i++) {
            final String cod = "E" + String.format("%03d", i);
            pool.submit(() -> {
                listos.countDown();
                try {
                    salida.await();
                    ResultadoOperacion r = centro.tomarPedido("P0001", cod);
                    if (r.getTipo() == ResultadoOperacion.Tipo.OK) oks.incrementAndGet();
                    else if (r.getTipo() == ResultadoOperacion.Tipo.YA_TOMADO) yaTomados.incrementAndGet();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        listos.await();
        salida.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(1, oks.get());
        assertEquals(hilos - 1, yaTomados.get());
        assertEquals("EN CAMINO", repo.findById("P0001").get().getEstado());
    }

    @Test
    void entregarLiberaAlRepartidor() {
        FakePedidoRepository repo = new FakePedidoRepository();
        repo.insert(pedidoEnEspera("P0001"));
        CentroDespacho centro = new CentroDespacho(repo);
        centro.conectarRepartidor("E004");

        assertEquals(ResultadoOperacion.Tipo.OK, centro.tomarPedido("P0001", "E004").getTipo());
        assertEquals(EstadoRepartidor.OCUPADO, centro.listarDisponibles().get(0).getEstado());

        ResultadoOperacion entrega = centro.entregarPedido("P0001", "E004");
        assertEquals(ResultadoOperacion.Tipo.OK, entrega.getTipo());
        assertEquals("ENTREGADO", repo.findById("P0001").get().getEstado());
        assertEquals(EstadoRepartidor.LIBRE, centro.listarDisponibles().get(0).getEstado());
    }

    @Test
    void asignacionAutomaticaAsignaPedidoEncolado() throws InterruptedException {
        FakePedidoRepository repo = new FakePedidoRepository();
        repo.insert(pedidoEnEspera("P0001"));
        CentroDespacho centro = new CentroDespacho(repo);
        centro.conectarRepartidor("E004");
        centro.encolarPedido("P0001");

        centro.iniciar();
        try {
            // Esperar (con timeout) a que el hilo despachador asigne.
            long limite = System.currentTimeMillis() + 3000;
            while (!"EN CAMINO".equals(repo.findById("P0001").get().getEstado())
                    && System.currentTimeMillis() < limite) {
                Thread.sleep(50);
            }
        } finally {
            centro.detener();
        }

        assertEquals("EN CAMINO", repo.findById("P0001").get().getEstado());
        assertEquals("E004", repo.findById("P0001").get().getCodRepartidor());
    }

    @Test
    void tomarPedidoConCodigoNuloNoLanza() {
        CentroDespacho centro = new CentroDespacho(new FakePedidoRepository());
        ResultadoOperacion r = centro.tomarPedido(null, "E004");
        assertEquals(ResultadoOperacion.Tipo.NO_ENCONTRADO, r.getTipo());
    }

    @Test
    void tomarPedidoConRepartidorNuloNoLanza() {
        CentroDespacho centro = new CentroDespacho(new FakePedidoRepository());
        ResultadoOperacion r = centro.tomarPedido("P0001", null);
        assertEquals(ResultadoOperacion.Tipo.REPARTIDOR_NO_DISPONIBLE, r.getTipo());
    }
}
