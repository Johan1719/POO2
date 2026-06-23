USE LaPtiteFranceDB;
GO

-- ==========================================
-- 0. DESTRUCCIÓN SEGURA
-- ==========================================
DROP TABLE IF EXISTS Pedido_Producto;
DROP TABLE IF EXISTS Pedido;
DROP TABLE IF EXISTS Telefono;
DROP TABLE IF EXISTS Asistente;
DROP TABLE IF EXISTS Repartidor;
DROP TABLE IF EXISTS Producto;
DROP TABLE IF EXISTS Empleado;
DROP TABLE IF EXISTS Pago;
DROP TABLE IF EXISTS Tarifa;
DROP TABLE IF EXISTS Categoria;
DROP TABLE IF EXISTS Cliente;
GO

-- ==========================================
-- 1. SECUENCIAS
-- ==========================================
IF OBJECT_ID('dbo.seq_cliente', 'SO') IS NOT NULL DROP SEQUENCE dbo.seq_cliente;
IF OBJECT_ID('dbo.seq_empleado', 'SO') IS NOT NULL DROP SEQUENCE dbo.seq_empleado;
IF OBJECT_ID('dbo.seq_producto', 'SO') IS NOT NULL DROP SEQUENCE dbo.seq_producto;
IF OBJECT_ID('dbo.seq_pago', 'SO') IS NOT NULL DROP SEQUENCE dbo.seq_pago;
IF OBJECT_ID('dbo.seq_pedido', 'SO') IS NOT NULL DROP SEQUENCE dbo.seq_pedido;
GO

CREATE SEQUENCE dbo.seq_cliente START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE dbo.seq_empleado START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE dbo.seq_producto START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE dbo.seq_pago START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE dbo.seq_pedido START WITH 1 INCREMENT BY 1;
GO

-- ==========================================
-- 2. TABLAS MAESTRAS (Catálogos sin secuencia)
-- ==========================================
CREATE TABLE Categoria (
    CodCat CHAR(6) PRIMARY KEY,
    NombreCat VARCHAR(30)
);

CREATE TABLE Tarifa (
    CodTarifa CHAR(3) PRIMARY KEY,
    NombreZona VARCHAR(30),
    PrecioTarifa FLOAT,
    TiempoPromedio INT
);

-- ==========================================
-- 3. TABLAS CON CRECIMIENTO (Con secuencias por DEFAULT)
-- ==========================================
CREATE TABLE Cliente (
    IDCliente CHAR(4) PRIMARY KEY DEFAULT ('C' + RIGHT('000' + CAST(NEXT VALUE FOR dbo.seq_cliente AS VARCHAR(10)), 3)),
    FechaRegistro DATETIME,
    NombreCliente VARCHAR(30),
    Nrocelular CHAR(9)
);

CREATE TABLE Empleado (
    CodEmpleado CHAR(4) PRIMARY KEY DEFAULT ('E' + RIGHT('000' + CAST(NEXT VALUE FOR dbo.seq_empleado AS VARCHAR(10)), 3)),
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
    CodAsistente CHAR(4) PRIMARY KEY FOREIGN KEY REFERENCES Empleado(CodEmpleado)
);

CREATE TABLE Repartidor (
    CodRepartidor CHAR(4) PRIMARY KEY FOREIGN KEY REFERENCES Empleado(CodEmpleado)
);

CREATE TABLE Pago (
    CodPago CHAR(5) PRIMARY KEY DEFAULT ('PG' + RIGHT('000' + CAST(NEXT VALUE FOR dbo.seq_pago AS VARCHAR(10)), 3)),
    MetodoPago VARCHAR(30),
    FechaPago DATETIME,
    MontoTotal FLOAT,
    Observaciones VARCHAR(30),
    CantDesc FLOAT,
    IGV FLOAT,
    CostoTarifa FLOAT
);

CREATE TABLE Producto (
    CodProducto CHAR(5) PRIMARY KEY DEFAULT ('PR' + RIGHT('000' + CAST(NEXT VALUE FOR dbo.seq_producto AS VARCHAR(10)), 3)),
    NombreProd VARCHAR(30),
    Stock SMALLINT,
    PrecioProd FLOAT,
    CodCat CHAR(6) FOREIGN KEY REFERENCES Categoria(CodCat),
    Activo BIT NOT NULL DEFAULT 1
);

CREATE TABLE Pedido (
    CodPedido CHAR(5) PRIMARY KEY DEFAULT ('P' + RIGHT('0000' + CAST(NEXT VALUE FOR dbo.seq_pedido AS VARCHAR(10)), 4)),
    Fechasolicitud DATETIME,
    MontoPedido FLOAT,
    Estado VARCHAR(30),
    TiempoEntEstimado DATETIME,
    TiempoEntReal DATETIME,
    HoraEnvio DATETIME,
    DireccionEntrega VARCHAR(100), 
    CodAsistente CHAR(4) FOREIGN KEY REFERENCES Asistente(CodAsistente), 
    CodRepartidor CHAR(4) FOREIGN KEY REFERENCES Repartidor(CodRepartidor),
    IDCliente CHAR(4) FOREIGN KEY REFERENCES Cliente(IDCliente),
    CodTarifa CHAR(3) FOREIGN KEY REFERENCES Tarifa(CodTarifa),
    CodPago CHAR(5) FOREIGN KEY REFERENCES Pago(CodPago)
);

