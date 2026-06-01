package com.laptitefrance.delivery.repositories;

import java.util.List;
import java.util.Optional;

public interface IRepositorioBase<T, ID> {
    void insert(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    void update(T entity);
    void deleteById(ID id);
}

