USE LaPtiteFranceDB;
GO

-- 1. Eliminamos las tablas genéricas que creamos hace un rato (el orden importa)
DROP TABLE IF EXISTS Pedido;
DROP TABLE IF EXISTS Direccion;
DROP TABLE IF EXISTS Telefono;
DROP TABLE IF EXISTS Cliente;
GO

-- 2. Creamos la tabla Cliente EXACTAMENTE con los tipos de dato de tu diagrama
CREATE TABLE Cliente (
    IDCliente CHAR(4) PRIMARY KEY,
    FechaRegistro DATETIME,
    NombreCliente VARCHAR(30),
    Nrocelular CHAR(9)
);
GO

USE LaPtiteFranceDB;
GO

-- Esto te mostrará todas las filas y columnas guardadas en la tabla
SELECT * FROM Cliente;

USE LaPtiteFranceDB;
GO

-- ==========================================
-- 1. TABLAS MAESTRAS (Sin llaves foráneas)
-- ==========================================

CREATE TABLE Categoria (
    CodCat CHAR(6) PRIMARY KEY,
    NombreCat VARCHAR(30)
);

CREATE TABLE Tarifa (
    CodTarifa CHAR(3) PRIMARY KEY,
    NombreTarifa VARCHAR(30),
    PrecioTarifa FLOAT
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
    Numero VARCHAR(10), -- Configurado específicamente como el número de calle
    Direccion VARCHAR(30),
    CorreoElec VARCHAR(30),
    AniosExp SMALLINT
);

-- Tabla de teléfonos normalizada
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
-- 3. TABLAS TRANSACCIONALES Y DEPENDIENTES
-- ==========================================

CREATE TABLE Producto (
    CodProducto CHAR(5) PRIMARY KEY,
    NombreProd VARCHAR(30),
    Stock SMALLINT,
    PrecioProd FLOAT,
    CodCat CHAR(6) FOREIGN KEY REFERENCES Categoria(CodCat)
);

CREATE TABLE Direccion (
    Ubicacion VARCHAR(30),
    distrito VARCHAR(30),
    AreaLoc VARCHAR(30),
    IDCliente CHAR(4),
    Numero VARCHAR(10), -- Número de calle 
    PRIMARY KEY (Ubicacion, distrito, AreaLoc, IDCliente),
    FOREIGN KEY (IDCliente) REFERENCES Cliente(IDCliente)
);

CREATE TABLE Atencion (
    IDCliente CHAR(4),
    CodAsistente CHAR(4),
    FechaAtencion DATETIME,
    PRIMARY KEY (IDCliente, CodAsistente),
    FOREIGN KEY (IDCliente) REFERENCES Cliente(IDCliente),
    FOREIGN KEY (CodAsistente) REFERENCES Asistente(CodAsistente)
);

-- ==========================================
-- 4. EL NÚCLEO: PEDIDO
-- ==========================================

CREATE TABLE Pedido (
    CodPedido CHAR(5) PRIMARY KEY,
    Fechasolicitud DATETIME,
    MontoPedido FLOAT,
    Estado VARCHAR(30),
    TiempoEntEstimado DATETIME,
    TiempoEntReal DATETIME,
    CodRepartidor CHAR(4) FOREIGN KEY REFERENCES Repartidor(CodRepartidor),
    IDCliente CHAR(4) FOREIGN KEY REFERENCES Cliente(IDCliente),
    CodTarifa CHAR(3) FOREIGN KEY REFERENCES Tarifa(CodTarifa),
    CodPago CHAR(5) FOREIGN KEY REFERENCES Pago(CodPago)
);

-- Tabla intermedia (Relación Muchos a Muchos)
CREATE TABLE Pedido_Producto (
    CodProducto CHAR(5),
    CodPedido CHAR(5),
    CantProd SMALLINT,
    PRIMARY KEY (CodProducto, CodPedido),
    FOREIGN KEY (CodProducto) REFERENCES Producto(CodProducto),
    FOREIGN KEY (CodPedido) REFERENCES Pedido(CodPedido)
);
GO

USE LaPtiteFranceDB;
GO

SELECT * FROM Cliente;
SELECT * FROM Categoria;
SELECT * FROM Tarifa;
SELECT * FROM Pago;
SELECT * FROM Empleado;
SELECT * FROM Telefono;
SELECT * FROM Asistente;
SELECT * FROM Repartidor;
SELECT * FROM Producto;
SELECT * FROM Direccion;
SELECT * FROM Atencion;
SELECT * FROM Pedido;
SELECT * FROM Pedido_Producto;
GO

USE LaPtiteFranceDB;
GO

