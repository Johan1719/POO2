package com.laptitefrance.delivery.controllers;

import java.util.List;
import java.util.Objects;

import com.laptitefrance.delivery.exceptions.DuplicateException;

import com.laptitefrance.delivery.exceptions.NotFoundException;
import com.laptitefrance.delivery.exceptions.ValidationException;
import com.laptitefrance.delivery.models.Cliente;
import com.laptitefrance.delivery.repositories.ClienteRepository;
import com.laptitefrance.delivery.repositories.ClienteRepositoryPagination;



public class ClienteController {

    private final ClienteRepository clienteRepository;

    private static final int DEFAULT_PAGE_SIZE = 10;

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

        Cliente nuevoCliente = new Cliente();
        // IDCliente se autogenera desde SQL (DEFAULT con SEQUENCE).
        nuevoCliente.setNombreCliente(nom);
        nuevoCliente.setNrocelular(cel);

        clienteRepository.insert(nuevoCliente);
        return nuevoCliente;

    }

    public List<Cliente> listarClientes(String celular) {
        // Mantiene el filtro, pero sin paginado en este método (para compatibilidad con la vista).
        String cel = celular == null ? "" : celular.trim();
        return listarClientesPaginado(cel, 1, Integer.MAX_VALUE);
    }

    public java.util.List<Cliente> listarClientesPaginado(String celular, int page, int pageSize) {
        int p = PaginationSupport.normalizePage(page);
        int ps = PaginationSupport.normalizePageSize(pageSize);

        String cel = celular == null ? "" : celular.trim();
        return clienteRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(c -> cel.isEmpty() || (c.getNrocelular() != null && c.getNrocelular().equals(cel)))
                .sorted((a, b) -> {
                    if (a.getFechaRegistro() == null && b.getFechaRegistro() == null) return 0;
                    if (a.getFechaRegistro() == null) return 1;
                    if (b.getFechaRegistro() == null) return -1;
                    return b.getFechaRegistro().compareTo(a.getFechaRegistro());
                })
                .skip((long) (p - 1) * ps)
                .limit(ps)
                .collect(java.util.stream.Collectors.toList());
    }

    public int contarClientesFiltrados(String celular) {
        String cel = celular == null ? "" : celular.trim();
        return (int) clienteRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(c -> cel.isEmpty() || (c.getNrocelular() != null && c.getNrocelular().equals(cel)))
                .count();
    }



    public void actualizarCelular(String idCliente, String nuevoCelular) {

        String id = idCliente == null ? "" : idCliente.trim();
        String cel = nuevoCelular == null ? "" : nuevoCelular.trim();

        if (id.isEmpty()) {
            throw new ValidationException("ID de cliente inválido.");
        }
        if (cel.isEmpty()) {
            throw new ValidationException("Ingrese un número de celular.");
        }
        if (!cel.matches("\\d{9}")) {
            throw new ValidationException("El número de celular debe tener exactamente 9 dígitos.");
        }

        // Validar duplicado contra otros clientes (no permitir que otro tenga ese celular)
        clienteRepository.findByTelefono(cel).ifPresent(existente -> {
            if (!Objects.equals(existente.getIdCliente(), id)) {
                throw new DuplicateException("Este número ya pertenece a: " + existente.getNombreCliente());
            }
        });

        // Actualización directa a nivel BD para evitar tocar FechaRegistro.
        ClienteRepositoryPagination.actualizarTelefonoPorId(id, cel);

    }

    public void actualizarNombre(String idCliente, String nuevoNombre) {
        String id = idCliente == null ? "" : idCliente.trim();
        String nombre = nuevoNombre == null ? "" : nuevoNombre.trim();

        if (id.isEmpty()) {
            throw new ValidationException("ID de cliente inválido.");
        }
        if (nombre.isEmpty()) {
            throw new ValidationException("Debe ingresar el nombre del cliente.");
        }
        if (nombre.length() > 30) {
            throw new ValidationException("El nombre es muy largo (Máximo 30 caracteres).\r\n");
        }
        if (nombre.matches(".*\\d.*")) {
            throw new ValidationException("El nombre no debe contener números.");
        }

        Cliente existente = clienteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("El cliente no existe en la base de datos."));

        existente.setNombreCliente(nombre);
        clienteRepository.update(existente);
    }

    public void eliminarCliente(String idCliente) {
        String id = idCliente == null ? "" : idCliente.trim();
        if (id.isEmpty()) {
            throw new ValidationException("ID de cliente inválido.");
        }

        // Validación rápida de existencia
        clienteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("El cliente no existe en la base de datos."));

        clienteRepository.deleteById(id);
    }

}

