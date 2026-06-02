package com.laptitefrance.delivery.services;

import java.util.List;
import java.util.Objects;

import com.laptitefrance.delivery.models.Pago;
import com.laptitefrance.delivery.repositories.IRepositorioBase;
import com.laptitefrance.delivery.repositories.PagoRepository;

public class PagoService {

    private final IRepositorioBase<Pago, String> pagoRepository;

    public PagoService() {
        this.pagoRepository = new PagoRepository();
    }

    public PagoService(IRepositorioBase<Pago, String> pagoRepository) {
        this.pagoRepository = Objects.requireNonNull(pagoRepository);
    }

    public List<Pago> obtenerPagos() {
        return pagoRepository.findAll();
    }
}

