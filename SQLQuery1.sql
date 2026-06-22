USE LaPtiteFranceDB;
GO

-- ==========================================
-- 0. DESTRUCCI�N SEGURA (Orden inverso: Hijos -> Padres)
-- ==========================================
DROP TABLE IF EXISTS Pedido_Producto;
DROP TABLE IF EXISTS Pedido;

-- Borramos las tablas viejas para liberar al Cliente y Asistente
DROP TABLE IF EXISTS Atencion;
DROP TABLE IF EXISTS Direccion;

DROP TABLE IF EXISTS Producto;
DROP TABLE IF EXISTS Repartidor;
DROP TABLE IF EXISTS Asistente;
DROP TABLE IF EXISTS Telefono;
DROP TABLE IF EXISTS Empleado;
DROP TABLE IF EXISTS Pago;
DROP TABLE IF EXISTS Tarifa;
DROP TABLE IF EXISTS Categoria;
DROP TABLE IF EXISTS Cliente;
GO

-- ==========================================
-- 1. TABLAS MAESTRAS (Sin llaves for�neas)
-- ==========================================
-- ==========================================
-- SECUENCIAS (autogeneración de códigos)
-- ==========================================
IF OBJECT_ID('dbo.seq_cliente', 'SO') IS NOT NULL DROP SEQUENCE dbo.seq_cliente;
IF OBJECT_ID('dbo.seq_pedido', 'SO') IS NOT NULL DROP SEQUENCE dbo.seq_pedido;
IF OBJECT_ID('dbo.seq_producto', 'SO') IS NOT NULL DROP SEQUENCE dbo.seq_producto;
GO

CREATE SEQUENCE dbo.seq_cliente START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE dbo.seq_pedido START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE dbo.seq_producto START WITH 1 INCREMENT BY 1;
GO

CREATE TABLE Cliente (
    IDCliente CHAR(4) NOT NULL DEFAULT ( 'C' + RIGHT('000' + CAST(NEXT VALUE FOR dbo.seq_cliente AS VARCHAR(10)), 3) ),
    FechaRegistro DATETIME,
    NombreCliente VARCHAR(30),
    Nrocelular CHAR(9),
    CONSTRAINT PK_Cliente PRIMARY KEY (IDCliente)
);



CREATE TABLE Categoria (
    CodCat CHAR(6) PRIMARY KEY,
    NombreCat VARCHAR(30)
);

CREATE TABLE Tarifa (
    CodTarifa CHAR(3) PRIMARY KEY,
    NombreZona VARCHAR(30),
    PrecioTarifa FLOAT,
    TiempoPromedio INT -- Tiempo logístico en minutos
);

CREATE TABLE Pago (
    CodPago CHAR(5) PRIMARY KEY,
    MetodoPago VARCHAR(30),
    FechaPago DATETIME,
    MontoTotal FLOAT,
    Observaciones VARCHAR(30),
    CantDesc FLOAT,
    IGV FLOAT,
    CostoTarifa FLOAT
);

-- ==========================================
-- 2. TABLAS DE EMPLEADOS Y CONTACTO
-- ==========================================
CREATE TABLE Empleado (
    CodEmpleado CHAR(4) PRIMARY KEY,
    Nombre VARCHAR(30),
    Numero VARCHAR(10), 
    Direccion VARCHAR(30),
    CorreoElec VARCHAR(30),
    AniosExp SMALLINT
);

CREATE TABLE Telefono (
    IDTelefono INT IDENTITY(1,1) PRIMARY KEY,
    CodEmpleado CHAR(4) FOREIGN KEY REFERENCES Empleado(CodEmpleado),
    NroTelefono VARCHAR(15) NOT NULL
);

CREATE TABLE Asistente (
    CodAsistente CHAR(4) PRIMARY KEY,
    FOREIGN KEY (CodAsistente) REFERENCES Empleado(CodEmpleado)
);

CREATE TABLE Repartidor (
    CodRepartidor CHAR(4) PRIMARY KEY,
    FOREIGN KEY (CodRepartidor) REFERENCES Empleado(CodEmpleado)
);

-- ==========================================
-- 3. TABLAS TRANSACCIONALES
-- ==========================================
CREATE TABLE Producto (
    CodProducto CHAR(5) PRIMARY KEY,
    NombreProd VARCHAR(30),
    Stock SMALLINT,
    PrecioProd FLOAT,
    CodCat CHAR(6) FOREIGN KEY REFERENCES Categoria(CodCat),
    Activo BIT NOT NULL DEFAULT 1
);


-- ==========================================
-- 4. EL N�CLEO: PEDIDO
-- ==========================================
CREATE TABLE Pedido (
    CodPedido CHAR(5) NOT NULL DEFAULT ('P' + RIGHT('0000' + CAST(NEXT VALUE FOR dbo.seq_pedido AS VARCHAR(10)), 4)) ,
    Fechasolicitud DATETIME,
    CONSTRAINT PK_Pedido PRIMARY KEY (CodPedido),

    MontoPedido FLOAT,
    Estado VARCHAR(30),
    TiempoEntEstimado DATETIME,
    TiempoEntReal DATETIME,
    HoraEnvio DATETIME, -- Hora en la que se despacha (asigna repartidor)

    DireccionEntrega VARCHAR(100), 
    CodAsistente CHAR(4) FOREIGN KEY REFERENCES Asistente(CodAsistente), 
    
    CodRepartidor CHAR(4) FOREIGN KEY REFERENCES Repartidor(CodRepartidor),
    IDCliente CHAR(4) FOREIGN KEY REFERENCES Cliente(IDCliente),
    CodTarifa CHAR(3) FOREIGN KEY REFERENCES Tarifa(CodTarifa),
    CodPago CHAR(5) FOREIGN KEY REFERENCES Pago(CodPago)
);

