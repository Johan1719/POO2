# TODO - Secuencias SQL para códigos (Clientes/Pedidos/Productos)

## Resumen
- Cliente: C### (3 dígitos)
- Pedido: P#### (4 dígitos)
- Producto: PR### (prefijo PR + 3 dígitos)

## Pasos
- [ ] Actualizar SQL (SQLQuery1.sql) para crear SEQUENCE:
  - [ ] seq_cliente
  - [ ] seq_pedido
  - [ ] seq_producto
- [ ] Ajustar los INSERT de:
  - [ ] ClienteRepository.insert (dejar que SQL genere el ID o pedir el NEXT VALUE)
  - [ ] PedidoController/PedidoRepository (generar CodPedido con SEQUENCE)
  - [ ] ProductoController/ProductoRepository (generar CodProducto con SEQUENCE)
- [ ] Quitar generación random desde:
  - [ ] ClienteController.registrarCliente
  - [ ] PedidoController.ensamblarNuevoPedido
  - [ ] (si aplica) cualquier random en Producto
- [ ] Compilar: `mvn -q test` y `mvn -q package`

