package com.laptitefrance.delivery.despacho;

import com.laptitefrance.delivery.models.Pedido;
import com.laptitefrance.delivery.repositories.IRepositorioBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demostración en vivo de la competencia por un pedido: N repartidores intentan
 * tomar el MISMO pedido a la vez y se comprueba que solo uno lo logra.
 *
 * Usa un repositorio en memoria propio (no requiere base de datos).
 */
public class SimuladorConcurrencia {

    /** Repo en memoria mínimo, autónomo para la demo. */
    static class RepoMemoria implements IRepositorioBase<Pedido, String> {
        private final ConcurrentHashMap<String, Pedido> datos = new ConcurrentHashMap<>();
        public void insert(Pedido e) { datos.put(e.getCodPedido(), e); }
        public Optional<Pedido> findById(String id) { return Optional.ofNullable(datos.get(id)); }
        public List<Pedido> findAll() { return new ArrayList<>(datos.values()); }
        public void update(Pedido e) { datos.put(e.getCodPedido(), e); }
        public void deleteById(String id) { datos.remove(id); }
    }

    public static void main(String[] args) throws InterruptedException {
        int repartidores = 10;

        RepoMemoria repo = new RepoMemoria();
        Pedido p = new Pedido();
        p.setCodPedido("P0001");
        p.setEstado("EN ESPERA");
        repo.insert(p);

        CentroDespacho centro = new CentroDespacho(repo);
        for (int i = 0; i < repartidores; i++) {
            centro.conectarRepartidor("E" + String.format("%03d", i));
        }

        ExecutorService pool = Executors.newFixedThreadPool(repartidores);
        CountDownLatch listos = new CountDownLatch(repartidores);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger ganadores = new AtomicInteger(0);

        for (int i = 0; i < repartidores; i++) {
            final String cod = "E" + String.format("%03d", i);
            pool.submit(() -> {
                listos.countDown();
                try {
                    salida.await();
                    ResultadoOperacion r = centro.tomarPedido("P0001", cod);
                    String marca = (r.getTipo() == ResultadoOperacion.Tipo.OK) ? "  <-- GANÓ" : "";
                    if (r.getTipo() == ResultadoOperacion.Tipo.OK) ganadores.incrementAndGet();
                    System.out.printf("Repartidor %s -> %s%s%n", cod, r.getTipo(), marca);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        listos.await();
        System.out.println("== Soltando " + repartidores + " repartidores a la vez ==");
        salida.countDown();
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("------------------------------------------");
        System.out.println("Ganadores (esperado = 1): " + ganadores.get());
        System.out.println("Estado final del pedido: " + repo.findById("P0001").get().getEstado());
    }
}
