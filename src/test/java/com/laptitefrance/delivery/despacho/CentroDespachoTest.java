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
}
