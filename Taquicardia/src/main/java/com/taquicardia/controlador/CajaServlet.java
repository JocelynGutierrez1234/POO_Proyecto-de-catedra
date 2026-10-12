package com.taquicardia.controlador;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.taquicardia.dao.FacturaDAO;
import com.taquicardia.modelo.Factura;
import com.taquicardia.modelo.LineaFactura;
import com.taquicardia.modelo.MetodoPago;
import com.taquicardia.modelo.Pago;
import com.taquicardia.modelo.PedidoPorCobrar;
import com.taquicardia.modelo.ResumenVentas;
import com.taquicardia.servicio.CalculadoraPago;

@WebServlet(name = "CajaServlet", urlPatterns = {"/CajaServlet"})
public class CajaServlet extends HttpServlet {

    private final FacturaDAO facturaDAO = new FacturaDAO();
    private final CalculadoraPago calculadora = new CalculadoraPago();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if (!tienePermiso(request, response)) return;

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
                verVentas(request, response);
                break;
            case "porCobrar":
            default:
                request.setAttribute("pedidos", facturaDAO.listarPedidosPorCobrar());
                request.getRequestDispatcher("caja.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if (!tienePermiso(request, response)) return;

        if ("pagar".equals(request.getParameter("accion"))) {
            pagar(request, response);
        } else {
            response.sendRedirect("CajaServlet");
        }
    }

    // Mostrar la cuenta del pedido
    private void mostrarCuenta(HttpServletRequest request, HttpServletResponse response, String error)
            throws ServletException, IOException {
        Integer idPedido = leerEntero(request.getParameter("idPedido"));
        PedidoPorCobrar pedido = (idPedido == null) ? null : facturaDAO.buscarPedidoPorCobrar(idPedido);
        if (pedido == null) {
            request.setAttribute("mensajeError", "El pedido no existe, todavía no está LISTO o ya fue cobrado.");
            request.setAttribute("pedidos", facturaDAO.listarPedidosPorCobrar());
            request.getRequestDispatcher("caja.jsp").forward(request, response);
            return;
        }

        List<LineaFactura> lineas = facturaDAO.listarLineas(idPedido);

        // con y sin propina para el switch de la pantalla
        Factura sinPropina = nuevaFactura(pedido, lineas, false);
        Factura conPropina = nuevaFactura(pedido, lineas, true);

        request.setAttribute("pedido", pedido);
        request.setAttribute("lineas", lineas);
        request.setAttribute("sinPropina", sinPropina);
        request.setAttribute("conPropina", conPropina);
        request.setAttribute("tasaIva", CalculadoraPago.TASA_IVA.movePointRight(2).stripTrailingZeros().toPlainString());
        request.setAttribute("tasaPropina", CalculadoraPago.TASA_PROPINA.movePointRight(2).stripTrailingZeros().toPlainString());
        if (error != null) request.setAttribute("mensajeError", error);
        request.getRequestDispatcher("cobrar.jsp").forward(request, response);
    }

    private Factura nuevaFactura(PedidoPorCobrar pedido, List<LineaFactura> lineas, boolean propina) {
        Factura f = new Factura();
        f.setIdPedido(pedido.getIdPedido());
        f.setNumeroMesa(pedido.getNumeroMesa());
        f.setLineas(lineas);
        calculadora.calcularTotales(f, propina);
        return f;
    }