DELETE FROM Pedido_Producto;
DELETE FROM Pedido;
DELETE FROM Atencion;
DELETE FROM Direccion;
DELETE FROM Telefono;
DELETE FROM Producto;
DELETE FROM Repartidor;
DELETE FROM Asistente;
DELETE FROM Pago;
DELETE FROM Tarifa;
DELETE FROM Categoria;
DELETE FROM Empleado;
DELETE FROM Cliente;
GO


DBCC CHECKIDENT ('Telefono', RESEED, 0);
GO

SELECT COUNT(*) AS Registros FROM Cliente;
SELECT COUNT(*) AS Registros FROM Categoria;
SELECT COUNT(*) AS Registros FROM Tarifa;
SELECT COUNT(*) AS Registros FROM Pago;
SELECT COUNT(*) AS Registros FROM Empleado;
SELECT COUNT(*) AS Registros FROM Telefono;
SELECT COUNT(*) AS Registros FROM Asistente;
SELECT COUNT(*) AS Registros FROM Repartidor;
SELECT COUNT(*) AS Registros FROM Producto;
SELECT COUNT(*) AS Registros FROM Direccion;
SELECT COUNT(*) AS Registros FROM Atencion;
SELECT COUNT(*) AS Registros FROM Pedido;
SELECT COUNT(*) AS Registros FROM Pedido_Producto;
GO











USE LaPtiteFranceDB;
GO

-- ==========================================
-- 0. DESTRUCCIÓN SEGURA (Orden inverso: Hijos -> Padres)
-- ==========================================
DROP TABLE IF EXISTS Pedido_Producto;
DROP TABLE IF EXISTS Pedido;
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
-- 1. TABLAS MAESTRAS (Sin llaves foráneas)
-- ==========================================

CREATE TABLE Cliente (
    IDCliente CHAR(4) PRIMARY KEY,
    FechaRegistro DATETIME,
    NombreCliente VARCHAR(30),
    Nrocelular CHAR(9)
);

CREATE TABLE Categoria (
    CodCat CHAR(6) PRIMARY KEY,
    NombreCat VARCHAR(30)
);

CREATE TABLE Tarifa (
    CodTarifa CHAR(3) PRIMARY KEY,
    NombreTarifa VARCHAR(30),
    PrecioTarifa FLOAT
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
    Numero VARCHAR(10), -- Número de calle
    Direccion VARCHAR(30),
    CorreoElec VARCHAR(30),
    AniosExp SMALLINT
);

-- Tabla de teléfonos normalizada
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
-- 3. TABLAS TRANSACCIONALES Y DEPENDIENTES
-- ==========================================

CREATE TABLE Producto (
    CodProducto CHAR(5) PRIMARY KEY,
    NombreProd VARCHAR(30),
    Stock SMALLINT,
    PrecioProd FLOAT,
    CodCat CHAR(6) FOREIGN KEY REFERENCES Categoria(CodCat)
);

CREATE TABLE Direccion (
    Ubicacion VARCHAR(30),
    distrito VARCHAR(30),
    AreaLoc VARCHAR(30),
    IDCliente CHAR(4),
    Numero VARCHAR(10), -- Número de calle 
    PRIMARY KEY (Ubicacion, distrito, AreaLoc, IDCliente),
    FOREIGN KEY (IDCliente) REFERENCES Cliente(IDCliente)
);

CREATE TABLE Atencion (
    IDCliente CHAR(4),
    CodAsistente CHAR(4),
    FechaAtencion DATETIME,
    PRIMARY KEY (IDCliente, CodAsistente),
    FOREIGN KEY (IDCliente) REFERENCES Cliente(IDCliente),
    FOREIGN KEY (CodAsistente) REFERENCES Asistente(CodAsistente)
);

-- ==========================================
-- 4. EL NÚCLEO: PEDIDO
-- ==========================================

CREATE TABLE Pedido (
    CodPedido CHAR(5) PRIMARY KEY,
    Fechasolicitud DATETIME,
    MontoPedido FLOAT,
    Estado VARCHAR(30),
    TiempoEntEstimado DATETIME,
    TiempoEntReal DATETIME,
    CodRepartidor CHAR(4) FOREIGN KEY REFERENCES Repartidor(CodRepartidor),
    IDCliente CHAR(4) FOREIGN KEY REFERENCES Cliente(IDCliente),
    CodTarifa CHAR(3) FOREIGN KEY REFERENCES Tarifa(CodTarifa),
    CodPago CHAR(5) FOREIGN KEY REFERENCES Pago(CodPago)
);

