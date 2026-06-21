# TODO

- [x] Corregir FK nula en `Pedido` al crear cliente nuevo desde `PanelNuevaVenta`
  - Cambio en `ClienteController.registrarCliente()` para recuperar el `idCliente` autogenerado tras insert.
- [x] Calcular `tiempoEntEstimado` cuando se asigna repartidor en `PedidoController.asignarRepartidor()`

  - Usar `Tarifa.tiempoPromedio` (vía `pedido.getCodTarifa()` -> `TarifaRepository.findById()` o query equivalente)
  - Setear `pedido.setTiempoEntEstimado(...)` y persistir con `pedidoRepository.update(pedido)`


