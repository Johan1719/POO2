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

## Checklist antes de grabar

- [ ] SQL Server encendido y `LaPtiteFranceDB` creada con `SQLQuery1.sql` (con las zonas
      renombradas: Lima Metropolitana / Callao).
- [ ] Proyecto compilado sin errores.
- [ ] Tener a mano los códigos: asistentes `E001`/`E002`/`E003`, repartidores `E004`/`E005`,
      cliente de prueba celular `987654321`.
- [ ] Borrar o vaciar `logs/auditoria-logins.txt` antes de grabar, para mostrarlo limpio.
- [ ] Navegador abierto en una pestaña para la parte de la API (`/repartidor`).
