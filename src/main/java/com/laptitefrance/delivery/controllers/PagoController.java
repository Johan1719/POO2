package com.laptitefrance.delivery.controllers;

import java.util.List;

import com.laptitefrance.delivery.models.Pago;
import com.laptitefrance.delivery.services.PagoService;

public class PagoController {

    private final PagoService pagoService;

    public PagoController() {
        this.pagoService = new PagoService();
    }

    public List<Pago> obtenerPagos() {
        return pagoService.obtenerPagos();
    }
}

