package com.laptitefrance.delivery.controllers;

import java.util.List;

import com.laptitefrance.delivery.models.Tarifa;
import com.laptitefrance.delivery.services.TarifaService;

public class TarifaController {

    private final TarifaService tarifaService;

    public TarifaController() {
        this.tarifaService = new TarifaService();
    }

    public List<Tarifa> obtenerTarifas() {
        return tarifaService.obtenerTarifas();
    }
}

