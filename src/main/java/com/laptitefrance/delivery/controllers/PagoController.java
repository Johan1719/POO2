package com.laptitefrance.delivery.controllers;

import java.util.List;
import java.util.Objects;

import com.laptitefrance.delivery.models.Pago;
import com.laptitefrance.delivery.repositories.IRepositorioBase;
import com.laptitefrance.delivery.repositories.PagoRepository;

public class PagoController {

    private final IRepositorioBase<Pago, String> pagoRepository;

    public PagoController() {
        this(new PagoRepository());
    }

    public PagoController(IRepositorioBase<Pago, String> pagoRepository) {
        this.pagoRepository = Objects.requireNonNull(pagoRepository);
    }

    public List<Pago> obtenerPagos() {
        return pagoRepository.findAll();
    }
}


