# Guion del video — Sistema de Delivery "La P'tite France" (POO2)

> Guion para grabar la explicación del proyecto. Cada sección trae **🎬 Mostrar** (qué tener
> en pantalla) y **🎤 Decir** (lo que narrás). Duración estimada total: 12–18 min.
> Antes de grabar: tené SQL Server corriendo, la base `LaPtiteFranceDB` cargada con
> `SQLQuery1.sql`, y el proyecto compilado.

---

## 0. Introducción (≈1 min)

**🎤 Decir:**
"Hola, voy a presentar **La P'tite France**, un sistema de delivery para una panadería,
hecho en **Java** con Programación Orientada a Objetos. El sistema tiene **dos caras**: una
**aplicación de escritorio (Swing)** que usa el personal de la tienda —cajeros y monitor de
pedidos— y una **API REST** que usan los repartidores desde el navegador del celular. Ambas
comparten la misma **base de datos SQL Server**."

**🎤 Decir (arquitectura):**
"El proyecto está organizado en **capas**:
- **models**: las entidades (Pedido, Producto, Cliente, Empleado, Tarifa, Pago…).
- **repositories**: el acceso a la base de datos (consultas SQL).
- **controllers**: la lógica de negocio.
- **views**: las pantallas Swing.
- **dtos**: objetos de transporte de datos para mostrar información.
- **audit**: la auditoría de accesos a archivo.

La regla es que las vistas no hablan directo con la base: piden todo a los controladores, y
los controladores usan los repositorios."

---

## 1. Punto de entrada: `MainAll` (≈1 min)

**🎬 Mostrar:** el archivo `MainAll.java`.

**🎤 Decir:**
"Todo arranca en `MainAll`. Hace dos cosas: primero **levanta la API REST** con Javalin, que
queda escuchando en el puerto 8080; y después **abre la ventana de login** de Swing. Un
detalle importante: la ventana se crea con `SwingUtilities.invokeLater`, porque toda la
interfaz gráfica de Swing debe construirse en su hilo especial, el *Event Dispatch Thread*."

---

## 2. Login + Auditoría a archivo (≈2 min)

**🎬 Mostrar:** correr `MainAll` → aparece la ventana de login.

**🎤 Decir:**
"El sistema pide un **código de empleado**. Voy a entrar con `E001`."

**🎬 Mostrar:** escribir `E001`, ingresar → entra al dashboard.

**🎬 Mostrar:** el archivo `LoginController.java`.

**🎤 Decir:**
"La lógica del login está en `LoginController`. Tiene tres caminos: si el código está vacío,
si el código no existe, o si es válido. Y acá conecté un requisito de **manejo de archivos**:
cada intento de login —exitoso o fallido— se **registra en un archivo de texto de auditoría**."

**🎬 Mostrar:** el archivo `audit/RegistroAccesos.java`, y luego `logs/auditoria-logins.txt`.

**🎤 Decir:**
"`RegistroAccesos` es una **clase de utilidad** —constructor privado, solo métodos
estáticos— que escribe una línea por evento en `logs/auditoria-logins.txt`, en modo
**append** para no borrar lo anterior. Si la escritura falla, no rompe el login: la auditoría
nunca debe impedir el acceso. Acá se ve el archivo: tengo el acceso exitoso de `E001`."

**🎬 Mostrar (opcional):** volver al login (botón Cerrar Sesión), probar `Z999` → error, y
campo vacío → error; luego abrir el txt y mostrar las líneas `FALLO`.

**🎤 Decir:** "Si pruebo un código inexistente o vacío, también queda registrado como FALLO,
con el motivo. Así queda la trazabilidad de quién intentó entrar y cuándo."

---

## 3. Dashboard y cierre de sesión (≈1 min)

**🎬 Mostrar:** la ventana del dashboard con sus pestañas.

**🎤 Decir:**
"Una vez dentro, el `DashboardAsistenteView` arma una ventana con **pestañas**: Nueva Venta,
Monitor de Pedidos, Clientes e Inventario. Arriba se ve el nombre del asistente y un botón
**Cerrar Sesión**, que con una confirmación vuelve al login para que entre otro trabajador.
Un detalle: al cambiar de pestaña, el sistema **refresca** los datos, así el inventario y el
menú siempre muestran el stock actualizado."

---

## 4. Nueva Venta — el corazón del sistema (≈4 min)

**🎬 Mostrar:** la pestaña "Nueva Venta".