-- Tabla intermedia (Relación Muchos a Muchos)
CREATE TABLE Pedido_Producto (
    CodProducto CHAR(5),
    CodPedido CHAR(5),
    CantProd SMALLINT,
    PRIMARY KEY (CodProducto, CodPedido),
    FOREIGN KEY (CodProducto) REFERENCES Producto(CodProducto),
    FOREIGN KEY (CodPedido) REFERENCES Pedido(CodPedido)
);
GO

USE LaPtiteFranceDB;
GO

USE LaPtiteFranceDB;
GO

-- ==========================================
-- 0. LIMPIEZA TOTAL (De Hijos a Padres)
-- ==========================================
DELETE FROM Pedido_Producto;
DELETE FROM Pedido;
DELETE FROM Atencion;
DELETE FROM Direccion;
DELETE FROM Producto;
DELETE FROM Repartidor;
DELETE FROM Asistente;
DELETE FROM Telefono;
DELETE FROM Empleado;
DELETE FROM Pago;
DELETE FROM Tarifa;
DELETE FROM Categoria;
DELETE FROM Cliente;
GO

-- ==========================================
-- 1. DATOS MAESTROS
-- ==========================================
INSERT INTO Categoria (CodCat, NombreCat) VALUES 
('CAT001', 'Panadería'),
('CAT002', 'Pastelería'),
('CAT003', 'Bebidas Calientes'),
('CAT004', 'Sándwiches');

INSERT INTO Tarifa (CodTarifa, NombreTarifa, PrecioTarifa) VALUES 
('T01', 'Zona Local', 5.00),
('T02', 'Zona Intermedia', 8.50),
('T03', 'Zona Lejana', 12.00);

INSERT INTO Cliente (IDCliente, FechaRegistro, NombreCliente, Nrocelular) VALUES 
('C001', GETDATE(), 'Valeria Mendoza', '987654321'),
('C002', GETDATE(), 'Carlos Ruiz', '912345678'),
('C003', GETDATE(), 'Sofia Carrillo', '999888777');
GO

-- ==========================================
-- 2. EL EQUIPO DE TRABAJO (Empleados)
-- ==========================================
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

INSERT INTO Asistente (CodAsistente) VALUES ('E001'), ('E002'), ('E003');
INSERT INTO Repartidor (CodRepartidor) VALUES ('E004'), ('E005');
GO

-- ==========================================
-- 3. CATÁLOGO Y DIRECCIONES
-- ==========================================
INSERT INTO Producto (CodProducto, NombreProd, Stock, PrecioProd, CodCat) VALUES 
('PR001', 'Croissant Clásico', 50, 6.50, 'CAT001'),
('PR002', 'Pain au Chocolat', 35, 8.00, 'CAT001'),
('PR003', 'Macaron de Frambuesa', 20, 5.50, 'CAT002'),
('PR004', 'Café Americano', 100, 7.00, 'CAT003'),
('PR005', 'Baguette Tradicional', 0, 6.00, 'CAT001');

INSERT INTO Direccion (Ubicacion, distrito, AreaLoc, IDCliente, Numero) VALUES 
('Residencial', 'San Isidro', 'Lima Centro', 'C001', '145'),
('Oficina', 'Miraflores', 'Lima Sur', 'C002', '890');
GO

-- ==========================================
-- 4. PEDIDOS DE PRUEBA
-- ==========================================
INSERT INTO Pago (CodPago, MetodoPago, FechaPago, MontoTotal, Observaciones, CantDesc, IGV, CostoTarifa) VALUES 
('PG001', 'Yape', GETDATE(), 26.00, 'Pago exacto', 0.0, 3.96, 5.00),
('PG002', 'Tarjeta Visa', GETDATE(), 16.50, 'Sin contacto', 0.0, 2.51, 5.00);

INSERT INTO Pedido (CodPedido, Fechasolicitud, MontoPedido, Estado, TiempoEntEstimado, TiempoEntReal, CodRepartidor, IDCliente, CodTarifa, CodPago) VALUES 
('PD001', GETDATE(), 21.00, 'PENDIENTE', DATEADD(MINUTE, 35, GETDATE()), NULL, 'E004', 'C001', 'T01', 'PG001'),
('PD002', GETDATE(), 11.50, 'ENTREGADO', DATEADD(MINUTE, 30, GETDATE()), GETDATE(), 'E005', 'C002', 'T01', 'PG002');

INSERT INTO Pedido_Producto (CodProducto, CodPedido, CantProd) VALUES 
('PR001', 'PD001', 2),
('PR002', 'PD001', 1),
('PR003', 'PD002', 1),
('PR005', 'PD002', 1);
GO



USE LaPtiteFranceDB;
GO

SELECT 
    CodPedido, 
    IdCliente, 
    MontoPedido, 
    Estado, 
    FechaSolicitud 
FROM 
    Pedido
ORDER BY 
    FechaSolicitud DESC;