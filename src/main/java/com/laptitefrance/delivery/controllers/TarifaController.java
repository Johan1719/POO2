package com.laptitefrance.delivery.controllers;

import java.util.List;
import java.util.Objects;

import com.laptitefrance.delivery.models.Tarifa;
import com.laptitefrance.delivery.repositories.IRepositorioBase;
import com.laptitefrance.delivery.repositories.TarifaRepository;

public class TarifaController {

    private final IRepositorioBase<Tarifa, String> tarifaRepository;

    public TarifaController() {
        this(new TarifaRepository());
    }

    public TarifaController(IRepositorioBase<Tarifa, String> tarifaRepository) {
        this.tarifaRepository = Objects.requireNonNull(tarifaRepository);
    }

    public List<Tarifa> obtenerTarifas() {
        return tarifaRepository.findAll();
    }

}