### 4.1 Buscar/registrar cliente
**🎤 Decir:**
"Primero busco al cliente por su **celular**. Ojo: esto usa el campo `Nrocelular` de la tabla
`Cliente`. Si no existe, puedo registrarlo con el botón **+ Nuevo**."

**🎬 Mostrar:** buscar `987654321` → aparece "Valeria Mendoza".

### 4.2 Agregar al carrito + control de stock
**🎬 Mostrar:** seleccionar un producto, "Agregar al Carrito", poner una cantidad **mayor al
stock** (ej. 999).

**🎤 Decir:**
"Cuando agrego un producto, valido la cantidad contra el **stock disponible**: el stock del
menú menos lo que ya tengo en el carrito. Si pido de más, me avisa al instante y no lo agrega."

**🎬 Mostrar:** agregar una cantidad válida; mostrar el botón **🗑 Vaciar Carrito** (vaciar y
volver a cargar) para demostrar que se puede empezar de cero.

### 4.3 Generar pedido — Recojo vs Delivery
**🎤 Decir:**
"Al generar el pedido, primero elijo la **modalidad**."

**🎬 Mostrar:** "Generar Pedido" → elegir **Recojo en tienda**.
**🎤 Decir:** "Si es **recojo**, no me pide dirección: el cliente lo retira. Se guarda como
'RECOJO EN TIENDA'."

**🎬 Mostrar:** otra venta → **Delivery a domicilio**; cambiar la zona en el combo.
**🎤 Decir:** "Si es **delivery**, me pide la dirección y me muestra en vivo el **total con
envío**: Productos + Envío = TOTAL. Al cambiar la zona, el total se recalcula. Ese monto total
es el que se guarda en el pedido."

### 4.4 Qué pasa por debajo (mostrar código)
**🎬 Mostrar:** `PedidoController.generarPedido(...)` y `VentaRepository.registrarVenta(...)`.

**🎤 Decir:**
"Por debajo, el `PedidoController` valida los datos, **consolida** los ítems repetidos y arma
el `Pedido` con el **patrón Builder** —esto es POO: construyo el objeto paso a paso de forma
legible—. Después delega en `VentaRepository`, que es la pieza clave: en **una sola
transacción** inserta el pedido, inserta sus productos en la tabla puente `Pedido_Producto`,
y **descuenta el stock**. Si algo falla, hace **rollback** y no queda nada a medias. Y si un
producto llega a **cero**, devuelve esa lista para mostrar el aviso de **reabastecer**."

**🎬 Mostrar:** generar una venta que deje un producto en 0 → aparece el aviso "⚠️ Producto X
ya no tiene stock, se sugiere reabastecer".

---

## 5. Monitor de Pedidos (≈3 min)

**🎬 Mostrar:** la pestaña "Monitor de Pedidos".

**🎤 Decir:**
"Acá el encargado ve todos los pedidos, paginados, y puede **filtrar por estado**: EN ESPERA,
EN CAMINO, ENTREGADO o CANCELADO."

### 5.1 Asignar repartidor (con regla de negocio)
**🎬 Mostrar:** seleccionar un pedido de **delivery** → "Asignar Repartidor" → elegir uno.
**🎤 Decir:** "Asigno un repartidor y el pedido pasa a **EN CAMINO**. Esto se procesa en
segundo plano para no congelar la ventana, y calcula la hora estimada de entrega según la
zona."

**🎬 Mostrar:** seleccionar un pedido de **recojo** → "Asignar Repartidor".
**🎤 Decir:** "Pero si el pedido es de **recojo en tienda**, no tiene sentido un repartidor:
el sistema lo **bloquea** con un aviso. Esa es una regla de negocio que agregué."

### 5.2 Cancelar y reactivar — stock coherente
**🎬 Mostrar:** "Editar Estado" → poner un pedido en **CANCELADO**; luego ir a Inventario y
mostrar que el stock **subió**.

**🎤 Decir:**
"Lo más interesante: como el stock se descuenta al generar el pedido, si después se
**cancela**, el sistema **devuelve** ese stock. Y si se **reactiva** un pedido cancelado,
lo vuelve a descontar. Todo esto también es transaccional, en `PedidoController` con
`VentaRepository`."

---

## 6. Inventario (≈1 min)

**🎬 Mostrar:** la pestaña "Inventario".

**🎤 Decir:**
"El inventario lista los productos con su stock y permite **crear**, **editar precio/stock**,
**cambiar estado** (activo/inactivo) y **eliminar**. Está **paginado** con un componente
reutilizable, el `PaginatorPanel`. Y como vimos, refleja en tiempo real los cambios de stock
que producen las ventas y las cancelaciones."

---

