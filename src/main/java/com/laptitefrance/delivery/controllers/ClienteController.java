package com.laptitefrance.delivery.controllers;

import com.laptitefrance.delivery.exceptions.ValidationException;
import com.laptitefrance.delivery.models.Cliente;
import com.laptitefrance.delivery.services.ClienteService;

public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController() {
        this.clienteService = new ClienteService();
    }

    public Cliente buscarClientePorCelular(String celular) {
        String cel = celular == null ? "" : celular.trim();
        if (cel.isEmpty()) {
            throw new ValidationException("Ingrese un número de celular.");
        }
        return clienteService.buscarClientePorCelular(cel);
    }

    public Cliente registrarCliente(String nombre, String celular) {
        String nom = nombre == null ? "" : nombre.trim();
        String cel = celular == null ? "" : celular.trim();

        if (nom.isEmpty()) throw new ValidationException("Debe ingresar un nombre.");
        return clienteService.registrarCliente(nom, cel);
    }
}