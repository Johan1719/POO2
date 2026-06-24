package com.laptitefrance.delivery.audit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Auditoría de accesos (manejo de archivos): registra en un archivo de texto cada
 * intento de inicio de sesión, exitoso o fallido. Es una clase de utilidad: no se
 * instancia (constructor privado), solo se usan sus métodos estáticos.
 */
public final class RegistroAccesos {

    // Ruta del archivo de auditoría (relativa a la raíz del proyecto) y formato de fecha/hora.
    private static final Path ARCHIVO = Paths.get("logs", "auditoria-logins.txt");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Constructor privado: impide crear objetos de esta clase (solo métodos estáticos).
    private RegistroAccesos() {}

    /** Registra un acceso correcto, guardando el código y el nombre del empleado. */
    public static void registrarExito(String codigo, String nombre) {
        escribir("EXITO", codigo, "nombre=" + (nombre == null ? "" : nombre));
    }

    /** Registra un intento fallido, guardando el código ingresado y el motivo del rechazo. */
    public static void registrarFallo(String codigo, String motivo) {
        escribir("FALLO", codigo, "motivo=" + (motivo == null ? "" : motivo));
    }

    /**
     * Núcleo de la escritura. Lógica:
     *  1) Arma la línea: "fecha | EXITO/FALLO | codigo=... | detalle".
     *  2) Crea la carpeta logs/ si no existe.
     *  3) Escribe en modo APPEND (agrega al final, sin borrar lo anterior).
     *  4) Si la escritura falla, NO propaga el error: la auditoría jamás debe impedir el login.
     */
    private static void escribir(String resultado, String codigo, String detalle) {
        String cod = codigo == null ? "" : codigo;
        String linea = LocalDateTime.now().format(FMT)
                + " | " + resultado
                + " | codigo=" + cod
                + " | " + detalle
                + System.lineSeparator();
        try {
            if (ARCHIVO.getParent() != null) {
                Files.createDirectories(ARCHIVO.getParent());
            }
            Files.write(ARCHIVO, linea.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("No se pudo escribir la auditoría de login: " + e.getMessage());
        }
    }
}
