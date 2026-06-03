package com.laptitefrance.delivery.controllers;

import com.laptitefrance.delivery.exceptions.NotFoundException;
import com.laptitefrance.delivery.exceptions.ValidationException;
import com.laptitefrance.delivery.models.Empleado;
import com.laptitefrance.delivery.repositories.EmpleadoRepository;

import java.util.Optional;

/**
 * CONTROLLER de autenticación.
 *
 * Responsabilidad: validar el acceso de un empleado a partir de su código.
 * No dibuja nada (eso es de LoginView) ni habla SQL (eso es de EmpleadoRepository):
 * solo orquesta la regla de negocio "para entrar, tu código debe existir en la BD".
 */
public class LoginController {

    // Dependencia hacia la capa de datos. El controlador NO sabe cómo se guarda
    // el empleado (SQL, memoria, archivo...), solo que puede pedírselo al repositorio.
    private final EmpleadoRepository empleadoRepository;

    /** Constructor de uso real: crea su propio repositorio contra SQL Server. */
    public LoginController() {
        this(new EmpleadoRepository());
    }

    /**
     * Constructor para Inyección de Dependencias: permite pasar un repositorio
     * distinto (por ejemplo, un mock en pruebas unitarias sin BD real).
     *
     * @param empleadoRepository repositorio de empleados a utilizar para autenticar.
     */
    public LoginController(EmpleadoRepository empleadoRepository) {
        this.empleadoRepository = empleadoRepository;
    }

    /**
     * Intenta iniciar sesión con el código indicado.
     *
     * Flujo:
     *   1. Normaliza la entrada (quita espacios, evita null).
     *   2. Si está vacía → ValidationException (la vista la muestra como aviso).
     *   3. Busca el empleado en la BD; si no existe → NotFoundException (error de acceso).
     *   4. Si existe, devuelve el Empleado para que la vista abra el dashboard.
     *
     * @param codigoEmpleado código (String) tecleado por el usuario; se admite null y
     *                       se le quitan espacios antes de validar.
     * @return el objeto {@link Empleado} correspondiente al código (nunca null si retorna).
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

