package com.laptitefrance.delivery.controllers;

import com.laptitefrance.delivery.models.Producto;
import com.laptitefrance.delivery.repositories.ProductoRepository;

import java.util.List;
import java.util.stream.Collectors;

public class ProductoController {

    private final ProductoRepository productoRepo;

    public ProductoController() {
        this.productoRepo = new ProductoRepository();
    }

    public List<Producto> obtenerProductosConStock() {
        return productoRepo.findAll().stream()
                .filter(p -> p.getStock() > 0)
                .collect(Collectors.toList());
    }
}