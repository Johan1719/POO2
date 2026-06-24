package com.laptitefrance.delivery.controllers;

import com.laptitefrance.delivery.audit.RegistroAccesos;
import com.laptitefrance.delivery.exceptions.NotFoundException;
import com.laptitefrance.delivery.exceptions.ValidationException;
import com.laptitefrance.delivery.models.Empleado;
import com.laptitefrance.delivery.repositories.EmpleadoRepository;

import java.util.Optional;

/**
 * Controlador de inicio de sesión. Valida el código de empleado contra la base de datos
 * y, de paso, deja registrado cada intento en la auditoría de accesos.
 *
 * Recibe el repositorio por constructor (inyección de dependencias): el constructor sin
 * argumentos usa el repositorio real; el otro permite inyectar uno simulado para pruebas.
 */
public class LoginController {

    private final EmpleadoRepository empleadoRepository;

    public LoginController() {
        this(new EmpleadoRepository());
    }

    public LoginController(EmpleadoRepository empleadoRepository) {
        this.empleadoRepository = empleadoRepository;
    }

    /**
     * Intenta iniciar sesión con un código de empleado.
     *
     * Lógica (tres caminos, cada uno deja registro en la auditoría):
     *  1) Código vacío  → registra FALLO y lanza ValidationException.
     *  2) Código que no existe en BD → registra FALLO y lanza NotFoundException.
     *  3) Código válido → registra EXITO y devuelve el Empleado encontrado.
     *
     * @throws ValidationException si el código es vacío o inválido
     * @throws NotFoundException si el empleado no existe
     */
    public Empleado login(String codigoEmpleado) {
        // Normaliza la entrada: null → "" y se quitan espacios alrededor.
        String codigo = codigoEmpleado == null ? "" : codigoEmpleado.trim();
        if (codigo.isEmpty()) {
            RegistroAccesos.registrarFallo(codigo, "Codigo vacio");
            throw new ValidationException("Por favor, ingrese un código.");
        }

        // Busca el empleado por su código (clave primaria) en la base de datos.
        Optional<Empleado> empleado = empleadoRepository.findById(codigo);
        if (empleado.isEmpty()) {
            RegistroAccesos.registrarFallo(codigo, "Codigo no existe");
            throw new NotFoundException("Código de empleado no existe en la base de datos.");
        }

        // Acceso correcto: se audita y se devuelve el empleado para abrir su dashboard.
        RegistroAccesos.registrarExito(codigo, empleado.get().getNombre());
        return empleado.get();
    }
}

