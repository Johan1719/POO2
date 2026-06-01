package com.laptitefrance.delivery.controllers;

import com.laptitefrance.delivery.exceptions.NotFoundException;
import com.laptitefrance.delivery.exceptions.ValidationException;
import com.laptitefrance.delivery.models.Empleado;
import com.laptitefrance.delivery.repositories.EmpleadoRepository;

import java.util.Optional;

public class LoginController {

    private final EmpleadoRepository empleadoRepository;

    public LoginController() {
        this(new EmpleadoRepository());
    }

    public LoginController(EmpleadoRepository empleadoRepository) {
        this.empleadoRepository = empleadoRepository;
    }

    /**
     * @throws ValidationException si el código es vacío o inválido
     * @throws NotFoundException si el empleado no existe
     */
    public Empleado login(String codigoEmpleado) {
        String codigo = codigoEmpleado == null ? "" : codigoEmpleado.trim();
        if (codigo.isEmpty()) {
            throw new ValidationException("Por favor, ingrese un código.");
        }

        Optional<Empleado> empleado = empleadoRepository.findById(codigo);
        if (empleado.isEmpty()) {
            throw new NotFoundException("Código de empleado no existe en la base de datos.");
        }

        return empleado.get();
    }
}

