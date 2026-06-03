package com.laptitefrance.delivery.controllers;

import java.util.Objects;

import com.laptitefrance.delivery.exceptions.DuplicateException;
import com.laptitefrance.delivery.exceptions.NotFoundException;
import com.laptitefrance.delivery.exceptions.ValidationException;
import com.laptitefrance.delivery.models.Cliente;
import com.laptitefrance.delivery.repositories.ClienteRepository;

public class ClienteController {

    private final ClienteRepository clienteRepository;

    public ClienteController() {
        this(new ClienteRepository());
    }

    public ClienteController(ClienteRepository clienteRepository) {
        this.clienteRepository = Objects.requireNonNull(clienteRepository);
    }

    public Cliente buscarClientePorCelular(String celular) {
        String cel = celular == null ? "" : celular.trim();

        if (cel.isEmpty()) {
            throw new ValidationException("Ingrese un número de celular.");
        }

        return clienteRepository.findByTelefono(cel)
                .orElseThrow(() -> new NotFoundException("El cliente no existe en la base de datos."));
    }

    public Cliente registrarCliente(String nombre, String celular) {
        String nom = nombre == null ? "" : nombre.trim();
        String cel = celular == null ? "" : celular.trim();

        if (nom.isEmpty()) {
            throw new ValidationException("Debe ingresar un nombre completo.");
        }
        if (cel.isEmpty()) {
            throw new ValidationException("Debe ingresar un número de celular.");
        }

        if (nom.length() > 30) {
            throw new ValidationException("El nombre es muy largo (Máximo 30 caracteres).");
        }

        if (!cel.matches("\\d{9}")) {
            throw new ValidationException("El número de celular debe tener exactamente 9 dígitos.");
        }

        if (nom.matches(".*\\d.*")) {
            throw new ValidationException("El nombre no debe contener números.");
        }

        clienteRepository.findByTelefono(cel).ifPresent(existente -> {
            throw new DuplicateException("Este número ya pertenece a: " + existente.getNombreCliente());
        });

        String nuevoId = "C" + (int) (Math.random() * 900 + 100);

        Cliente nuevoCliente = new Cliente();
        nuevoCliente.setIdCliente(nuevoId);
        nuevoCliente.setNombreCliente(nom);
        nuevoCliente.setNrocelular(cel);

        clienteRepository.insert(nuevoCliente);
        return nuevoCliente;
    }
}

