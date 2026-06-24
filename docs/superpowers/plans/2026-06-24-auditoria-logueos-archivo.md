# Auditoría de logueos a archivo — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Registrar en `logs/auditoria-logins.txt` cada intento de login (exitoso o fallido) sin bloquear el acceso.

**Architecture:** Una clase nueva `RegistroAccesos` (paquete `audit`) escribe líneas al archivo en modo append; `LoginController.login()` la invoca en sus tres salidas (código vacío, código inexistente, éxito).

**Tech Stack:** Java 21, `java.nio.file` para E/S de archivos.

## Global Constraints

- **No se introduce framework de tests.** Verificación **manual**: compilar en el IDE, correr `MainAll`, loguear y abrir el `.txt`. `mvn` no está en el PATH.
- La auditoría **nunca interrumpe el login**: si la escritura falla, se captura `IOException` y se imprime en `System.err`.
- Archivo: `logs/auditoria-logins.txt` (relativo a la raíz del proyecto), append, UTF-8; la carpeta `logs/` se crea sola.
- Formato por línea: `yyyy-MM-dd HH:mm:ss | EXITO|FALLO | codigo=<cod> | nombre=<n>` (éxito) o `... | motivo=<m>` (fallo).
- Sin cambios en la base de datos. Idioma de mensajes: español.

---

### Task 1: Clase `RegistroAccesos`

**Files:**
- Create: `src/main/java/com/laptitefrance/delivery/audit/RegistroAccesos.java`

**Interfaces:**
- Produces:
  - `static void RegistroAccesos.registrarExito(String codigo, String nombre)`
  - `static void RegistroAccesos.registrarFallo(String codigo, String motivo)`

- [ ] **Step 1: Crear la clase**

```java
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
```

- [ ] **Step 2: Compilar en el IDE**

Esperado: compila sin errores.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/audit/RegistroAccesos.java
git commit -m "feat: clase RegistroAccesos para auditar logins a archivo"
```

---

### Task 2: Enganchar la auditoría en `LoginController`

**Files:**
- Modify: `src/main/java/com/laptitefrance/delivery/controllers/LoginController.java`

**Interfaces:**
- Consumes: `RegistroAccesos.registrarExito(...)` / `registrarFallo(...)` (Task 1).

- [ ] **Step 1: Importar `RegistroAccesos`**

En `LoginController.java`, junto a los imports existentes, agregar:

```java
import com.laptitefrance.delivery.audit.RegistroAccesos;
```

- [ ] **Step 2: Registrar en las tres salidas de `login`**

Reemplazar el método `login`:

```java
    public Empleado login(String codigoEmpleado) {
        String codigo = codigoEmpleado == null ? "" : codigoEmpleado.trim();
        if (codigo.isEmpty()) {
            RegistroAccesos.registrarFallo(codigo, "Codigo vacio");
            throw new ValidationException("Por favor, ingrese un código.");
        }

        Optional<Empleado> empleado = empleadoRepository.findById(codigo);
        if (empleado.isEmpty()) {
            RegistroAccesos.registrarFallo(codigo, "Codigo no existe");
            throw new NotFoundException("Código de empleado no existe en la base de datos.");
        }

        RegistroAccesos.registrarExito(codigo, empleado.get().getNombre());
        return empleado.get();
    }
```

- [ ] **Step 3: Compilar en el IDE**

Esperado: compila sin errores.

- [ ] **Step 4: Verificación manual — login exitoso**

Correr `MainAll`, loguear con `E001`. Abrir `logs/auditoria-logins.txt` (en la raíz del proyecto). Esperado: una línea tipo
`2026-06-24 14:30:01 | EXITO | codigo=E001 | nombre=Jean Piere Micuilla`.

- [ ] **Step 5: Verificación manual — login fallido**

Loguear con `Z999` (inexistente) → línea `FALLO | codigo=Z999 | motivo=Codigo no existe`.
Dar "Ingresar" con el campo vacío → línea `FALLO | codigo= | motivo=Codigo vacio`.
Repetir varios logins → las líneas se **acumulan** (append), no se sobrescriben.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/laptitefrance/delivery/controllers/LoginController.java
git commit -m "feat: registrar logins exitosos y fallidos en la auditoria"
```

---

## Self-Review

- **Cobertura del spec:**
  - Clase `RegistroAccesos` con `registrarExito`/`registrarFallo`: Task 1. ✅
  - Archivo `logs/auditoria-logins.txt`, append, UTF-8, carpeta autocreada: Task 1 (`Files.write` con CREATE+APPEND, `createDirectories`). ✅
  - Enganche en las 3 salidas de `LoginController.login`: Task 2. ✅
  - No bloquear el login ante fallo de E/S: Task 1 (catch `IOException` → `System.err`). ✅
  - Eventos exitosos + fallidos: Tasks 1 y 2. ✅
  - Sin cambios de BD: ninguna tarea toca la base. ✅
- **Sin placeholders:** todo el código está completo. ✅
- **Consistencia de tipos:** `registrarExito(String,String)` y `registrarFallo(String,String)` definidos en Task 1 y llamados con esas firmas en Task 2. ✅

## Verificación final

1. Login `E001` → línea EXITO con código y nombre.
2. Login `Z999` → línea FALLO motivo=Codigo no existe.
3. Campo vacío → línea FALLO motivo=Codigo vacio.
4. Varios logins → se acumulan (append).
