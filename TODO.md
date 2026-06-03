# TODO - Migración BD LaPtiteFranceDB

> Última actualización: 2026-06-02

## Paso 1 (HECHO)
- Actualizar `models/Pedido.java` para incluir:
  - `codAsistente`
  - `direccionEntrega`
  - getters/setters y Builder.

## Paso 2 (HECHO)
- Actualizar `repositories/PedidoRepository.java` (SQL INSERT/SELECT/UPDATE) para columnas nuevas:
  - `DireccionEntrega`
  - `CodAsistente`

## Paso 3 (HECHO — cambió de enfoque)
- ~~Actualizar `services/PedidoService.java`~~ → **La capa `services/` fue eliminada** (commit `74d4dd4`).
  - Las vistas ahora llaman **directamente a los controllers** (p. ej. `PanelNuevaVenta` → `PedidoController.generarPedido(...)`).
  - El llenado de campos requeridos por la BD se hace en `PedidoController.ensamblarNuevoPedido(...)`.
  - `DashboardAsistenteView` inyecta un único `PedidoController` (con el código del cajero) a las vistas.

## Paso 4 (HECHO)
- Documentación del proyecto:
  - `README.md`: estructura por capas, patrones de diseño (MVC, Repository/DAO, Builder, Observer, DI, Singleton, excepciones de dominio), flujo de operaciones e instrucciones de ejecución.
  - JavaDoc en controllers (`PedidoController`, `LoginController`, `ClienteController`, `ProductoController`, `PagoController`, `TarifaController`, `ApiRepartidor`) e interfaz base `IRepositorioBase` con `@param` / `@return` / `@throws`.

## Pendiente

### P5 — Compilación y verificación
- Ejecutar `mvn clean package` para confirmar que no quedan fallos de compilación.

### P6 — Limpiar código/artefactos obsoletos
- Revisar referencias a tablas `Atencion` / `Direccion` si la compilación las reporta.
- Evaluar si `target/` debería estar versionado o ignorarse en `.gitignore` (hoy hay `.class` en git).

### P7 — Completar funcionalidad parcial
- `ApiRepartidor`: reemplazar los datos **simulados** del endpoint `/api/pedidos-pendientes`
  por una consulta real al `PedidoRepository`.
- Pestañas placeholder en `DashboardAsistenteView`: "👥 Clientes" y "📦 Inventario" son `JPanel` vacíos.

### P8 — Documentación restante (opcional)
- ~~Extender JavaDoc a la capa `repositories`~~ **HECHO**: JavaDoc de clase en los 11 DAO
  (rol, tabla, clave, notas de JOIN/clave compuesta) + helpers de fecha y métodos especiales
  (`ClienteRepository.findByTelefono`, `PedidoProductoRepository.splitId`).
- Pendiente: documentar los `models` (Builder de `Pedido`, getters/setters).
