package com.laptitefrance.delivery.controllers;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.laptitefrance.delivery.models.Producto;
import com.laptitefrance.delivery.repositories.IRepositorioBase;
import com.laptitefrance.delivery.repositories.ProductoRepository;

public class ProductoController {

    private final IRepositorioBase<Producto, String> productoRepository;

    public ProductoController() {
        this(new ProductoRepository());
    }

    public ProductoController(IRepositorioBase<Producto, String> productoRepository) {
        this.productoRepository = Objects.requireNonNull(productoRepository);
    }

    public List<Producto> obtenerProductosConStock() {
        return productoRepository.findAll().stream()
                .filter(p -> p != null)
                .filter(p -> p.getStock() > 0)
                .collect(Collectors.toList());
    }
}

