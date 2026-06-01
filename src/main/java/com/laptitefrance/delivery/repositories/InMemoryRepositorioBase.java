package com.laptitefrance.delivery.repositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class InMemoryRepositorioBase<T, ID> implements IRepositorioBase<T, ID> {

    private final Map<ID, T> store = new ConcurrentHashMap<>();
    private final Function<T, ID> idExtractor;

    public InMemoryRepositorioBase(Function<T, ID> idExtractor) {
        this.idExtractor = idExtractor;
    }

    @Override
    public void insert(T entity) {
        ID id = idExtractor.apply(entity);
        store.put(id, entity);
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void update(T entity) {
        insert(entity);
    }

    @Override
    public void deleteById(ID id) {
        store.remove(id);
    }
}

