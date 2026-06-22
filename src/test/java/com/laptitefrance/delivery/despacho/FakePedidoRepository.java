package com.laptitefrance.delivery.despacho;

import com.laptitefrance.delivery.models.Pedido;
import com.laptitefrance.delivery.repositories.IRepositorioBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Repositorio en memoria, thread-safe, para pruebas sin base de datos. */
public class FakePedidoRepository implements IRepositorioBase<Pedido, String> {

    private final ConcurrentHashMap<String, Pedido> datos = new ConcurrentHashMap<>();
    private final AtomicInteger secuencia = new AtomicInteger(0);

    @Override
    public void insert(Pedido entity) {
        if (entity.getCodPedido() == null) {
            entity.setCodPedido(String.format("P%04d", secuencia.incrementAndGet()));
        }
        datos.put(entity.getCodPedido(), entity);
    }

    @Override
    public Optional<Pedido> findById(String id) {
        return Optional.ofNullable(datos.get(id));
    }

    @Override
    public List<Pedido> findAll() {
        return new ArrayList<>(datos.values());
    }

    @Override
    public void update(Pedido entity) {
        datos.put(entity.getCodPedido(), entity);
    }

    @Override
    public void deleteById(String id) {
        datos.remove(id);
    }
}
