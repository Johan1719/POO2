package com.laptitefrance.delivery.services;

import com.laptitefrance.delivery.exceptions.DuplicateException;
import com.laptitefrance.delivery.exceptions.NotFoundException;
import com.laptitefrance.delivery.exceptions.ValidationException;
import com.laptitefrance.delivery.models.Cliente;
import com.laptitefrance.delivery.repositories.ClienteRepository;

import java.util.Optional;

public class ClienteService {

    private final ClienteRepository clienteRepo;

    public ClienteService() {
        this.clienteRepo = new ClienteRepository();
    }

    public Cliente buscarClientePorCelular(String celular) {
        return clienteRepo.findByTelefono(celular)
                .orElseThrow(() -> new NotFoundException("El cliente no existe en la BD."));
    }

    public Cliente registrarCliente(String nombre, String celular) {
        if (nombre.matches(".*\\d.*")) throw new ValidationException("El nombre no debe contener números.");
        if (!celular.matches("\\d{9}")) throw new ValidationException("El celular debe tener exactamente 9 dígitos.");

        Optional<Cliente> existente = clienteRepo.findByTelefono(celular);
        if (existente.isPresent()) {
            throw new DuplicateException("Este número ya pertenece a: " + existente.get().getNombreCliente());
        }

        String nuevoId = "C" + (int) (Math.random() * 900 + 100);
        Cliente nuevoCliente = new Cliente();
        nuevoCliente.setIdCliente(nuevoId);
        nuevoCliente.setNombreCliente(nombre);
        nuevoCliente.setNrocelular(celular);

        clienteRepo.insert(nuevoCliente);
        return nuevoCliente;
    }
}