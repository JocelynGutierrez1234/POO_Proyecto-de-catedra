-- Tablas del módulo de pagos y comprobantes
-- Ejecutar después del script principal (usa la tabla pedidos)

USE taquicardia_bd;

CREATE TABLE IF NOT EXISTS facturas (
    id_factura        INT AUTO_INCREMENT PRIMARY KEY,
    numero_factura    VARCHAR(20)   NULL UNIQUE,
    id_pedido         INT           NOT NULL,
    id_usuario        INT           NULL,
    nombre_cliente    VARCHAR(100)  NOT NULL DEFAULT 'Consumidor Final',
    documento_cliente VARCHAR(20)   NULL,
    fecha_hora        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal          DECIMAL(10,2) NOT NULL,
    iva               DECIMAL(10,2) NOT NULL,
    propina           DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total             DECIMAL(10,2) NOT NULL,
    estado            ENUM('VIGENTE','ANULADA') NOT NULL DEFAULT 'VIGENTE',
    motivo_anulacion  VARCHAR(200)  NULL,
    fecha_anulacion   DATETIME      NULL,
    -- solo puede haber un comprobante vigente por pedido
    pedido_vigente    INT GENERATED ALWAYS AS (IF(estado = 'VIGENTE', id_pedido, NULL)) STORED,
    UNIQUE KEY uq_factura_pedido_vigente (pedido_vigente),
    INDEX idx_factura_pedido (id_pedido),
    INDEX idx_factura_fecha (fecha_hora),
    CONSTRAINT fk_factura_pedido FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido),
    CONSTRAINT chk_factura_montos CHECK (subtotal >= 0 AND iva >= 0 AND propina >= 0 AND total >= 0),
    CONSTRAINT chk_factura_total  CHECK (total = subtotal + iva + propina)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- un pago por comprobante
CREATE TABLE IF NOT EXISTS pagos (
    id_pago         INT AUTO_INCREMENT PRIMARY KEY,
    id_factura      INT           NOT NULL UNIQUE,
    metodo_pago     ENUM('EFECTIVO','TARJETA') NOT NULL,
    monto_recibido  DECIMAL(10,2) NOT NULL,
    cambio          DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    fecha_hora      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pago_factura FOREIGN KEY (id_factura) REFERENCES facturas(id_factura),
    CONSTRAINT chk_pago_montos CHECK (monto_recibido >= 0 AND cambio >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- cuando exista la tabla usuarios:
-- ALTER TABLE facturas
--   ADD CONSTRAINT fk_factura_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario);