## 7. La API del Repartidor (≈2 min)

**🎬 Mostrar:** abrir el navegador en `http://localhost:8080/repartidor`.

**🎤 Decir:**
"Esta es la otra cara del sistema: una **API REST** que el repartidor abre desde el navegador.
Ingreso mi código, por ejemplo `E004`."

**🎬 Mostrar:** ingresar `E004` → "Cargar pedidos" → aparece "Bienvenido, Johan Vasquez 👋" y
la lista de pedidos.

**🎤 Decir:**
"La API saluda con el **nombre del repartidor** —que obtiene cruzando las tablas Repartidor y
Empleado— y muestra solo sus pedidos **EN CAMINO**, con la dirección de entrega. Puedo
**confirmar la entrega** de un pedido, y eso lo marca como ENTREGADO en la base."

**🎬 Mostrar:** confirmar una entrega; opcional: probar un código inexistente y mostrar el
error 404.

**🎬 Mostrar:** el archivo `ApiRepartidor.java`.
**🎤 Decir:** "El código vive en `ApiRepartidor`, con Javalin: define las rutas GET y POST.
Si el código de repartidor no existe, responde **404**."

---

## 8. Conceptos de POO para destacar (≈1 min)

**🎤 Decir (cierre técnico):**
"Para cerrar, los conceptos de **Programación Orientada a Objetos** que usa el proyecto:
- **Encapsulamiento**: cada modelo protege sus datos con getters y setters.
- **Herencia**: las excepciones `ValidationException` y `NotFoundException` heredan de
  `DomainException`.
- **Interfaces y genéricos**: `IRepositorioBase<T, ID>` define el contrato CRUD que cumplen
  los repositorios; eso es **polimorfismo**.
- **Patrón Builder**: para construir los pedidos.
- **Inyección de dependencias**: los controladores reciben sus repositorios por constructor.
- **Separación en capas** (vista → controlador → repositorio → base de datos).
- **Transacciones** para mantener la integridad (pedido + ítems + stock).
- **Manejo de archivos** en la auditoría de logins.
- **Concurrencia** con `CompletableFuture` al asignar repartidor.
- Y una **API REST** además de la app de escritorio."

**🎤 Decir (final):**
"Eso es **La P'tite France**: un sistema de delivery completo, con su app de tienda, su API
para repartidores, control de stock transaccional y auditoría. ¡Gracias!"

---

# PARTE B — Recorrido técnico detallado (método por método)

> Esta parte es para cuando muestres el **código en pantalla** y quieras explicarlo a fondo.
> Por cada método: **qué recibe**, **qué hace (lógica)**, **qué devuelve** y un **punto a resaltar**.

## B.1 `MainAll.java`

### `public static void main(String[] args)`
- **Qué recibe:** los argumentos de la línea de comandos (no se usan, se pasan a la API).
- **Qué hace:**
  1. `ApiRepartidor.main(args)` → arranca el servidor web Javalin en el puerto 8080. Esta
     llamada **no bloquea**: deja el servidor escuchando en hilos propios y sigue.
  2. `SwingUtilities.invokeLater(() -> new LoginView().setVisible(true))` → crea y muestra la
     ventana de login.
- **Punto a resaltar:** `invokeLater` encola la creación de la ventana en el **Event Dispatch
  Thread**, el único hilo donde Swing puede tocar la interfaz. El `try/catch (Throwable)`
  imprime el error en vez de fallar en silencio.

## B.2 `LoginController.java`

### `public Empleado login(String codigoEmpleado)`
- **Qué recibe:** el código que el usuario tipeó en la ventana.
- **Qué hace (3 caminos):**
  1. `String codigo = codigoEmpleado == null ? "" : codigoEmpleado.trim();` → normaliza:
     si es `null` lo vuelve `""`, y `trim()` le saca los espacios de los costados.
  2. Si `codigo.isEmpty()` → audita FALLO ("Codigo vacio") y lanza `ValidationException`.
  3. `empleadoRepository.findById(codigo)` → busca el empleado en la BD; devuelve un
     `Optional<Empleado>`. Si está vacío → audita FALLO ("Codigo no existe") y lanza
     `NotFoundException`.
  4. Si existe → audita EXITO con el nombre y **devuelve** el `Empleado`.
- **Qué devuelve:** el `Empleado` encontrado (con él, el dashboard sabe quién entró).
- **Punto a resaltar:** uso de `Optional` para evitar `null`, e **inyección de dependencias**:
  el repositorio entra por el constructor, así se puede testear con uno simulado.

