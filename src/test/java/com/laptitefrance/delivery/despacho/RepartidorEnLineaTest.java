package com.laptitefrance.delivery.despacho;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RepartidorEnLineaTest {

    @Test
    void soloUnHiloLograOcuparElRepartidor() throws InterruptedException {
        RepartidorEnLinea rep = new RepartidorEnLinea("E004");
        int hilos = 20;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch listos = new CountDownLatch(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger(0);

        for (int i = 0; i < hilos; i++) {
            pool.submit(() -> {
                listos.countDown();
                try {
                    salida.await();
                    if (rep.intentarOcupar("P0001")) {
                        exitos.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        listos.await();
        salida.countDown(); // soltar todos a la vez
        pool.shutdown();
        pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        assertEquals(1, exitos.get());
        assertEquals(EstadoRepartidor.OCUPADO, rep.getEstado());
        assertEquals("P0001", rep.getCodPedidoActual());
    }
}
