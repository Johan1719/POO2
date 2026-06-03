package com.laptitefrance.delivery.controllers;

import java.util.Objects;

import com.laptitefrance.delivery.exceptions.DuplicateException;
import com.laptitefrance.delivery.exceptions.NotFoundException;
import com.laptitefrance.delivery.exceptions.ValidationException;
import com.laptitefrance.delivery.models.Cliente;
import com.laptitefrance.delivery.repositories.ClienteRepository;

/**
 * CONTROLLER de clientes.
 *
 * Encapsula las reglas para buscar y registrar clientes. Importante: aquí viven
 * las VALIDACIONES (formato de celular, longitud del nombre, duplicados), no en la vista.
 */
public class ClienteController {

    private final ClienteRepository clienteRepository;

    public ClienteController() {
        this(new ClienteRepository());
    }

    /** Inyección de dependencias: permite pasar un repositorio alternativo (p. ej. en tests). */
    public ClienteController(ClienteRepository clienteRepository) {
        this.clienteRepository = Objects.requireNonNull(clienteRepository);
    }

    /**
     * Busca un cliente por su número de celular (lo usa la pantalla de venta para
     * identificar rápido al cliente). Si no existe, lanza NotFoundException.
     *
     * @param celular número de celular (String) a buscar; se admite null y se recortan espacios.
     * @return el objeto {@link Cliente} encontrado (nunca null si retorna).
     * @throws ValidationException si el celular está vacío.
     * @throws NotFoundException   si ningún cliente tiene ese celular.
     */
    public Cliente buscarClientePorCelular(String celular) {
        String cel = celular == null ? "" : celular.trim();

        if (cel.isEmpty()) {
            throw new ValidationException("Ingrese un número de celular.");
        }

        return clienteRepository.findByTelefono(cel)
                .orElseThrow(() -> new NotFoundException("El cliente no existe en la base de datos."));
    }

    /**
     * Registra un cliente nuevo aplicando todas las reglas de negocio:
     *   - nombre y celular obligatorios,
     *   - nombre ≤ 30 caracteres y sin números,
     *   - celular de exactamente 9 dígitos,
     *   - el celular no puede estar ya registrado (DuplicateException).
     * Si todo es válido, genera un ID y lo persiste.
     *
     * @param nombre  nombre completo (String) del cliente; máx. 30 caracteres y sin números.
     * @param celular celular (String) de exactamente 9 dígitos; no puede existir previamente.
     * @return el objeto {@link Cliente} recién creado, ya con su idCliente asignado.
     * @throws ValidationException si nombre o celular incumplen el formato esperado.
     * @throws DuplicateException  si el celular ya pertenece a otro cliente.
     */
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