CREATE TABLE Pedido_Producto (
    CodProducto CHAR(5) FOREIGN KEY REFERENCES Producto(CodProducto),
    CodPedido CHAR(5) FOREIGN KEY REFERENCES Pedido(CodPedido),
    CantProd SMALLINT,
    PRIMARY KEY (CodProducto, CodPedido)
);
GO

-- ==========================================
-- 4. INSERCIÓN DE DATOS (Sincronización Simple)
-- ==========================================

-- Catálogos (Insertamos con códigos fijos)
INSERT INTO Categoria (CodCat, NombreCat) VALUES 
('CAT001', 'Panadería'), ('CAT002', 'Pastelería'), ('CAT003', 'Bebidas Calientes'), ('CAT004', 'Sándwiches');

INSERT INTO Tarifa (CodTarifa, NombreZona, PrecioTarifa, TiempoPromedio) VALUES 
('T01', 'Retiro en Tienda', 0.00, 5), ('T02', 'Huaral Centro', 5.00, 20), ('T03', 'Alrededores Huaral', 8.50, 45);

-- Entidades base
INSERT INTO Cliente (FechaRegistro, NombreCliente, Nrocelular) VALUES 
(GETDATE(), 'Valeria Mendoza', '987654321'), (GETDATE(), 'Carlos Ruiz', '912345678'), (GETDATE(), 'Sofia Carrillo', '999888777');

INSERT INTO Empleado (Nombre, Numero, Direccion, CorreoElec, AniosExp) VALUES 
('Jean Piere Micuilla', '120', 'Av. Arequipa', 'jean@ptitefrance.com', 2),
('Bruno Diaz Seki', '445', 'Av. Javier Prado', 'bruno@ptitefrance.com', 3),
('Alessandra Fuentes', '789', 'Av. La Marina', 'ale@ptitefrance.com', 1),
('Johan Vasquez', '321', 'Calle Los Pinos', 'johan@ptitefrance.com', 2),
('Jazmin Mendoza', '654', 'Av. Salaverry', 'jazmin@ptitefrance.com', 1);

INSERT INTO Telefono (CodEmpleado, NroTelefono) VALUES 
('E001', '987111222'), ('E002', '987333444'), ('E004', '987555666');

INSERT INTO Asistente (CodAsistente) VALUES ('E001'), ('E002'), ('E003');
INSERT INTO Repartidor (CodRepartidor) VALUES ('E004'), ('E005');

INSERT INTO Producto (NombreProd, Stock, PrecioProd, CodCat) VALUES 
('Croissant Clásico', 50, 6.50, 'CAT001'),
('Pain au Chocolat', 35, 8.00, 'CAT001'),
('Macaron de Frambuesa', 20, 5.50, 'CAT002'),
('Café Americano', 100, 7.00, 'CAT003'),
('Baguette Tradicional', 0, 6.00, 'CAT001');

INSERT INTO Pago (MetodoPago, FechaPago, MontoTotal, Observaciones, CantDesc, IGV, CostoTarifa) VALUES 
('Yape', GETDATE(), 0.0, 'Pago por Yape', 0.0, 0.0, 0.0),
('Efectivo', GETDATE(), 0.0, 'Pago al contado', 0.0, 0.0, 0.0);

-- ==========================================
-- 4B. DATOS EXTRA (40 usuarios y 40 productos) para probar paginado
-- ==========================================

-- 40 usuarios extra (clientes)
DECLARE @u INT = 4;
WHILE @u < 44
BEGIN
    INSERT INTO Cliente (FechaRegistro, NombreCliente, Nrocelular)
    VALUES (
        DATEADD(MINUTE, -(@u * 2), GETDATE()),
        'Cliente ' + CAST(@u AS VARCHAR(10)),
        RIGHT('9' + CAST(@u AS VARCHAR(10)), 9)
    );
    SET @u = @u + 1;
END;

-- 40 productos extra
DECLARE @pr INT = 6;
WHILE @pr < 46
BEGIN
    DECLARE @codCat CHAR(6);
    SET @codCat = CASE
        WHEN (@pr % 4) = 1 THEN 'CAT001'
        WHEN (@pr % 4) = 2 THEN 'CAT002'
        WHEN (@pr % 4) = 3 THEN 'CAT003'
        ELSE 'CAT004'
    END;

    INSERT INTO Producto (NombreProd, Stock, PrecioProd, CodCat, Activo)
    VALUES (
        'Producto ' + CAST(@pr AS VARCHAR(10)),
        (@pr % 50) + 1,
        CAST((@pr * 1.1) AS FLOAT) / 10.0,
        @codCat,
        1
    );

    SET @pr = @pr + 1;
END;

-- ==========================================
-- 5. SINCRONIZACIÓN PERFECTA (Pedidos + Detalles)
-- Estrategia B: NO hardcodear CodPedido. Capturar el CodPedido real generado.
-- ==========================================
GO

