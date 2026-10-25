package com.taquicardia.servicio;

import com.taquicardia.modelo.Categoria;
import com.taquicardia.modelo.Producto;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Reglas del catálogo ejecutadas en el servidor, además de las validaciones HTML. */
public final class ValidadorMenu {
    private static final Set<String> TIPOS = Set.of("PLATILLO", "BEBIDA");
    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("99999999.99");

    private ValidadorMenu() {
    }

    public static Map<String, String> validarProducto(Producto producto) {
        Map<String, String> errores = new LinkedHashMap<>();
        String nombre = limpiar(producto.getNombre());
        if (nombre.isEmpty()) errores.put("nombre", "El nombre del producto es obligatorio.");
        else if (nombre.length() > 100) errores.put("nombre", "El nombre no puede superar 100 caracteres.");

        String descripcion = producto.getDescripcion() == null ? "" : producto.getDescripcion().trim();
        if (descripcion.length() > 500) errores.put("descripcion", "La descripción no puede superar 500 caracteres.");
        if (producto.getIdCategoria() <= 0) errores.put("idCategoria", "Selecciona una categoría válida.");
        if (producto.getTipo() == null || !TIPOS.contains(producto.getTipo().trim().toUpperCase())) {
            errores.put("tipo", "Selecciona si el producto es platillo o bebida.");
        }

        BigDecimal precio = producto.getPrecio();
        if (precio == null || precio.signum() <= 0) {
            errores.put("precio", "El precio debe ser mayor que cero.");
        } else if (precio.compareTo(PRECIO_MAXIMO) > 0 || precio.scale() > 2) {
            errores.put("precio", "Usa un precio válido de hasta 8 dígitos enteros y 2 decimales.");
        }
        return errores;
    }

    public static Map<String, String> validarCategoria(Categoria categoria) {
        Map<String, String> errores = new LinkedHashMap<>();
        String nombre = limpiar(categoria.getNombre());
        if (nombre.isEmpty()) errores.put("nombre", "El nombre de la categoría es obligatorio.");
        else if (nombre.length() > 80) errores.put("nombre", "El nombre no puede superar 80 caracteres.");
        String descripcion = categoria.getDescripcion() == null ? "" : categoria.getDescripcion().trim();
        if (descripcion.length() > 250) errores.put("descripcion", "La descripción no puede superar 250 caracteres.");
        return errores;
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
