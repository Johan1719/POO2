package com.laptitefrance.delivery.controllers;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.laptitefrance.delivery.models.Producto;
import com.laptitefrance.delivery.repositories.IRepositorioBase;
import com.laptitefrance.delivery.repositories.ProductoRepository;

/**
 * CONTROLLER de productos (catálogo / menú).
 * Provee a la vista de venta solo los productos vendibles (con stock disponible).
 */
public class ProductoController {

    private final IRepositorioBase<Producto, String> productoRepository;

    public ProductoController() {
        this(new ProductoRepository());
    }

    /** Inyección de dependencias para permitir repositorios alternativos en pruebas. */
    public ProductoController(IRepositorioBase<Producto, String> productoRepository) {
        this.productoRepository = Objects.requireNonNull(productoRepository);
    }

    /**
     * Devuelve únicamente los productos con stock > 0. La regla "no mostrar lo agotado"
     * vive aquí (en el controller), no en la vista ni en la BD.
     *
     * @return List&lt;Producto&gt; con los productos disponibles; lista vacía si todo está agotado.
     */
    public List<Producto> obtenerProductosConStock() {
        return productoRepository.findAll().stream()
                .filter(p -> p != null)
                .filter(p -> p.getStock() > 0)
                .collect(Collectors.toList());
    }
}

