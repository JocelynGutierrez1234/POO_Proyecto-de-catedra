-- Tablas de pagos y facturas
-- Correr después del script de la base de datos (usa la tabla pedidos)

USE taquicardia_bd;

-- una factura por pedido (UNIQUE para no cobrar dos veces)
CREATE TABLE IF NOT EXISTS facturas (
    id_factura      INT AUTO_INCREMENT PRIMARY KEY,
    numero_factura  VARCHAR(20)   NULL UNIQUE,
    id_pedido       INT           NOT NULL UNIQUE,
    id_usuario      INT           NULL,
    nombre_cliente  VARCHAR(100)  NOT NULL DEFAULT 'Consumidor Final',
    documento_cliente VARCHAR(20) NULL,
    fecha_hora      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal        DECIMAL(10,2) NOT NULL,
    iva             DECIMAL(10,2) NOT NULL,
    propina         DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total           DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_factura_pedido FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido)
);

CREATE TABLE IF NOT EXISTS pagos (
    id_pago         INT AUTO_INCREMENT PRIMARY KEY,
    id_factura      INT           NOT NULL,
    metodo_pago     ENUM('EFECTIVO','TARJETA') NOT NULL,
    monto_recibido  DECIMAL(10,2) NOT NULL,
    cambio          DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    fecha_hora      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pago_factura FOREIGN KEY (id_factura) REFERENCES facturas(id_factura)
);

-- cuando ya esté la tabla usuarios:
-- ALTER TABLE facturas
--   ADD CONSTRAINT fk_factura_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario);
