package com.taquicardia.controlador;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.taquicardia.dao.CobroException;
import com.taquicardia.dao.FacturaDAO;
import com.taquicardia.modelo.Factura;
import com.taquicardia.modelo.LineaFactura;
import com.taquicardia.modelo.MetodoPago;
import com.taquicardia.modelo.Pago;
import com.taquicardia.modelo.PedidoPorCobrar;
import com.taquicardia.servicio.CalculadoraPago;

/*
 * Caja: pedidos por cobrar, cobro, comprobantes e historial de ventas.
 *
 * GET  CajaServlet                               pedidos pendientes de cobro
 * GET  ?accion=cobrar&idPedido=N                 cuenta del pedido
 * POST accion=pagar                              registrar cobro
 * GET  ?accion=verFactura&idFactura=N            comprobante
 * GET  ?accion=ventas[&desde&hasta&numero&estado] historial
 * GET  ?accion=editar&idFactura=N   / POST accion=actualizar      editar datos del cliente
 * GET  ?accion=anular&idFactura=N   / POST accion=confirmarAnular  anular (solo administrador)
 */
@WebServlet(name = "CajaServlet", urlPatterns = {"/CajaServlet"})
public class CajaServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String VISTAS = "/WEB-INF/vistas/";

    // atributos que guarda el login en la sesión
    private static final String SESION_ROL = "rol";
    private static final String SESION_ID_USUARIO = "idUsuario";
    private static final String ROL_ADMIN = "ADMINISTRADOR";
    private static final Set<String> ROLES_PERMITIDOS = Set.of("CAJERO", ROL_ADMIN);

    private static final Pattern PATRON_MONTO = Pattern.compile("\\d{1,8}(\\.\\d{1,2})?");
    private static final Pattern PATRON_DOCUMENTO = Pattern.compile("[0-9A-Za-z-]{1,20}");
    private static final Pattern PATRON_NUMERO = Pattern.compile("[0-9A-Za-z-]{1,20}");

    // mensajes que se muestran después de un redirect
    private static final String FLASH_EXITO = "caja.flash.exito";
    private static final String FLASH_AVISO = "caja.flash.aviso";
    private static final String FLASH_ERROR = "caja.flash.error";

    private static final String ERROR_BD =
            "No se pudo consultar la base de datos. Revisa que MySQL esté encendido e inténtalo de nuevo.";

    private final transient FacturaDAO facturaDAO = new FacturaDAO();
    private final transient CalculadoraPago calculadora = new CalculadoraPago();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if (!tienePermiso(request, response)) return;
        response.setHeader("Cache-Control", "no-store");
        pasarFlashARequest(request);

        String accion = request.getParameter("accion");
        if (accion == null) accion = "porCobrar";

        switch (accion) {
            case "cobrar":
                mostrarCuenta(request, response, null);
                break;
            case "verFactura":
                verFactura(request, response);
                break;
            case "ventas":
                verHistorial(request, response);
                break;
            case "editar":
                mostrarEditar(request, response, null);
                break;
            case "anular":
                if (esAdministrador(request, response)) mostrarAnular(request, response, null);
                break;
            case "porCobrar":
            default:
                mostrarPorCobrar(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if (!tienePermiso(request, response)) return;
        response.setHeader("Cache-Control", "no-store");

        String accion = request.getParameter("accion");
        if ("pagar".equals(accion)) {
            pagar(request, response);
        } else if ("actualizar".equals(accion)) {
            actualizar(request, response);
        } else if ("confirmarAnular".equals(accion)) {
            if (esAdministrador(request, response)) anular(request, response);
        } else {
            response.sendRedirect("CajaServlet");
        }
    }

    // ---------- Pedidos pendientes de cobro ----------

    private void mostrarPorCobrar(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("pedidos", facturaDAO.listarPedidosPorCobrar());
        } catch (SQLException e) {
            registrarError("listar pedidos por cobrar", e);
            request.setAttribute("pedidos", new ArrayList<PedidoPorCobrar>());
            request.setAttribute("mensajeError", ERROR_BD);
        }
        request.getRequestDispatcher(VISTAS + "caja.jsp").forward(request, response);
    }

    // ---------- Cuenta del pedido ----------

    private void mostrarCuenta(HttpServletRequest request, HttpServletResponse response, String error)
            throws ServletException, IOException {
        Integer idPedido = leerEntero(request.getParameter("idPedido"));
        if (idPedido == null) {
            request.setAttribute("mensajeError", "El número de pedido no es válido.");
            mostrarPorCobrar(request, response);
            return;
        }
        try {
            PedidoPorCobrar pedido = facturaDAO.buscarPedidoPorCobrar(idPedido);
            if (pedido == null) {
                // si ya está cobrado se muestra su comprobante
                Integer idFactura = facturaDAO.buscarIdFacturaPorPedido(idPedido);
                if (idFactura != null) {
                    flash(request, FLASH_AVISO, "El pedido #" + idPedido
                            + " ya estaba cobrado. No se registró un segundo cobro; este es su comprobante.");
                    response.sendRedirect("CajaServlet?accion=verFactura&idFactura=" + idFactura);
                    return;
                }
                request.setAttribute("mensajeError", "El pedido #" + idPedido
                        + " no existe, todavía no está LISTO o no tiene productos.");
                mostrarPorCobrar(request, response);
                return;
            }

            List<LineaFactura> lineas = facturaDAO.listarLineas(idPedido);
            Factura sinPropina = nuevaFactura(pedido, lineas, false);
            Factura conPropina = nuevaFactura(pedido, lineas, true);

            request.setAttribute("pedido", pedido);
            request.setAttribute("lineas", lineas);
            request.setAttribute("sinPropina", sinPropina);
            request.setAttribute("conPropina", conPropina);
            request.setAttribute("tasaIva", porcentaje(CalculadoraPago.TASA_IVA));
            request.setAttribute("tasaPropina", porcentaje(CalculadoraPago.TASA_PROPINA));
            if (error != null) request.setAttribute("mensajeError", error);
            request.getRequestDispatcher(VISTAS + "cobrar.jsp").forward(request, response);

        } catch (IllegalArgumentException e) {
            request.setAttribute("mensajeError", e.getMessage());
            mostrarPorCobrar(request, response);
        } catch (SQLException e) {
            registrarError("mostrar la cuenta del pedido " + idPedido, e);
            request.setAttribute("mensajeError", ERROR_BD);
            mostrarPorCobrar(request, response);
        }
    }

    private Factura nuevaFactura(PedidoPorCobrar pedido, List<LineaFactura> lineas, boolean propina) {
        Factura f = new Factura();
        f.setIdPedido(pedido.getIdPedido());
        f.setNumeroMesa(pedido.getNumeroMesa() != 0 ? pedido.getNumeroMesa() : pedido.getIdMesa());
        f.setLineas(lineas);
        calculadora.calcularTotales(f, propina);
        return f;
    }

    // ---------- Registrar cobro ----------
    // los totales se calculan aquí con los datos de la base, no se toman del formulario

    private void pagar(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer idPedido = leerEntero(request.getParameter("idPedido"));
        if (idPedido == null) {
            flash(request, FLASH_ERROR, "El número de pedido no es válido.");
            response.sendRedirect("CajaServlet");
            return;
        }

        try {
            PedidoPorCobrar pedido = facturaDAO.buscarPedidoPorCobrar(idPedido);
            if (pedido == null) {
                mostrarCuenta(request, response, null);
                return;
            }
            List<LineaFactura> lineas = facturaDAO.listarLineas(idPedido);

            MetodoPago metodo = leerMetodo(request.getParameter("metodoPago"));
            if (metodo == null) {
                mostrarCuenta(request, response, "Selecciona un método de pago válido (Efectivo o Tarjeta).");
                return;
            }

            boolean incluirPropina = request.getParameter("incluirPropina") != null;
            Factura factura = nuevaFactura(pedido, lineas, incluirPropina);

            // si la cuenta cambió mientras la pantalla estaba abierta se vuelve a mostrar
            BigDecimal subtotalMostrado = leerMontoOpcional(request.getParameter("subtotalMostrado"));
            if (subtotalMostrado == null || subtotalMostrado.compareTo(factura.getSubtotal()) != 0) {
                mostrarCuenta(request, response, "La cuenta de este pedido cambió desde que la abriste. "
                        + "Revisa el nuevo total y vuelve a confirmar el pago.");
                return;
            }

            String errorCliente = validarCliente(request);
            if (errorCliente != null) {
                mostrarCuenta(request, response, errorCliente);
                return;
            }
            String nombre = limpiar(request.getParameter("nombreCliente"));
            factura.setNombreCliente(nombre == null ? "Consumidor Final" : nombre);
            factura.setDocumentoCliente(limpiar(request.getParameter("documentoCliente")));
            factura.setIdUsuario(usuarioEnSesion(request));

            BigDecimal recibido = (metodo == MetodoPago.TARJETA)
                    ? factura.getTotal()
                    : leerMontoEfectivo(request.getParameter("montoRecibido"));
            Pago pago = new Pago();
            pago.setMetodoPago(metodo);
            pago.setMontoRecibido(recibido);
            pago.setCambio(calculadora.calcularCambio(metodo, factura.getTotal(), recibido));

            int idFactura = facturaDAO.registrarCobro(factura, pago);
            String mensaje = "Pago registrado. Comprobante " + factura.getNumeroFactura() + " generado.";
            if (metodo == MetodoPago.EFECTIVO) {
                mensaje += " Cambio a entregar: $" + pago.getCambio().toPlainString() + ".";
            }
            flash(request, FLASH_EXITO, mensaje);
            response.sendRedirect("CajaServlet?accion=verFactura&idFactura=" + idFactura);

        } catch (IllegalArgumentException e) {
            mostrarCuenta(request, response, e.getMessage());
        } catch (CobroException e) {
            if (e.getIdFacturaExistente() != null) {
                flash(request, FLASH_AVISO, e.getMessage() + " No se registró un segundo cobro; este es su comprobante.");
                response.sendRedirect("CajaServlet?accion=verFactura&idFactura=" + e.getIdFacturaExistente());
            } else {
                mostrarCuenta(request, response, e.getMessage());
            }
        } catch (SQLException e) {
            registrarError("registrar el cobro del pedido " + idPedido, e);
            mostrarCuenta(request, response, "No se pudo registrar el pago por un error de base de datos. "
                    + "No se guardó ningún cambio; inténtalo de nuevo.");
        }
    }

    // ---------- Ver comprobante ----------

    private void verFactura(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Factura factura = cargarFactura(request, response);
        if (factura == null) return;
        request.setAttribute("factura", factura);
        request.setAttribute("esAdmin", ROL_ADMIN.equals(rolEnSesion(request)));
        request.getRequestDispatcher(VISTAS + "factura.jsp").forward(request, response);
    }

    // devuelve null (y ya respondió) si no existe o falla la base
    private Factura cargarFactura(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer idFactura = leerEntero(request.getParameter("idFactura"));
        try {
            Factura factura = (idFactura == null) ? null : facturaDAO.obtenerFactura(idFactura);
            if (factura == null) {
                flash(request, FLASH_ERROR, "El comprobante solicitado no existe.");
                response.sendRedirect("CajaServlet?accion=ventas");
            }
            return factura;
        } catch (SQLException e) {
            registrarError("obtener el comprobante " + idFactura, e);
            request.setAttribute("mensajeError", ERROR_BD);
            mostrarPorCobrar(request, response);
            return null;
        }
    }

    // ---------- Editar datos del cliente ----------

    private void mostrarEditar(HttpServletRequest request, HttpServletResponse response, String error)
            throws ServletException, IOException {
        Factura factura = cargarFactura(request, response);
        if (factura == null) return;
        if (factura.isAnulada()) {
            flash(request, FLASH_ERROR, "Un comprobante anulado no se puede editar.");
            response.sendRedirect("CajaServlet?accion=verFactura&idFactura=" + factura.getIdFactura());
            return;
        }
        request.setAttribute("factura", factura);
        if (error != null) request.setAttribute("mensajeError", error);
        request.getRequestDispatcher(VISTAS + "editarFactura.jsp").forward(request, response);
    }

    private void actualizar(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer idFactura = leerEntero(request.getParameter("idFactura"));
        if (idFactura == null) {
            flash(request, FLASH_ERROR, "El comprobante solicitado no existe.");
            response.sendRedirect("CajaServlet?accion=ventas");
            return;
        }
        String error = validarCliente(request);
        String nombre = limpiar(request.getParameter("nombreCliente"));
        if (error == null && nombre == null) error = "El nombre del cliente es obligatorio.";
        if (error != null) {
            mostrarEditar(request, response, error);
            return;
        }
        try {
            facturaDAO.actualizarCliente(idFactura, nombre, limpiar(request.getParameter("documentoCliente")));
            flash(request, FLASH_EXITO, "Datos del comprobante actualizados con éxito.");
            response.sendRedirect("CajaServlet?accion=verFactura&idFactura=" + idFactura);
        } catch (CobroException e) {
            flash(request, FLASH_ERROR, e.getMessage());
            response.sendRedirect("CajaServlet?accion=verFactura&idFactura=" + idFactura);
        } catch (SQLException e) {
            registrarError("actualizar el comprobante " + idFactura, e);
            mostrarEditar(request, response, "No se pudo guardar el cambio. Inténtalo de nuevo.");
        }
    }

    // ---------- Anular comprobante ----------

    private void mostrarAnular(HttpServletRequest request, HttpServletResponse response, String error)
            throws ServletException, IOException {
        Factura factura = cargarFactura(request, response);
        if (factura == null) return;
        if (factura.isAnulada()) {
            flash(request, FLASH_AVISO, "El comprobante ya estaba anulado.");
            response.sendRedirect("CajaServlet?accion=verFactura&idFactura=" + factura.getIdFactura());
            return;
        }
        request.setAttribute("factura", factura);
        if (error != null) request.setAttribute("mensajeError", error);
        request.getRequestDispatcher(VISTAS + "anularFactura.jsp").forward(request, response);
    }

    private void anular(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer idFactura = leerEntero(request.getParameter("idFactura"));
        if (idFactura == null) {
            flash(request, FLASH_ERROR, "El comprobante solicitado no existe.");
            response.sendRedirect("CajaServlet?accion=ventas");
            return;
        }
        String motivo = limpiar(request.getParameter("motivo"));
        if (motivo == null || motivo.length() < 5) {
            mostrarAnular(request, response, "Escribe el motivo de la anulación (mínimo 5 caracteres).");
            return;
        }
        if (motivo.length() > 200) {
            mostrarAnular(request, response, "El motivo admite máximo 200 caracteres.");
            return;
        }
        try {
            facturaDAO.anularFactura(idFactura, motivo);
            flash(request, FLASH_EXITO, "Comprobante anulado con éxito. El pedido volvió a la caja para cobrarse de nuevo.");
            response.sendRedirect("CajaServlet?accion=verFactura&idFactura=" + idFactura);
        } catch (CobroException e) {
            flash(request, FLASH_AVISO, e.getMessage());
            response.sendRedirect("CajaServlet?accion=verFactura&idFactura=" + idFactura);
        } catch (SQLException e) {
            registrarError("anular el comprobante " + idFactura, e);
            mostrarAnular(request, response, "No se pudo anular el comprobante. Inténtalo de nuevo.");
        }
    }

    // ---------- Historial de ventas ----------

    private void verHistorial(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<String> errores = new ArrayList<>();

        String pFecha = limpiar(request.getParameter("fecha"));
        String pDesde = limpiar(request.getParameter("desde"));
        String pHasta = limpiar(request.getParameter("hasta"));
        if (pFecha != null && pDesde == null && pHasta == null) {
            pDesde = pFecha;
            pHasta = pFecha;
        }
        String numero = limpiar(request.getParameter("numero"));
        String estado = limpiar(request.getParameter("estado"));

        LocalDate desde = leerFecha(pDesde, "Desde", errores);
        LocalDate hasta = leerFecha(pHasta, "Hasta", errores);
        if (numero != null) {
            numero = numero.toUpperCase(Locale.ROOT);
            if (!PATRON_NUMERO.matcher(numero).matches()) {
                errores.add("El número de comprobante solo puede tener letras, números y guiones (ej. FAC-000012 o 12).");
            }
        }
        if (estado != null) {
            estado = estado.toUpperCase(Locale.ROOT);
            if (!Factura.VIGENTE.equals(estado) && !Factura.ANULADA.equals(estado)) {
                errores.add("El estado seleccionado no es válido.");
            }
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            errores.add("La fecha \"Desde\" no puede ser posterior a la fecha \"Hasta\".");
        }

        List<Factura> facturas = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        try {
            hoy = facturaDAO.fechaActual();
            // sin filtros: ventas de hoy
            if (errores.isEmpty() && pDesde == null && pHasta == null && numero == null && estado == null) {
                desde = hoy;
                hasta = hoy;
            }
            if (errores.isEmpty()) {
                facturas = facturaDAO.buscarFacturas(desde, hasta, numero, estado);
            }
        } catch (SQLException e) {
            registrarError("consultar el historial de ventas", e);
            errores.add(ERROR_BD);
        }
        if (!errores.isEmpty()) request.setAttribute("mensajeError", String.join(" ", errores));

        request.setAttribute("desde", desde == null ? (pDesde == null ? "" : pDesde) : desde.toString());
        request.setAttribute("hasta", hasta == null ? (pHasta == null ? "" : pHasta) : hasta.toString());
        request.setAttribute("numero", numero == null ? "" : numero);
        request.setAttribute("estado", estado == null ? "" : estado);
        request.setAttribute("hoy", hoy.toString());
        request.setAttribute("esAdmin", ROL_ADMIN.equals(rolEnSesion(request)));
        request.setAttribute("facturas", facturas);
        request.setAttribute("resumen", calculadora.resumir(facturas));
        request.getRequestDispatcher(VISTAS + "ventas.jsp").forward(request, response);
    }

    // ---------- Sesión y roles ----------

    private boolean tienePermiso(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String rol = rolEnSesion(request);
        if (rol == null) {
            String login = getServletContext().getInitParameter("paginaLogin");
            if (login != null && !login.isBlank()) {
                response.sendRedirect(request.getContextPath() + "/" + login.trim());
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                        "Debes iniciar sesión como cajero o administrador para usar la caja.");
            }
            return false;
        }
        if (ROLES_PERMITIDOS.contains(rol)) return true;
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Solo el cajero o el administrador pueden usar la caja.");
        return false;
    }

    private boolean esAdministrador(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (ROL_ADMIN.equals(rolEnSesion(request))) return true;
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Solo el administrador puede anular comprobantes.");
        return false;
    }

    private static String rolEnSesion(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        Object rol = (sesion == null) ? null : sesion.getAttribute(SESION_ROL);
        return (rol == null) ? null : rol.toString().trim().toUpperCase(Locale.ROOT);
    }

    private Integer usuarioEnSesion(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion == null) return null;
        Object id = sesion.getAttribute(SESION_ID_USUARIO);
        if (id instanceof Integer) return (Integer) id;
        return (id == null) ? null : leerEntero(id.toString());
    }

    // ---------- Ayudas ----------

    private static String validarCliente(HttpServletRequest request) {
        String nombre = limpiar(request.getParameter("nombreCliente"));
        if (nombre != null && nombre.length() > 100) {
            return "El nombre del cliente admite máximo 100 caracteres.";
        }
        String documento = limpiar(request.getParameter("documentoCliente"));
        if (documento != null && !PATRON_DOCUMENTO.matcher(documento).matches()) {
            return "El DUI/NIT solo puede tener números, letras y guiones (máximo 20 caracteres).";
        }
        return null;
    }

    private static void flash(HttpServletRequest request, String clave, String mensaje) {
        request.getSession().setAttribute(clave, mensaje);
    }

    private static void pasarFlashARequest(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion == null) return;
        moverFlash(sesion, request, FLASH_EXITO, "mensajeExito");
        moverFlash(sesion, request, FLASH_AVISO, "mensajeAviso");
        moverFlash(sesion, request, FLASH_ERROR, "mensajeError");
    }

    private static void moverFlash(HttpSession sesion, HttpServletRequest request, String clave, String atributo) {
        Object valor = sesion.getAttribute(clave);
        if (valor != null) {
            request.setAttribute(atributo, valor);
            sesion.removeAttribute(clave);
        }
    }

    private void registrarError(String accion, Exception e) {
        getServletContext().log("[Caja] Error al " + accion + ": " + e.getMessage(), e);
    }

    private static String porcentaje(BigDecimal tasa) {
        return tasa.movePointRight(2).stripTrailingZeros().toPlainString();
    }

    private static Integer leerEntero(String valor) {
        try {
            int n = Integer.parseInt(valor.trim());
            return n > 0 ? n : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static MetodoPago leerMetodo(String valor) {
        if (valor == null) return null;
        try {
            return MetodoPago.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static BigDecimal leerMontoEfectivo(String valor) {
        String v = limpiar(valor);
        if (v == null) {
            throw new IllegalArgumentException("Escribe el monto en efectivo que entregó el cliente.");
        }
        if (v.indexOf(',') >= 0 && v.indexOf('.') < 0) v = v.replace(',', '.');
        if (v.startsWith("-")) {
            throw new IllegalArgumentException("El monto recibido no puede ser negativo.");
        }
        if (!PATRON_MONTO.matcher(v).matches()) {
            throw new IllegalArgumentException("El monto recibido no es válido. Escribe solo números con máximo "
                    + "2 decimales y sin separador de miles, por ejemplo 25.50.");
        }
        return new BigDecimal(v).setScale(2);
    }

    private static BigDecimal leerMontoOpcional(String valor) {
        String v = limpiar(valor);
        if (v == null || !PATRON_MONTO.matcher(v).matches()) return null;
        return new BigDecimal(v).setScale(2);
    }

    private static LocalDate leerFecha(String valor, String campo, List<String> errores) {
        if (valor == null) return null;
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException e) {
            errores.add("La fecha \"" + campo + "\" no es válida (formato AAAA-MM-DD).");
            return null;
        }
    }

    private static String limpiar(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }
}
