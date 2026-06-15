# TODO: Cascade delete Cliente -> Pedidos (SQL Server)

## Objetivo
Al eliminar un `Cliente` se deben eliminar automáticamente:
- sus `Pedido`
- y los `Pedido_Producto` asociados a esos pedidos (por cascada)

## Cambios en `SQLQuery1.sql`
1. En `CREATE TABLE Pedido` cambiar la FK:
   - `IDCliente ... FOREIGN KEY REFERENCES Cliente(IDCliente)`
   - por `... REFERENCES Cliente(IDCliente) ON DELETE CASCADE`

2. En `CREATE TABLE Pedido_Producto` cambiar la FK:
   - `CodPedido ... FOREIGN KEY REFERENCES Pedido(CodPedido)`
   - por `... REFERENCES Pedido(CodPedido) ON DELETE CASCADE`

3. (Opcional) Si quieres también cascada de Producto:
   - `CodProducto ... FOREIGN KEY REFERENCES Producto(CodProducto)`
   - por `... REFERENCES Producto(CodProducto) ON DELETE CASCADE`

## Cómo aplicar
- Editar `SQLQuery1.sql`.
- Ejecutar el script completo (tu script ya hace `DROP TABLE IF EXISTS` para recrear todo).

## Validación
- Insertar/crear un cliente con pedidos.
- Ejecutar `DELETE FROM Cliente WHERE IDCliente='Cxxx'`.
- Verificar que ya no existen `Pedido` para ese `IDCliente`.
- Verificar que tampoco existen `Pedido_Producto` para esos `CodPedido`.

