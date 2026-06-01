package com.laptitefrance.delivery.controllers;

import com.laptitefrance.delivery.exceptions.DuplicateException;
import com.laptitefrance.delivery.exceptions.NotFoundException;
import com.laptitefrance.delivery.exceptions.ValidationException;
import com.laptitefrance.delivery.models.Cliente;
import com.laptitefrance.delivery.repositories.ClienteRepository;

import java.util.Optional;

public class ClienteController {

    private final ClienteRepository clienteRepo;

    public ClienteController() {
        this.clienteRepo = new ClienteRepository();
    }

    public Cliente buscarClientePorCelular(String celular) {
        String cel = celular == null ? "" : celular.trim();
        if (cel.isEmpty()) {
            throw new ValidationException("Ingrese un número de celular.");
        }

        return clienteRepo.findByTelefono(cel)
                .orElseThrow(() -> new NotFoundException("El cliente no existe en la BD."));
    }

    public Cliente registrarCliente(String nombre, String celular) {
        String nom = nombre == null ? "" : nombre.trim();
        String cel = celular == null ? "" : celular.trim();

        if (nom.isEmpty()) throw new ValidationException("Debe ingresar un nombre.");
        if (nom.matches(".*\\d.*")) throw new ValidationException("El nombre no debe contener números.");
        if (!cel.matches("\\d{9}")) throw new ValidationException("El celular debe tener exactamente 9 dígitos.");

        Optional<Cliente> existente = clienteRepo.findByTelefono(cel);
        if (existente.isPresent()) {
            throw new DuplicateException("Este número ya pertenece a: " + existente.get().getNombreCliente());
        }

        String nuevoId = "C" + (int) (Math.random() * 900 + 100);
        Cliente nuevoCliente = new Cliente();
        nuevoCliente.setIdCliente(nuevoId);
        nuevoCliente.setNombreCliente(nom);
        nuevoCliente.setNrocelular(cel);

        clienteRepo.insert(nuevoCliente);
        return nuevoCliente;
    }
}