package com.laptitefrance.delivery.controllers;

import java.util.List;
import java.util.Objects;

import com.laptitefrance.delivery.models.Pago;
import com.laptitefrance.delivery.repositories.IRepositorioBase;
import com.laptitefrance.delivery.repositories.PagoRepository;

/**
 * CONTROLLER de métodos de pago.
 * Su única tarea es ofrecer a la vista la lista de formas de pago disponibles
 * (efectivo, tarjeta, etc.) para poblar el combo de la pantalla de venta.
 */
public class PagoController {

    private final IRepositorioBase<Pago, String> pagoRepository;

    public PagoController() {
        this(new PagoRepository());
    }

    /** Inyección de dependencias para permitir repositorios alternativos en pruebas. */
    public PagoController(IRepositorioBase<Pago, String> pagoRepository) {
        this.pagoRepository = Objects.requireNonNull(pagoRepository);
    }

    /**
     * Lista todos los métodos de pago registrados en la BD.
     *
     * @return List&lt;Pago&gt; con los métodos de pago disponibles; vacía si no hay ninguno.
     */
    public List<Pago> obtenerPagos() {
        return pagoRepository.findAll();
    }
}


