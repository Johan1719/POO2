package com.laptitefrance.delivery.services;

import com.laptitefrance.delivery.models.Producto;
import com.laptitefrance.delivery.repositories.ProductoRepository;

import java.util.List;
import java.util.stream.Collectors;

public class ProductoService {

    private final ProductoRepository productoRepo;

    public ProductoService() {
        this.productoRepo = new ProductoRepository();
    }

    public List<Producto> obtenerProductosConStock() {
        return productoRepo.findAll().stream()
                .filter(p -> p.getStock() > 0)
                .collect(Collectors.toList());
    }
}