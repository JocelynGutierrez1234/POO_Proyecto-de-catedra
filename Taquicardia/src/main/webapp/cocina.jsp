<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Taquicardia - Monitor de Cocina</title>
    <link rel="stylesheet" href="https://jsdelivr.net">
</head>
<body class="bg-dark text-light">

<div class="container mt-5">
    <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-3">
        <h1 class="text-warning fw-bold">🧑‍🍳 Monitor de Pedidos en Cocina</h1>
        <a href="PedidoServlet?accion=listarMesas" class="btn btn-sm btn-outline-warning">Volver a Mesas 🍽️</a>
    </div>

    <div class="table-responsive">
        <table class="table table-dark table-striped table-hover align-middle border border-secondary shadow">
            <thead>
                <tr class="text-warning fs-5">
                    <th>Pedido #</th>
                    <th>N° Mesa</th>
                    <th>Hora de Orden</th>
                    <th>Estado de Preparación</th>
                    <th class="text-center">Acción en Cocina</th>
                </tr>
            </thead>
            <tbody>
          
                <c:forEach var="pedido" items="${pedidosCocina}">
                    <tr>
                        <td class="fw-bold text-info"># ${pedido.idPedido}</td>
                        <td>Mesa ${pedido.idMesa}</td>
                        <td class="text-muted">${pedido.fechaHora}</td>
                        <td>
                            <span class="badge ${pedido.estado == 'PENDIENTE' ? 'bg-danger' : 'bg-warning text-dark'} p-2 fs-6">
                                ${pedido.estado}
                            </span>
                        </td>
                        <td class="text-center">
                            <form action="PedidoServlet" method="POST">
                                <input type="hidden" name="idPedido" value="${pedido.idPedido}">
                                <input type="hidden" name="accion" value="actualizarPedido">
                                
                                <c:if test="${pedido.estado == 'PENDIENTE'}">
                                    <input type="hidden" name="estado" value="EN_PREPARACION">
                                    <button type="submit" class="btn btn-warning fw-bold btn-sm shadow-sm">Empezar Preparación 🌯</button>
                                </c:if>
                                <c:if test="${pedido.estado == 'EN_PREPARACION'}">
                                    <input type="hidden" name="estado" value="LISTO">
                                    <button type="submit" class="btn btn-success fw-bold btn-sm shadow-sm">¡Orden Lista! ✔️</button>
                                </c:if>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty pedidosCocina}">
                    <tr>
                        <td colspan="5" class="text-center py-4 text-muted fs-5">
                            🎉 ¡Felicidades! No hay pedidos pendientes en la taquería.
                        </td>
                    </tr>
                </c:if>
            </tbody>
        </table>
    </div>
</div>

</body>
</html>
