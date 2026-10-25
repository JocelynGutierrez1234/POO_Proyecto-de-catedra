-- Módulo de menú (Carlos)
-- Ejecutar DESPUÉS de crear/seleccionar taquicardia_bd y antes de probar MenuServlet.
-- No reemplaza el script maestro del equipo: pedidos, mesas y detalle_pedidos pertenecen a otros módulos.
USE taquicardia_bd;

CREATE TABLE IF NOT EXISTS categorias (
    id_categoria INT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(80) NOT NULL,
    descripcion VARCHAR(250) NULL,
    PRIMARY KEY (id_categoria),
    CONSTRAINT uq_categorias_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS productos (
    id_producto INT NOT NULL AUTO_INCREMENT,
    id_categoria INT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500) NULL,
    tipo ENUM('PLATILLO', 'BEBIDA') NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    disponible TINYINT(1) NOT NULL DEFAULT 1,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id_producto),
    CONSTRAINT uq_productos_categoria_nombre UNIQUE (id_categoria, nombre),
    CONSTRAINT ck_productos_precio CHECK (precio > 0),
    CONSTRAINT fk_productos_categoria FOREIGN KEY (id_categoria)
        REFERENCES categorias (id_categoria)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_productos_menu (activo, disponible, tipo, id_categoria),
    INDEX idx_productos_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Datos iniciales idempotentes: pueden ejecutarse de nuevo sin duplicar productos.
INSERT INTO categorias (nombre, descripcion) VALUES
    ('Antojitos', 'Sabores tradicionales para compartir o abrir el apetito.'),
    ('Platos fuertes', 'Platillos mexicanos preparados al momento.'),
    ('Bebidas', 'Aguas frescas y bebidas para acompañar.')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO productos (id_categoria, nombre, descripcion, tipo, precio, disponible, activo)
SELECT c.id_categoria, 'Tacos al pastor', 'Tres tacos con piña, cebolla, cilantro y salsa de la casa.', 'PLATILLO', 4.50, 1, 1
FROM categorias c WHERE c.nombre = 'Antojitos'
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO productos (id_categoria, nombre, descripcion, tipo, precio, disponible, activo)
SELECT c.id_categoria, 'Quesadillas de maíz', 'Queso fundido en tortilla de maíz, con curtido y salsa.', 'PLATILLO', 3.75, 1, 1
FROM categorias c WHERE c.nombre = 'Antojitos'
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO productos (id_categoria, nombre, descripcion, tipo, precio, disponible, activo)
SELECT c.id_categoria, 'Enchiladas verdes', 'Tortillas rellenas, salsa verde, crema y queso fresco.', 'PLATILLO', 6.25, 1, 1
FROM categorias c WHERE c.nombre = 'Platos fuertes'
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO productos (id_categoria, nombre, descripcion, tipo, precio, disponible, activo)
SELECT c.id_categoria, 'Tamal de elote', 'Tamal suave de elote servido calientito.', 'PLATILLO', 2.25, 1, 1
FROM categorias c WHERE c.nombre = 'Platos fuertes'
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO productos (id_categoria, nombre, descripcion, tipo, precio, disponible, activo)
SELECT c.id_categoria, 'Horchata', 'Bebida fría de arroz, canela y vainilla.', 'BEBIDA', 1.75, 1, 1
FROM categorias c WHERE c.nombre = 'Bebidas'
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO productos (id_categoria, nombre, descripcion, tipo, precio, disponible, activo)
SELECT c.id_categoria, 'Agua de jamaica', 'Infusión de flor de jamaica servida bien fría.', 'BEBIDA', 1.50, 1, 1
FROM categorias c WHERE c.nombre = 'Bebidas'
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);
