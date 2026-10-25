<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>
<fmt:setLocale value="en_US"/>
<c:set var="seccion" value="ventas"/>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Taquicardia - Ventas del día</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="<c:url value='/assets/css/taquicardia.css'/>">
    <style>@media print { .no-print { display: none !important; } }</style>
</head>
<body class="bg-light">
<%@ include file="/WEB-INF/jspf/navCaja.jspf" %>

<div class="container my-4">
    <div class="d-flex flex-wrap justify-content-between align-items-end mb-4 gap-2">
        <div>
            <h1 class="fw-bold text-danger mb-0">📊 Cierre de caja</h1>
            <p class="text-muted mb-0">Ventas facturadas el día ${fecha}</p>
        </div>
        <form class="d-flex gap-2 no-print" method="GET" action="CajaServlet">
            <input type="hidden" name="accion" value="ventas">
            <input type="date" name="fecha" value="${fecha}" class="form-control">
            <button class="btn btn-outline-dark">Ver</button>
            <button type="button" class="btn btn-dark" onclick="window.print()">🖨️</button>
        </form>
    </div>

    <div class="row g-3 mb-4">
        <div class="col-6 col-md-3">
            <div class="card shadow-sm h-100"><div class="card-body">
                <div class="text-muted small">Total vendido</div>
                <div class="fs-3 fw-bold text-danger">$<fmt:formatNumber value="${resumen.totalVendido}" pattern="#,##0.00"/></div>
                <div class="small text-muted">${resumen.cantidadFacturas} factura(s)</div>
            </div></div>
        </div>
        <div class="col-6 col-md-3">
            <div class="card shadow-sm h-100"><div class="card-body">
                <div class="text-muted small">💵 Efectivo</div>
                <div class="fs-4 fw-bold">$<fmt:formatNumber value="${resumen.totalEfectivo}" pattern="#,##0.00"/></div>
            </div></div>
        </div>
        <div class="col-6 col-md-3">
            <div class="card shadow-sm h-100"><div class="card-body">
                <div class="text-muted small">💳 Tarjeta</div>
                <div class="fs-4 fw-bold">$<fmt:formatNumber value="${resumen.totalTarjeta}" pattern="#,##0.00"/></div>
            </div></div>
        </div>
        <div class="col-6 col-md-3">
            <div class="card shadow-sm h-100"><div class="card-body">
                <div class="text-muted small">IVA / Propinas</div>
                <div class="fs-6 fw-bold">IVA $<fmt:formatNumber value="${resumen.totalIva}" pattern="#,##0.00"/></div>
                <div class="fs-6 fw-bold">Propinas $<fmt:formatNumber value="${resumen.totalPropinas}" pattern="#,##0.00"/></div>
            </div></div>
        </div>
    </div>

    <div class="table-responsive">
        <table class="table table-hover align-middle bg-white shadow-sm border">
            <thead class="table-dark">
                <tr>
                    <th>Factura</th>
                    <th>Hora</th>
                    <th>Mesa</th>
                    <th>Cliente</th>
                    <th>Pago</th>
                    <th class="text-end">Subtotal</th>
                    <th class="text-end">IVA</th>
                    <th class="text-end">Propina</th>
                    <th class="text-end">Total</th>
                    <th class="no-print"></th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="f" items="${facturas}">
                    <tr>
                        <td class="fw-bold">${f.numeroFactura}</td>
                        <td><fmt:formatDate value="${f.fechaHora}" pattern="HH:mm"/></td>
                        <td>${f.numeroMesa}</td>
                        <td><c:out value="${f.nombreCliente}"/></td>
                        <td><span class="badge ${f.pago.metodoPago == 'TARJETA' ? 'bg-primary' : 'bg-success'}">${f.pago.metodoPago}</span></td>
                        <td class="text-end">$<fmt:formatNumber value="${f.subtotal}" pattern="#,##0.00"/></td>
                        <td class="text-end">$<fmt:formatNumber value="${f.iva}" pattern="#,##0.00"/></td>
                        <td class="text-end">$<fmt:formatNumber value="${f.propina}" pattern="#,##0.00"/></td>
                        <td class="text-end fw-bold">$<fmt:formatNumber value="${f.total}" pattern="#,##0.00"/></td>
                        <td class="no-print"><a class="btn btn-sm btn-outline-secondary" href="CajaServlet?accion=verFactura&idFactura=${f.idFactura}">Ver</a></td>
                    </tr>
                </c:forEach>
                <c:if test="${empty facturas}">
                    <tr><td colspan="10" class="text-center text-muted py-5">No hay facturas registradas en esta fecha.</td></tr>
                </c:if>
            </tbody>
        </table>
    </div>
</div>
</body>
</html>
