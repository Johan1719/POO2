package com.laptitefrance.delivery.controllers;

import java.util.List;
import java.util.Objects;

import com.laptitefrance.delivery.repositories.RepartidorMonitorRepository;
import com.laptitefrance.delivery.dtos.RepartidorMonitorRow;




/**
 * Controller específico para el módulo de monitor de repartidores.
 * 
 * Objetivo: que las Views no dependan de repositorios.
 */
public class RepartidorMonitorController {

    private final RepartidorMonitorRepository repartidorMonitorRepository;

    public RepartidorMonitorController() {
        this(new RepartidorMonitorRepository());
    }

    public RepartidorMonitorController(RepartidorMonitorRepository repartidorMonitorRepository) {
        this.repartidorMonitorRepository = Objects.requireNonNull(repartidorMonitorRepository);
    }

    public List<RepartidorMonitorRow> listarRepartidoresConEstado() {
        return repartidorMonitorRepository.listarRepartidoresConEstado();
    }


}

