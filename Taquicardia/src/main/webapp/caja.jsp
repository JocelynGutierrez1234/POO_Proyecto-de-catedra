<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>
<fmt:setLocale value="en_US"/>
<c:set var="seccion" value="caja"/>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Taquicardia - Caja</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
</head>
<body class="bg-light">
<%@ include file="/WEB-INF/jspf/navCaja.jspf" %>

<div class="container my-4">
    <div class="d-flex flex-wrap justify-content-between align-items-center mb-4 gap-2">
        <div>
            <h1 class="fw-bold text-danger mb-0">💵 Caja</h1>
            <p class="text-muted mb-0">Pedidos que cocina marcó como listos y están pendientes de pago</p>
        </div>
        <a href="CajaServlet" class="btn btn-outline-secondary btn-sm">↻ Actualizar</a>
    </div>

    <c:if test="${not empty mensajeError}">
        <div class="alert alert-danger"><c:out value="${mensajeError}"/></div>
    </c:if>

    <div class="table-responsive">
        <table class="table table-hover align-middle bg-white shadow-sm border">
            <thead class="table-dark">
                <tr>
                    <th>Pedido #</th>
                    <th>Mesa</th>
                    <th>Hora</th>
                    <th>Estado</th>
                    <th class="text-center">Artículos</th>
                    <th class="text-end">Subtotal</th>
                    <th class="text-center">Acción</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="p" items="${pedidos}">
                    <tr>
                        <td class="fw-bold"># ${p.idPedido}</td>
                        <td>Mesa ${p.numeroMesa != 0 ? p.numeroMesa : p.idMesa}</td>
                        <td class="text-muted"><fmt:formatDate value="${p.fechaHora}" pattern="dd/MM/yyyy HH:mm"/></td>
                        <td>
                            <span class="badge ${p.estado == 'LISTO' ? 'bg-success' : 'bg-secondary'}">${p.estado}</span>
                        </td>
                        <td class="text-center">${p.totalArticulos}</td>
                        <td class="text-end fw-semibold">$<fmt:formatNumber value="${p.subtotal}" pattern="#,##0.00"/></td>
                        <td class="text-center">
                            <a href="CajaServlet?accion=cobrar&idPedido=${p.idPedido}" class="btn btn-danger btn-sm fw-bold">Cobrar</a>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty pedidos}">
                    <tr>
                        <td colspan="7" class="text-center py-5 text-muted">
                            No hay pedidos pendientes de cobro. Cuando cocina marque una orden como <strong>LISTO</strong>, aparecerá aquí.
                        </td>
                    </tr>
                </c:if>
            </tbody>
        </table>
    </div>
</div>
</body>
</html>
