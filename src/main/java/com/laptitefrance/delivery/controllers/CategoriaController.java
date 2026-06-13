package com.laptitefrance.delivery.controllers;

import java.util.List;
import java.util.Objects;

import com.laptitefrance.delivery.models.Categoria;
import com.laptitefrance.delivery.repositories.CategoriaRepository;

public class CategoriaController {

    private final CategoriaRepository categoriaRepository;

    public CategoriaController() {
        this(new CategoriaRepository());
    }

    public CategoriaController(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = Objects.requireNonNull(categoriaRepository);
    }

    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAll();
    }
}

