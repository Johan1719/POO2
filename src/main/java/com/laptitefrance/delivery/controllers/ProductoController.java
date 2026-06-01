package com.laptitefrance.delivery.controllers;

import com.laptitefrance.delivery.models.Producto;
import com.laptitefrance.delivery.services.ProductoService;

import java.util.List;

public class ProductoController {

    private final ProductoService productoService;

    public ProductoController() {
        this.productoService = new ProductoService();
    }

    public List<Producto> obtenerProductosConStock() {
        return productoService.obtenerProductosConStock();
    }
}