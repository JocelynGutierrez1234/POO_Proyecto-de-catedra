package com.taquicardia.servicio;

import com.taquicardia.modelo.Categoria;
import com.taquicardia.modelo.Producto;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValidadorMenuTest {
    @Test
    void aceptaProductoCompleto() {
        Producto producto = new Producto();
        producto.setNombre("Tacos al pastor");
        producto.setIdCategoria(1);
        producto.setTipo("PLATILLO");
        producto.setPrecio(new BigDecimal("4.50"));
        producto.setDescripcion("Tortillas, piña y cilantro.");
        assertTrue(ValidadorMenu.validarProducto(producto).isEmpty());
    }

    @Test
    void rechazaPrecioNoPositivoYTipoDesconocido() {
        Producto producto = new Producto();
        producto.setNombre("Agua fresca");
        producto.setIdCategoria(2);
        producto.setTipo("POSTRE");
        producto.setPrecio(BigDecimal.ZERO);
        assertTrue(ValidadorMenu.validarProducto(producto).containsKey("precio"));
        assertTrue(ValidadorMenu.validarProducto(producto).containsKey("tipo"));
    }

    @Test
    void exigeNombreDeCategoria() {
        Categoria categoria = new Categoria();
        assertTrue(ValidadorMenu.validarCategoria(categoria).containsKey("nombre"));
        categoria.setNombre("Bebidas");
        assertFalse(ValidadorMenu.validarCategoria(categoria).containsKey("nombre"));
    }
}
