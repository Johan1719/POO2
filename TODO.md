# TODO - API pedidos por repartidor

## Plan de implementación
1) Crear DTO de respuesta para el API de pedidos del repartidor (incluye: codPedido, estado, montoPedido, direccionEntrega, cliente id/nombre).
2) Crear Repository que haga JOIN Pedido+Cliente filtrando por `Pedido.CodRepartidor`.
3) Crear Controller API que:
   - GET: liste pedidos por `codRepartidor`
   - POST: marque pedido como `ENTREGADO` y guarde hora si aplica.
4) Actualizar `ApiRepartidor.java` para exponer endpoints:
   - `GET /api/repartidores/{codRepartidor}/pedidos`
   - `POST /api/repartidores/{codRepartidor}/pedidos/{codPedido}/entregar`
5) (Opcional) Proveer ejemplo de uso desde frontend (fetch).
6) Compilar y verificar que el endpoint responda JSON.

