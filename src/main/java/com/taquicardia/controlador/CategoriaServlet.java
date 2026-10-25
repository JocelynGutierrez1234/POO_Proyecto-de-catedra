package com.taquicardia.controlador;

import com.taquicardia.dao.CategoriaDAO;
import com.taquicardia.modelo.Categoria;
import com.taquicardia.servicio.ValidadorMenu;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/** Controlador HTTP de categorías del menú. */
@WebServlet(name = "CategoriaServlet", urlPatterns = {"/CategoriaServlet"})
public class CategoriaServlet extends HttpServlet {
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String accion = request.getParameter("accion");
        if (accion == null || accion.isBlank()) accion = "listar";
        try {
            if ("nuevo".equals(accion)) {
                prepararFormulario(request, new Categoria(), "Nueva categoría");
                request.getRequestDispatcher("categoria-form.jsp").forward(request, response);
            } else if ("editar".equals(accion)) {
                Integer id = enteroPositivo(request.getParameter("id"));
                Categoria categoria = id == null ? null : categoriaDAO.buscarPorId(id);
                if (categoria == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Categoría no encontrada.");
                    return;
                }
                prepararFormulario(request, categoria, "Editar categoría");
                request.getRequestDispatcher("categoria-form.jsp").forward(request, response);
            } else {
                request.setAttribute("categorias", categoriaDAO.listar());
                tomarFlash(request);
                request.getRequestDispatcher("categorias.jsp").forward(request, response);
            }
        } catch (SQLException e) {
            getServletContext().log("Error en el módulo de categorías", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "No se pudieron cargar las categorías. Verifica la conexión y ejecuta sql/menu.sql.");
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
            else response.sendRedirect("CategoriaServlet");
        } catch (SQLException e) {
            getServletContext().log("Error al modificar categorías", e);
            flash(request, "error", "No se pudo guardar. Verifica si ya existe una categoría con ese nombre.");
            response.sendRedirect("CategoriaServlet");
        }
    }

    private void guardar(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        String idRecibido = request.getParameter("idCategoria");
        if (idRecibido != null && !idRecibido.isBlank() && !"0".equals(idRecibido.trim())
                && enteroPositivo(idRecibido) == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Identificador de categoría inválido.");
            return;
        }
        Categoria categoria = new Categoria();
        Integer id = enteroPositivo(request.getParameter("idCategoria"));
        categoria.setIdCategoria(id == null ? 0 : id);
        categoria.setNombre(request.getParameter("nombre") == null ? "" : request.getParameter("nombre").trim());
        categoria.setDescripcion(request.getParameter("descripcion"));
        Map<String, String> errores = ValidadorMenu.validarCategoria(categoria);
        if (!errores.isEmpty()) {
            prepararFormulario(request, categoria, categoria.getIdCategoria() > 0 ? "Editar categoría" : "Nueva categoría");
            request.setAttribute("errores", errores);
            request.getRequestDispatcher("categoria-form.jsp").forward(request, response);
            return;
        }

        if (categoria.getIdCategoria() > 0) {
            if (!categoriaDAO.actualizar(categoria)) flash(request, "error", "La categoría ya no existe.");
            else flash(request, "exito", "Categoría actualizada correctamente.");
        } else {
            categoriaDAO.insertar(categoria);
            flash(request, "exito", "Categoría registrada correctamente.");
        }
        response.sendRedirect("CategoriaServlet");
    }

    private void eliminar(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        Integer id = enteroPositivo(request.getParameter("idCategoria"));
        if (id == null) flash(request, "error", "No se recibió una categoría válida.");
        else if (categoriaDAO.eliminar(id)) flash(request, "exito", "Categoría eliminada.");
        else flash(request, "aviso", "No se puede eliminar: la categoría todavía tiene productos asociados.");
        response.sendRedirect("CategoriaServlet");
    }

    private void prepararFormulario(HttpServletRequest request, Categoria categoria, String titulo) {
        request.setAttribute("categoria", categoria);
        request.setAttribute("tituloFormulario", titulo);
    }

    private void flash(HttpServletRequest request, String tipo, String mensaje) {
        request.getSession().setAttribute("categoriaFlashTipo", tipo);
        request.getSession().setAttribute("categoriaFlash", mensaje);
    }

    private void tomarFlash(HttpServletRequest request) {
        var sesion = request.getSession(false);
        if (sesion == null) return;
        Object mensaje = sesion.getAttribute("categoriaFlash");
        Object tipo = sesion.getAttribute("categoriaFlashTipo");
        if (mensaje != null) request.setAttribute("mensaje", mensaje);
        if (tipo != null) request.setAttribute("tipoMensaje", tipo);
        sesion.removeAttribute("categoriaFlash");
        sesion.removeAttribute("categoriaFlashTipo");
    }

    private static Integer enteroPositivo(String valor) {
        try {
            int numero = Integer.parseInt(valor == null ? "" : valor.trim());
            return numero > 0 ? numero : null;
        } catch (NumberFormatException e) { return null; }
    }
}
