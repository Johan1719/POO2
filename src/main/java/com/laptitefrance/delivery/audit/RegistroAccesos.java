package com.laptitefrance.delivery.audit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Auditoría de accesos: escribe una línea por intento de login en un archivo de texto. */
public final class RegistroAccesos {

    private static final Path ARCHIVO = Paths.get("logs", "auditoria-logins.txt");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private RegistroAccesos() {}

    public static void registrarExito(String codigo, String nombre) {
        escribir("EXITO", codigo, "nombre=" + (nombre == null ? "" : nombre));
    }

    public static void registrarFallo(String codigo, String motivo) {
        escribir("FALLO", codigo, "motivo=" + (motivo == null ? "" : motivo));
    }

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
