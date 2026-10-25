package com.taquicardia.controlador;

import com.taquicardia.dao.CategoriaDAO;
import com.taquicardia.dao.ProductoDAO;
import com.taquicardia.modelo.Categoria;
import com.taquicardia.modelo.Producto;
import com.taquicardia.servicio.ValidadorMenu;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** Controlador HTTP del catálogo; el SQL permanece en los DAO. */
@WebServlet(name = "MenuServlet", urlPatterns = {"/MenuServlet"})
public class MenuServlet extends HttpServlet {
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String accion = request.getParameter("accion");
        if (accion == null || accion.isBlank()) accion = "listar";
        try {
            switch (accion) {
                case "nuevo":
                    prepararFormulario(request, new Producto(), "Crear producto");
                    request.getRequestDispatcher("producto-form.jsp").forward(request, response);
                    break;
                case "editar":
                    Integer id = enteroPositivo(request.getParameter("id"));
                    Producto existente = id == null ? null : productoDAO.buscarPorId(id);
                    if (existente == null) {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Producto no encontrado.");
                        return;
                    }
                    prepararFormulario(request, existente, "Editar producto");
                    request.getRequestDispatcher("producto-form.jsp").forward(request, response);
                    break;
                case "listar":
                default:
                    listar(request, response);
                    break;
            }
        } catch (SQLException e) {
            getServletContext().log("Error en el módulo de menú", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "No se pudo consultar el menú. Verifica la conexión y ejecuta sql/menu.sql.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String accion = request.getParameter("accion");
        try {
            if ("guardar".equals(accion)) guardar(request, response);
            else if ("eliminar".equals(accion)) eliminar(request, response);
            else if ("disponibilidad".equals(accion)) cambiarDisponibilidad(request, response);
            else response.sendRedirect("MenuServlet");
        } catch (SQLException e) {
            getServletContext().log("Error al actualizar el catálogo", e);
            flash(request, "error", "No se pudo guardar el cambio. Comprueba la base de datos y que el nombre no esté repetido.");
            response.sendRedirect("MenuServlet");
        }
    }

    private void listar(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        String busqueda = limitar(request.getParameter("q"), 100);
        String tipo = request.getParameter("tipo");
        String disponibilidad = request.getParameter("disponible");
        String estado = request.getParameter("estado");
        if (!"PLATILLO".equals(tipo) && !"BEBIDA".equals(tipo)) tipo = "";
        if (!"si".equals(disponibilidad) && !"no".equals(disponibilidad)) disponibilidad = "";
        if (!"activos".equals(estado) && !"inactivos".equals(estado) && !"todos".equals(estado)) estado = "activos";
        Integer idCategoria = enteroPositivo(request.getParameter("categoria"));

        request.setAttribute("productos", productoDAO.listar(busqueda, tipo, idCategoria, disponibilidad, estado));
        request.setAttribute("categorias", categoriaDAO.listar());
        request.setAttribute("filtroQ", busqueda == null ? "" : busqueda);
        request.setAttribute("filtroTipo", tipo);
        request.setAttribute("filtroDisponible", disponibilidad);
        request.setAttribute("filtroEstado", estado);
        request.setAttribute("filtroCategoria", idCategoria == null ? 0 : idCategoria);
        tomarFlash(request);
        request.getRequestDispatcher("menu.jsp").forward(request, response);
    }

    private void guardar(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        String idRecibido = request.getParameter("idProducto");
        if (idRecibido != null && !idRecibido.isBlank() && !"0".equals(idRecibido.trim())
                && enteroPositivo(idRecibido) == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Identificador de producto inválido.");
            return;
        }
        Producto producto = leerProducto(request);
        Map<String, String> errores = ValidadorMenu.validarProducto(producto);
        if (errores.isEmpty() && categoriaDAO.buscarPorId(producto.getIdCategoria()) == null) {
            errores.put("idCategoria", "La categoría seleccionada ya no existe.");
        }
        if (!errores.isEmpty()) {
            prepararFormulario(request, producto, producto.getIdProducto() > 0 ? "Editar producto" : "Crear producto");
            request.setAttribute("errores", errores);
            request.getRequestDispatcher("producto-form.jsp").forward(request, response);
            return;
        }

        if (producto.getIdProducto() > 0) {
            if (!productoDAO.actualizar(producto)) {
                flash(request, "error", "El producto ya no existe o no pudo actualizarse.");
            } else {
                flash(request, "exito", "Producto actualizado correctamente.");
            }
        } else {
            productoDAO.insertar(producto);
            flash(request, "exito", "Producto registrado correctamente.");
        }
        response.sendRedirect("MenuServlet");
    }

    private Producto leerProducto(HttpServletRequest request) {
        Producto producto = new Producto();
        Integer id = enteroPositivo(request.getParameter("idProducto"));
        producto.setIdProducto(id == null ? 0 : id);
        Integer idCategoria = enteroPositivo(request.getParameter("idCategoria"));
        producto.setIdCategoria(idCategoria == null ? 0 : idCategoria);
        producto.setNombre(request.getParameter("nombre") == null ? "" : request.getParameter("nombre").trim());
        producto.setDescripcion(request.getParameter("descripcion"));
        producto.setTipo(request.getParameter("tipo"));
        producto.setPrecio(leerPrecio(request.getParameter("precio")));
        producto.setDisponible("1".equals(request.getParameter("disponible")));
        producto.setActivo("1".equals(request.getParameter("activo")));
        return producto;
    }

    private void eliminar(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        Integer id = enteroPositivo(request.getParameter("idProducto"));
        if (id == null) flash(request, "error", "No se recibió un identificador de producto válido.");
        else {
            ProductoDAO.ResultadoEliminacion resultado = productoDAO.eliminar(id);
            switch (resultado) {
                case ELIMINADO: flash(request, "exito", "Producto eliminado."); break;
                case ARCHIVADO: flash(request, "aviso", "El producto se archivó porque aparece en pedidos; se conserva el historial de facturas."); break;
                default: flash(request, "error", "El producto ya no existe.");
            }
        }
        response.sendRedirect("MenuServlet");
    }

    private void cambiarDisponibilidad(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        Integer id = enteroPositivo(request.getParameter("idProducto"));
        String valor = request.getParameter("disponible");
        if (id == null || (!"0".equals(valor) && !"1".equals(valor))) {
            flash(request, "error", "No se recibió un cambio de disponibilidad válido.");
        } else if (productoDAO.cambiarDisponibilidad(id, "1".equals(valor))) {
            flash(request, "exito", "Disponibilidad actualizada.");
        } else {
            flash(request, "error", "No se pudo cambiar la disponibilidad de ese producto.");
        }
        response.sendRedirect("MenuServlet");
    }

    private void prepararFormulario(HttpServletRequest request, Producto producto, String titulo)
            throws SQLException {
        request.setAttribute("producto", producto);
        request.setAttribute("categorias", categoriaDAO.listar());
        request.setAttribute("tituloFormulario", titulo);
    }

    private void flash(HttpServletRequest request, String tipo, String mensaje) {
        request.getSession().setAttribute("menuFlashTipo", tipo);
        request.getSession().setAttribute("menuFlash", mensaje);
    }

    private void tomarFlash(HttpServletRequest request) {
        var sesion = request.getSession(false);
        if (sesion == null) return;
        Object mensaje = sesion.getAttribute("menuFlash");
        Object tipo = sesion.getAttribute("menuFlashTipo");
        if (mensaje != null) request.setAttribute("mensaje", mensaje);
        if (tipo != null) request.setAttribute("tipoMensaje", tipo);
        sesion.removeAttribute("menuFlash");
        sesion.removeAttribute("menuFlashTipo");
    }

    private static BigDecimal leerPrecio(String valor) {
        try { return new BigDecimal(valor == null ? "" : valor.trim().replace(',', '.')); }
        catch (NumberFormatException e) { return null; }
    }

    private static Integer enteroPositivo(String valor) {
        try {
            int numero = Integer.parseInt(valor == null ? "" : valor.trim());
            return numero > 0 ? numero : null;
        } catch (NumberFormatException e) { return null; }
    }

    private static String limitar(String texto, int maximo) {
        if (texto == null) return null;
        texto = texto.trim();
        return texto.length() > maximo ? texto.substring(0, maximo) : texto;
    }
}
