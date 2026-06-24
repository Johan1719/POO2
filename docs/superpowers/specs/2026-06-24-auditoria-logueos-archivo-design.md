# Diseño: Auditoría de logueos a archivo de texto

Fecha: 2026-06-24
Estado: Aprobado por el usuario

## Contexto

Sistema de delivery "La P'tite France" (POO2). Se quiere una **auditoría de accesos al
sistema** (manejo de archivos): cada intento de login —exitoso o fallido— se registra en un
archivo de texto plano. Reemplaza la idea previa de auditoría de pedidos (que se consulta
mejor desde la base de datos).

El login pasa por un punto único: `LoginController.login(codigoEmpleado)`
([LoginController.java](../../../src/main/java/com/laptitefrance/delivery/controllers/LoginController.java)):
- Código vacío → lanza `ValidationException`.
- Código inexistente → lanza `NotFoundException`.
- Código válido → retorna el `Empleado`.

## Objetivo

Registrar en un `.txt` cada acceso: **logueos exitosos y fallidos**, con fecha/hora, código
ingresado y nombre (en éxito) o motivo (en fallo).

## Decisiones tomadas

- **Eventos:** logueos exitosos y fallidos (no se registra el cierre de sesión).
- **Salida:** solo el archivo `.txt` (sin visor dentro de la app).
- **Ubicación:** `logs/auditoria-logins.txt`, relativo al directorio de trabajo (la raíz del
  proyecto, igual que `application.properties`). La carpeta `logs/` se crea sola si no existe.
- **Modo:** append (no se sobrescribe), codificación UTF-8.
- **Robustez:** si la escritura del archivo falla, **no se interrumpe el login**; el error se
  imprime en consola.
- **Sin cambios en la base de datos.**

## Arquitectura

Clase nueva con responsabilidad única: **`RegistroAccesos`** (paquete
`com.laptitefrance.delivery.audit`). Solo sabe escribir una línea en el archivo de auditoría.

Métodos estáticos públicos:
- `void registrarExito(String codigo, String nombre)`
- `void registrarFallo(String codigo, String motivo)`

Ambos delegan en un método privado que:
1. Asegura que exista la carpeta `logs/`.
2. Abre `logs/auditoria-logins.txt` en modo append (UTF-8).
3. Escribe una línea con el formato definido.
4. Captura `IOException` y la reporta por consola sin propagarla.

## Flujo de datos

`LoginController.login(codigoEmpleado)` invoca a `RegistroAccesos` en sus tres salidas:

- Código vacío: `RegistroAccesos.registrarFallo(codigo, "Codigo vacio")` antes de
  lanzar `ValidationException`.
- Código inexistente: `RegistroAccesos.registrarFallo(codigo, "Codigo no existe")` antes de
  lanzar `NotFoundException`.
- Éxito: `RegistroAccesos.registrarExito(codigo, empleado.getNombre())` antes del `return`.

La vista (`LoginView`) no se modifica.

## Formato del archivo

Una línea por evento. Fecha/hora `yyyy-MM-dd HH:mm:ss`. Campos separados por ` | `:

```
2026-06-24 14:30:01 | EXITO | codigo=E001 | nombre=Jean Piere Micuilla
2026-06-24 14:30:05 | FALLO | codigo=Z999 | motivo=Codigo no existe
2026-06-24 14:31:00 | FALLO | codigo= | motivo=Codigo vacio
```

## Manejo de errores

- Falla de E/S al escribir el log → se captura `IOException`, se imprime en `System.err` y el
  login continúa normal (la auditoría nunca debe bloquear el acceso).
- Código nulo → se normaliza a cadena vacía antes de registrar.

## Pruebas / verificación (manual)

1. Loguear con `E001` (válido) → en `logs/auditoria-logins.txt` aparece una línea `EXITO` con
   código y nombre.
2. Loguear con `Z999` (inexistente) → línea `FALLO ... motivo=Codigo no existe`.
3. Dar "Ingresar" con el campo vacío → línea `FALLO ... motivo=Codigo vacio`.
4. Repetir logins → las líneas se **acumulan** (append), no se sobrescriben.

## Fuera de alcance

- Registro del cierre de sesión (logout).
- Visor del log dentro de la aplicación.
- Rotación/archivado del log por tamaño o fecha.
- Persistencia de la auditoría en base de datos.
