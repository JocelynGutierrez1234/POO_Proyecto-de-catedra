package com.taquicardia.controlador;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.taquicardia.dao.MesaDAO;
import com.taquicardia.dao.PedidoDAO;
import com.taquicardia.modelo.Mesa;
import com.taquicardia.modelo.Pedido;

@WebServlet(name = "PedidoServlet", urlPatterns = {"/PedidoServlet"})
public class PedidoServlet extends HttpServlet {

    private MesaDAO mesaDAO = new MesaDAO();
    private PedidoDAO pedidoDAO = new PedidoDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String accion = request.getParameter("accion");
        
        if (accion == null) {
            accion = "listarMesas";
        }

        switch (accion) {
            case "listarMesas":
                List<Mesa> listaMesas = mesaDAO.listarMesas();
                request.setAttribute("mesas", listaMesas);
                request.getRequestDispatcher("mesas.jsp").forward(request, response);
                break;
                
            case "verCocina":
                List<Pedido> pedidosCocina = pedidoDAO.listarPedidosCocina();
                request.setAttribute("pedidosCocina", pedidosCocina);
                request.getRequestDispatcher("cocina.jsp").forward(request, response);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String accion = request.getParameter("accion");

        if ("cambiarMesa".equals(accion)) {
            int idMesa = Integer.parseInt(request.getParameter("idMesa"));
            String nuevoEstado = request.getParameter("estado");
            mesaDAO.cambiarEstadoMesa(idMesa, nuevoEstado);
            response.sendRedirect("PedidoServlet?accion=listarMesas");
            
        } else if ("actualizarPedido".equals(accion)) {
            int idPedido = Integer.parseInt(request.getParameter("idPedido"));
            String nuevoEstado = request.getParameter("estado");
            pedidoDAO.actualizarEstadoPedido(idPedido, nuevoEstado);
            response.sendRedirect("PedidoServlet?accion=verCocina");
        }
    }
}
