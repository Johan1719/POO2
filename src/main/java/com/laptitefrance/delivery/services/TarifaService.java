package com.laptitefrance.delivery.services;

import java.util.List;
import java.util.Objects;

import com.laptitefrance.delivery.models.Tarifa;
import com.laptitefrance.delivery.repositories.IRepositorioBase;
import com.laptitefrance.delivery.repositories.TarifaRepository;

public class TarifaService {

    private final IRepositorioBase<Tarifa, String> tarifaRepository;

    public TarifaService() {
        this.tarifaRepository = new TarifaRepository();
    }

    public TarifaService(IRepositorioBase<Tarifa, String> tarifaRepository) {
        this.tarifaRepository = Objects.requireNonNull(tarifaRepository);
    }

    public List<Tarifa> obtenerTarifas() {
        return tarifaRepository.findAll();
    }
}