CREATE TABLE Pedido_Producto (
    CodProducto CHAR(5),
    CodPedido CHAR(5),
    CantProd SMALLINT,
    PRIMARY KEY (CodProducto, CodPedido),
    FOREIGN KEY (CodProducto) REFERENCES Producto(CodProducto),
    FOREIGN KEY (CodPedido) REFERENCES Pedido(CodPedido)
);
GO

-- ==========================================
-- 5. INSERCI�N DE DATOS LIMPIOS Y CORREGIDOS
-- ==========================================

-- Categor�as
INSERT INTO Categoria (CodCat, NombreCat) VALUES 
('CAT001', 'Panader�a'),
('CAT002', 'Pasteler�a'),
('CAT003', 'Bebidas Calientes'),
('CAT004', 'S�ndwiches');

-- Tarifas (Zonas actualizadas)
INSERT INTO Tarifa (CodTarifa, NombreZona, PrecioTarifa, TiempoPromedio) VALUES 
('T01', 'Retiro en Tienda', 0.00, 5),
('T02', 'Huaral Centro', 5.00, 20),
('T03', 'Alrededores Huaral', 8.50, 45);

-- Clientes
INSERT INTO Cliente (IDCliente, FechaRegistro, NombreCliente, Nrocelular) VALUES 
('C001', GETDATE(), 'Valeria Mendoza', '987654321'),
('C002', GETDATE(), 'Carlos Ruiz', '912345678'),
('C003', GETDATE(), 'Sofia Carrillo', '999888777');

-- Empleados
INSERT INTO Empleado (CodEmpleado, Nombre, Numero, Direccion, CorreoElec, AniosExp) VALUES 
('E001', 'Jean Piere Micuilla', '120', 'Av. Arequipa', 'jean@ptitefrance.com', 2),
('E002', 'Bruno Diaz Seki', '445', 'Av. Javier Prado', 'bruno@ptitefrance.com', 3),
('E003', 'Alessandra Fuentes', '789', 'Av. La Marina', 'ale@ptitefrance.com', 1),
('E004', 'Johan Vasquez', '321', 'Calle Los Pinos', 'johan@ptitefrance.com', 2),
('E005', 'Jazmin Mendoza', '654', 'Av. Salaverry', 'jazmin@ptitefrance.com', 1);

INSERT INTO Telefono (CodEmpleado, NroTelefono) VALUES 
('E001', '987111222'),
('E002', '987333444'),
('E004', '987555666');

-- Asignaci�n de Roles
INSERT INTO Asistente (CodAsistente) VALUES ('E001'), ('E002'), ('E003');
INSERT INTO Repartidor (CodRepartidor) VALUES ('E004'), ('E005');

-- Productos
INSERT INTO Producto (CodProducto, NombreProd, Stock, PrecioProd, CodCat) VALUES 
('PR001', 'Croissant Cl�sico', 50, 6.50, 'CAT001'),
('PR002', 'Pain au Chocolat', 35, 8.00, 'CAT001'),
('PR003', 'Macaron de Frambuesa', 20, 5.50, 'CAT002'),
('PR004', 'Caf� Americano', 100, 7.00, 'CAT003'),
('PR005', 'Baguette Tradicional', 0, 6.00, 'CAT001');

-- Pagos (Solo Yape y Efectivo)
INSERT INTO Pago (CodPago, MetodoPago, FechaPago, MontoTotal, Observaciones, CantDesc, IGV, CostoTarifa) VALUES 
('PG001', 'Yape', GETDATE(), 0.0, 'Pago por Yape', 0.0, 0.0, 0.0),
('PG002', 'Efectivo', GETDATE(), 0.0, 'Pago al contado', 0.0, 0.0, 0.0);

-- Pedidos de Prueba adaptados a los nuevos datos
USE LaPtiteFranceDB;
GO

-- ... (Mant�n toda la estructura de tablas anterior igual) ...

-- Pedidos de Prueba con Estados Vitales
INSERT INTO Pedido (CodPedido, Fechasolicitud, MontoPedido, Estado, TiempoEntEstimado, TiempoEntReal, DireccionEntrega, CodAsistente, CodRepartidor, IDCliente, CodTarifa, CodPago) VALUES 
('P0001', GETDATE(), 21.00, 'EN ESPERA', DATEADD(MINUTE, 35, GETDATE()), NULL, 'Residencial San Isidro 145', 'E001', 'E004', 'C001', 'T01', 'PG001'),
('P0002', GETDATE(), 11.50, 'ENTREGADO', DATEADD(MINUTE, 30, GETDATE()), GETDATE(), 'Oficina Miraflores 890', 'E002', 'E005', 'C002', 'T02', 'PG002');
GO

INSERT INTO Pedido_Producto (CodProducto, CodPedido, CantProd) VALUES 
('PR001', 'P0001', 2),
('PR002', 'P0001', 1),
('PR003', 'P0002', 1),
('PR005', 'P0002', 1);
GO

-- ==========================================
-- 6. VERIFICACI�N FINAL
-- ==========================================
SELECT 
    CodPedido, 
    IDCliente, 
    MontoPedido, 
    Estado, 
    Fechasolicitud,
    DireccionEntrega, 
    CodTarifa,        
    CodPago           
FROM 
    Pedido
ORDER BY 
    Fechasolicitud DESC;
GO