    // Cobrar (el total se vuelve a calcular aquí, no se toma del formulario)
    private void pagar(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer idPedido = leerEntero(request.getParameter("idPedido"));
        PedidoPorCobrar pedido = (idPedido == null) ? null : facturaDAO.buscarPedidoPorCobrar(idPedido);
        if (pedido == null) {
            mostrarCuenta(request, response, null);
            return;
        }

        List<LineaFactura> lineas = facturaDAO.listarLineas(idPedido);
        if (lineas.isEmpty()) {
            mostrarCuenta(request, response, "El pedido no tiene productos; no se puede facturar.");
            return;
        }

        MetodoPago metodo;
        try {
            metodo = MetodoPago.valueOf(request.getParameter("metodoPago"));
        } catch (Exception e) {
            mostrarCuenta(request, response, "Selecciona un método de pago válido.");
            return;
        }

        boolean incluirPropina = request.getParameter("incluirPropina") != null;
        Factura factura = nuevaFactura(pedido, lineas, incluirPropina);

        String nombre = limpiar(request.getParameter("nombreCliente"));
        factura.setNombreCliente(nombre == null ? "Consumidor Final" : recortar(nombre, 100));
        factura.setDocumentoCliente(recortar(limpiar(request.getParameter("documentoCliente")), 20));
        factura.setIdUsuario(usuarioEnSesion(request));

        BigDecimal recibido;
        if (metodo == MetodoPago.TARJETA) {
            recibido = factura.getTotal();
        } else {
            recibido = leerMonto(request.getParameter("montoRecibido"));
            if (recibido == null) {
                mostrarCuenta(request, response, "Escribe el monto en efectivo que entregó el cliente.");
                return;
            }
        }

        Pago pago = new Pago();
        pago.setMetodoPago(metodo);
        pago.setMontoRecibido(recibido);
        try {
            pago.setCambio(calculadora.calcularCambio(metodo, factura.getTotal(), recibido));
        } catch (IllegalArgumentException e) {
            mostrarCuenta(request, response, e.getMessage());
            return;
        }

        try {
            int idFactura = facturaDAO.registrarCobro(factura, pago);
            // redirect para que al recargar no se cobre otra vez
            response.sendRedirect("CajaServlet?accion=verFactura&nueva=1&idFactura=" + idFactura);
        } catch (SQLException e) {
            System.out.println("Error al registrar cobro: " + e.getMessage());
            mostrarCuenta(request, response, "No se pudo registrar el pago: " + e.getMessage());
        }
    }

    private void verFactura(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Integer idFactura = leerEntero(request.getParameter("idFactura"));
        Factura factura = (idFactura == null) ? null : facturaDAO.obtenerFactura(idFactura);
        if (factura == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Factura no encontrada");
            return;
        }
        request.setAttribute("factura", factura);
        request.setAttribute("nueva", request.getParameter("nueva") != null);
        request.getRequestDispatcher("factura.jsp").forward(request, response);
    }

    private void verVentas(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LocalDate fecha = LocalDate.now();
        String param = request.getParameter("fecha");
        if (param != null && !param.isBlank()) {
            try { fecha = LocalDate.parse(param); } catch (DateTimeParseException ignorada) { }
        }
        List<Factura> facturas = facturaDAO.listarFacturasPorFecha(fecha);
        request.setAttribute("fecha", fecha.toString());
        request.setAttribute("facturas", facturas);
        request.setAttribute("resumen", new ResumenVentas(facturas));
        request.getRequestDispatcher("ventas.jsp").forward(request, response);
    }

    // Solo cajero y admin (si todavía no hay login deja pasar)
    private boolean tienePermiso(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession sesion = request.getSession(false);
        Object rol = (sesion == null) ? null : sesion.getAttribute("rol");
        if (rol == null) return true;
        String r = rol.toString().toUpperCase();
        if (r.equals("ADMINISTRADOR") || r.equals("CAJERO")) return true;
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Solo el cajero o el administrador pueden usar la caja.");
        return false;
    }

    // id del cajero que inició sesión
    private Integer usuarioEnSesion(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion == null) return null;
        Object id = sesion.getAttribute("idUsuario");
        if (id instanceof Integer) return (Integer) id;
        return (id == null) ? null : leerEntero(id.toString());
    }

    // Métodos de ayuda
    private static Integer leerEntero(String valor) {
        try { return Integer.valueOf(valor.trim()); } catch (Exception e) { return null; }
    }

    private static BigDecimal leerMonto(String valor) {
        try {
            BigDecimal monto = new BigDecimal(valor.trim().replace(",", "."));
            return monto.signum() < 0 ? null : monto.setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (Exception e) {
            return null;
        }
    }

    private static String limpiar(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    private static String recortar(String valor, int max) {
        return (valor == null || valor.length() <= max) ? valor : valor.substring(0, max);
    }
}
