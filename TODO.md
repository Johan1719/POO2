# TODO - Panel Clientes/Inventario (paginado + corrección controllers)

## 1) Arreglar llamados a métodos inexistentes
- [x] Actualizar `ClienteController` con:
  - [x] `listarClientes(String celular)`
  - [x] `actualizarCelular(String idCliente, String nuevoCelular)`
- [x] Actualizar `ProductoController` con:
  - [x] `listarProductos(String filtro)`
  - [x] `actualizarStock(String codProducto, int nuevoStock)`

## 2) Implementar paginado (pageSize = 10)
- [x] Actualizar `ClienteController` con:
  - [x] `listarClientesPaginado(String celular, int page, int pageSize)`
  - [x] `contarClientesFiltrados(String celular)`
- [x] Actualizar `ProductoController` con:
  - [x] `listarProductosPaginado(String filtro, int page, int pageSize)`
  - [x] `contarProductosFiltrados(String filtro)`

## 3) Implementar paginado en la UI
- [x] Actualizar `PanelClientes` para mostrar paginador (Anterior/Siguiente, página actual/total) y usar métodos paginados.
- [x] Actualizar `PanelInventario` para mostrar paginador opcional (igual estilo) y usar métodos paginados.
- [x] Activar pestañas Clientes/Inventario en `DashboardAsistenteView` (de placeholders a vistas reales)

## 4) Prueba / compilación
- [ ] Correr `mvn test` o `mvn -q package` para verificar que compila sin errores.

