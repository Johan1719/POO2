# TODO - Migración BD LaPtiteFranceDB

## Paso 1 (HECHO)
- Actualizar `models/Pedido.java` para incluir:
  - `codAsistente`
  - `direccionEntrega`
  - getters/setters y Builder.

## Paso 2 (HECHO)
- Actualizar `repositories/PedidoRepository.java` (SQL INSERT/SELECT/UPDATE) para columnas nuevas:
  - `DireccionEntrega`
  - `CodAsistente`

## Paso 3 (EN PROGRESO)
- Actualizar `services/PedidoService.java` para llenar los campos requeridos por la BD si el flujo UI aún no los define.

## Paso 4
- Ejecutar compilación (`mvn test` / `mvn package`) para listar fallos restantes.

## Paso 5
- Eliminar o corregir código obsoleto (tablas `Atencion`/`Direccion`) según falle la compilación.