DECLARE @CodPedido1 CHAR(5);
DECLARE @CodPedido2 CHAR(5);

DECLARE @t TABLE (CodPedido CHAR(5));

-- Insertar muchos pedidos para probar paginado (120 escenarios => ~240 pedidos aprox.)
DECLARE @i INT = 1;
DECLARE @CodPedido CHAR(5);

WHILE @i <= 120
BEGIN
    -- ===== Pedido EN ESPERA =====
    DELETE FROM @t;

    INSERT INTO Pedido (Fechasolicitud, MontoPedido, Estado, TiempoEntEstimado, TiempoEntReal, DireccionEntrega, CodAsistente, CodRepartidor, IDCliente, CodTarifa, CodPago)
    OUTPUT INSERTED.CodPedido INTO @t
    VALUES
    (DATEADD(MINUTE, -(@i * 3), GETDATE()),
     (15.00 + (@i * 0.10)),
     'EN ESPERA',
     DATEADD(MINUTE, 35 + (@i % 10), GETDATE()),
     NULL,
     'Residencial San Isidro ' + CAST(@i AS VARCHAR(10)),
     CASE WHEN (@i % 2) = 0 THEN 'E001' ELSE 'E002' END,
     CASE WHEN (@i % 2) = 0 THEN 'E004' ELSE 'E005' END,
     CASE WHEN (@i % 3) = 0 THEN 'C001' WHEN (@i % 3) = 1 THEN 'C002' ELSE 'C003' END,
     'T01',
     'PG001');

    SELECT TOP 1 @CodPedido = CodPedido FROM @t;

    -- Detalle EN ESPERA (2 a 3 ítems)
    INSERT INTO Pedido_Producto (CodProducto, CodPedido, CantProd)
    VALUES
    ('PR001', @CodPedido, 1 + (@i % 3)),
    ('PR002', @CodPedido, 1 + (@i % 2)),
    ('PR003', @CodPedido, 1 + (@i % 4));

    -- ===== Pedido EN CAMINO =====
    DELETE FROM @t;

    INSERT INTO Pedido (Fechasolicitud, MontoPedido, Estado, TiempoEntEstimado, TiempoEntReal, DireccionEntrega, CodAsistente, CodRepartidor, IDCliente, CodTarifa, CodPago)
    OUTPUT INSERTED.CodPedido INTO @t
    VALUES
    (DATEADD(MINUTE, -(@i * 3) - 1, GETDATE()),
     (12.00 + (@i * 0.07)),
     'EN CAMINO',
     DATEADD(MINUTE, 20 + (@i % 15), GETDATE()),
     NULL,
     'Av. Camino Real ' + CAST(@i AS VARCHAR(10)),
     CASE WHEN (@i % 2) = 0 THEN 'E002' ELSE 'E001' END,
     CASE WHEN (@i % 2) = 0 THEN 'E005' ELSE 'E004' END,
     CASE WHEN (@i % 3) = 0 THEN 'C002' WHEN (@i % 3) = 1 THEN 'C001' ELSE 'C003' END,
     'T02',
     'PG002');

    SELECT TOP 1 @CodPedido = CodPedido FROM @t;

    INSERT INTO Pedido_Producto (CodProducto, CodPedido, CantProd)
    VALUES
    ('PR004', @CodPedido, 1 + (@i % 5)),
    ('PR001', @CodPedido, 1 + (@i % 2));

    -- ===== Pedido ENTREGADO =====
    DELETE FROM @t;

    INSERT INTO Pedido (Fechasolicitud, MontoPedido, Estado, TiempoEntEstimado, TiempoEntReal, DireccionEntrega, CodAsistente, CodRepartidor, IDCliente, CodTarifa, CodPago)
    OUTPUT INSERTED.CodPedido INTO @t
    VALUES
    (DATEADD(MINUTE, -(@i * 4), GETDATE()),
     (10.00 + (@i * 0.08)),
     'ENTREGADO',
     DATEADD(MINUTE, 25 + (@i % 12), GETDATE()),
     DATEADD(MINUTE, -(@i % 30), GETDATE()),
     'Oficina Miraflores ' + CAST(@i AS VARCHAR(10)),
     CASE WHEN (@i % 2) = 0 THEN 'E002' ELSE 'E001' END,
     CASE WHEN (@i % 2) = 0 THEN 'E005' ELSE 'E004' END,
     CASE WHEN (@i % 3) = 0 THEN 'C002' WHEN (@i % 3) = 1 THEN 'C001' ELSE 'C003' END,
     'T02',
     'PG002');

    SELECT TOP 1 @CodPedido = CodPedido FROM @t;

    INSERT INTO Pedido_Producto (CodProducto, CodPedido, CantProd)
    VALUES
    ('PR003', @CodPedido, 1 + (@i % 4)),
    ('PR004', @CodPedido, 1 + (@i % 2));

    SET @i = @i + 1;
END;

-- ==========================================
-- 6. VERIFICACIÓN FINAL
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

-- (2) Vista rápida extra igual que tu script original
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

USE LaPtiteFranceDB;
GO

select * from repartidor