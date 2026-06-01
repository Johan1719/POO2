package com.laptitefrance.delivery.audit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuditoriaLog {
    // Define la ruta del archivo de auditoría (se creará en la raíz del proyecto)
    private static final String RUTA_ARCHIVO = "auditoria_pedidos.txt";

    // Agregamos 'synchronized' para hacerlo thread-safe ante los CompletableFuture
    public static synchronized void registrarAccion(String codEmpleado, String accion) {
        try {
            String fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            // Formato CSV simple: FechaHora, CodigoEmpleado, Accion
            String log = String.format("%s,%s,%s\n", fechaHora, codEmpleado, accion);
            
            // Escribe en el archivo añadiendo al final (APPEND) o lo crea si no existe
            Files.writeString(Paths.get(RUTA_ARCHIVO), log, 
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    
        } catch (IOException e) {
            System.err.println("Error al escribir en el log de auditoría: " + e.getMessage());
        }
    }
}