## B.3 `audit/RegistroAccesos.java`

### `registrarExito(String codigo, String nombre)` y `registrarFallo(String codigo, String motivo)`
- **Qué hacen:** son atajos públicos que llaman al método privado `escribir(...)` con el
  resultado ("EXITO"/"FALLO") y el detalle ("nombre=..." o "motivo=...").

### `private static void escribir(String resultado, String codigo, String detalle)`
- **Qué hace (lógica):**
  1. Arma la línea: `fecha | resultado | codigo=... | detalle`, con la fecha formateada
     `yyyy-MM-dd HH:mm:ss`.
  2. `Files.createDirectories(...)` crea la carpeta `logs/` si no existe.
  3. `Files.write(..., CREATE, APPEND)` escribe la línea al final del archivo (sin borrar lo
     anterior), en UTF-8.
  4. El `catch (IOException)` reporta por consola y **no** relanza el error.
- **Punto a resaltar:** clase **de utilidad** (constructor privado, todo `static`), modo
  **append** y la idea de que la auditoría **nunca** debe frenar el login.

## B.4 `PedidoController.java`

### `public List<String> generarPedido(cliente, items, total, direccion, codTarifa, codPago, esRecojo)`
- **Qué recibe:** el cliente, la lista de ítems del carrito (`ItemVenta`), el monto total, la
  dirección, los códigos de tarifa y pago, y un booleano de si es recojo.
- **Qué hace:**
  1. `validarDatosGeneracion(...)` → chequea todo (la dirección solo se exige si NO es recojo).
  2. **Consolida** los ítems en un `LinkedHashMap` por código de producto: si el mismo producto
     vino dos veces, suma las cantidades (evita romper la PK de `Pedido_Producto`).
  3. `ensamblarNuevoPedido(...)` → crea el `Pedido` con el **Builder**.
  4. `new VentaRepository().registrarVenta(pedido, items)` → hace la venta atómica.
- **Qué devuelve:** `List<String>` con los nombres de productos que quedaron en stock 0.
- **Punto a resaltar:** el controlador **orquesta** pero no escribe SQL; eso es del repositorio.

### `private static void validarDatosGeneracion(...)`
- **Qué hace:** lanza `ValidationException` si falta el cliente, si no hay ítems, si el total
  es ≤ 0, si falta tarifa o pago, o si es delivery y la dirección está vacía.
- **Punto a resaltar:** la regla `if (!esRecojo && direccion vacía)` es la que hace la
  dirección **opcional solo en recojo**.

### `private static Pedido ensamblarNuevoPedido(...)`
- **Qué hace:** usa `new PedidoBuilder().idCliente(...).montoPedido(...)....build()` para
  construir el pedido en estado "EN ESPERA", con repartidor y tiempos aún en `null`.
- **Punto a resaltar:** **patrón Builder** — construcción legible, campo por campo.

### `public void asignarRepartidor(String codPedido, String codRepartidor)`
- **Qué hace:** dentro de un `CompletableFuture.runAsync(...)` (en segundo plano): busca el
  pedido, lo pone "EN CAMINO", le asigna el repartidor, calcula la hora estimada de entrega
  (hora actual + tiempo promedio de la tarifa) y la hora de despacho, y guarda con `update`.
- **Punto a resaltar:** **concurrencia** — se hace asíncrono para no congelar la ventana.

### `public boolean esPedidoDeRecojo(String codPedido)`
- **Qué hace:** trae el pedido y responde `true` si la dirección empieza con "RECOJO EN
  TIENDA" **o** si su tarifa es de recojo (`Tarifa.esRecojo()`).
- **Punto a resaltar:** detección robusta (por dato guardado o por tarifa); la usa el monitor
  para **bloquear** la asignación de repartidor a un recojo.

### `public void actualizarEstadoPedido(String codPedido, String nuevoEstado)`
- **Qué hace:** compara el estado anterior con el nuevo y decide:
  - activo → CANCELADO: `ventaRepository.reponerStockPorCancelacion(...)` (devuelve stock).
  - CANCELADO → activo: `ventaRepository.descontarStockPorReactivacion(...)` (re-descuenta).
  - otro cambio: solo `pedidoRepository.update(...)`.
- **Punto a resaltar:** mantiene el **stock coherente** sin importar las transiciones.

## B.5 `VentaRepository.java`

