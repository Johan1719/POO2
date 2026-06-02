package com.laptitefrance.delivery.controllers;

import com.laptitefrance.delivery.exceptions.ValidationException;
import com.laptitefrance.delivery.models.Cliente;
import com.laptitefrance.delivery.services.ClienteService;

public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController() {
        // ESTRICTO: Solo instanciamos el servicio
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

        // 1. Validaciones de presencia
        if (nom.isEmpty()) {
            throw new ValidationException("Debe ingresar un nombre completo.");
        }
        if (cel.isEmpty()) {
            throw new ValidationException("Debe ingresar un número de celular.");
        }

        // 2. Validaciones de formato y longitud (Evita el Error de Truncamiento en SQL)
        if (nom.length() > 30) {
            throw new ValidationException("El nombre es muy largo (Máximo 30 caracteres).");
        }
        
        // Expresión regular para asegurar que el celular tenga exactamente 9 dígitos numéricos
        if (!cel.matches("\\d{9}")) { 
            throw new ValidationException("El número de celular debe tener exactamente 9 dígitos.");
        }

        // Si pasa todas las aduanas, recién molestamos al Servicio
        return clienteService.registrarCliente(nom, cel);
    }
}