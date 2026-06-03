package com.laptitefrance.delivery.controllers;

import java.util.List;
import java.util.Objects;

import com.laptitefrance.delivery.models.Tarifa;
import com.laptitefrance.delivery.repositories.IRepositorioBase;
import com.laptitefrance.delivery.repositories.TarifaRepository;

/**
 * CONTROLLER de tarifas de envío.
 * Provee a la vista de venta las tarifas disponibles (cada una con su costo y
 * tiempo promedio) para calcular el envío del pedido.
 */
public class TarifaController {

    private final IRepositorioBase<Tarifa, String> tarifaRepository;

    public TarifaController() {
        this(new TarifaRepository());
    }

    /** Inyección de dependencias para permitir repositorios alternativos en pruebas. */
    public TarifaController(IRepositorioBase<Tarifa, String> tarifaRepository) {
        this.tarifaRepository = Objects.requireNonNull(tarifaRepository);
    }

    /**
     * Lista todas las tarifas de envío registradas en la BD.
     *
     * @return List&lt;Tarifa&gt; con las tarifas disponibles; vacía si no hay ninguna.
     */
    public List<Tarifa> obtenerTarifas() {
        return tarifaRepository.findAll();
    }

}