### `public List<String> registrarVenta(Pedido pedido, List<ItemVenta> items)`
- **Qué hace (transacción):**
  1. `con.setAutoCommit(false)` → empieza la transacción manual.
  2. Inserta el `Pedido` con `OUTPUT INSERTED.CodPedido` y recupera el **código autogenerado**.
  3. Por cada ítem: lee el stock; si no alcanza, lanza `ValidationException` (que dispara el
     **rollback**); si alcanza, inserta en `Pedido_Producto` y hace
     `UPDATE Producto SET Stock = Stock - cantidad`.
  4. Junta los productos cuyo stock quedó en 0.
  5. `con.commit()` confirma todo; el `catch` hace `con.rollback()`.
- **Qué devuelve:** la lista de nombres de productos en 0.
- **Punto a resaltar:** **atomicidad** — o se hace todo (pedido + ítems + stock) o nada;
  y `OUTPUT INSERTED.CodPedido` es cómo SQL Server devuelve la clave recién creada.

### `reponerStockPorCancelacion(...)` y `descontarStockPorReactivacion(...)`
- **Qué hacen:** leen los ítems del pedido de `Pedido_Producto` y, en transacción, suman
  (cancelación) o restan (reactivación, validando que alcance) el stock, y actualizan el estado.

## B.6 `PanelNuevaVenta.java` (la vista)

### `private void agregarAlCarrito()`
- **Qué hace:** toma el producto seleccionado, pide la cantidad, calcula el **stock
  disponible** (stock del menú − lo que ya hay en el carrito de ese producto) y, si la cantidad
  pedida lo supera, avisa y no agrega. Si está OK, agrega la fila y actualiza el total.
- **Punto a resaltar:** validación **inmediata** de stock, antes de generar el pedido.

### `private void generarPedido()`
- **Qué hace:** Paso 1 elige modalidad (recojo/delivery); Paso 2 muestra el formulario
  correspondiente (en delivery, con el **total en vivo** que se recalcula al cambiar la zona);
  arma la lista de `ItemVenta`, calcula `montoTotal = productos + envío`, llama a
  `pedidoController.generarPedido(...)` y, si vuelven productos en 0, muestra el aviso de
  reabastecer. Al final `limpiarPantalla()` recarga el menú con el stock actualizado.
- **Punto a resaltar:** la vista es "tonta": **no** sabe SQL ni reglas; solo recoge datos y se
  los pasa al controlador.

### `private void vaciarCarrito()`
- **Qué hace:** borra todas las filas del carrito y pone el total en 0 (para empezar de cero).

## B.7 `ApiRepartidor.java`

### `public static void main(String[] args)`
- **Qué hace:** crea la app Javalin con CORS, la arranca en `:8080` y registra las rutas:
  - `GET /repartidor` → sirve el HTML de la página del repartidor.
  - `GET /api/repartidores/{cod}/pedidos` → valida el código, busca el **nombre** del
    repartidor (si no existe responde **404**) y devuelve en JSON `nombreRepartidor` + la lista
    de pedidos **EN CAMINO**.
  - `POST /api/repartidores/{cod}/pedidos/{codPedido}/entregar` → marca el pedido como
    ENTREGADO si pertenece a ese repartidor.
- **Punto a resaltar:** es una **API REST** (verbos GET/POST, respuestas JSON, código 404),
  la segunda interfaz del sistema además del escritorio.

## B.8 Apoyos transversales

- **`DBConnection.getConexion()`**: lee `application.properties` (una sola vez, en un bloque
  `static`) y devuelve una conexión nueva a SQL Server con `DriverManager`. Todos los
  repositorios la usan.
- **`IRepositorioBase<T, ID>`**: la **interfaz genérica** con `insert/findById/findAll/update/
  deleteById`. Es el contrato CRUD que implementan los repositorios → **polimorfismo + genéricos**.
- **`Tarifa.esRecojo()`**: devuelve `true` si el precio es 0 o el nombre contiene
  "retiro"/"tienda"; así el sistema distingue recojo de delivery.
- **Excepciones** (`DomainException` → `ValidationException`, `NotFoundException`): **herencia**
  para clasificar los errores de negocio.

---

## Checklist antes de grabar

- [ ] SQL Server encendido y `LaPtiteFranceDB` creada con `SQLQuery1.sql` (con las zonas
      renombradas: Lima Metropolitana / Callao).
- [ ] Proyecto compilado sin errores.
- [ ] Tener a mano los códigos: asistentes `E001`/`E002`/`E003`, repartidores `E004`/`E005`,
      cliente de prueba celular `987654321`.
- [ ] Borrar o vaciar `logs/auditoria-logins.txt` antes de grabar, para mostrarlo limpio.
- [ ] Navegador abierto en una pestaña para la parte de la API (`/repartidor`).
