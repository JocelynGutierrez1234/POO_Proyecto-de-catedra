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
    <title>Taquicardia - Historial de ventas</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <style>@media print { .no-print { display: none !important; } }</style>
</head>
<body class="bg-light">
<%@ include file="/WEB-INF/jspf/navCaja.jspf" %>

<div class="container my-4">
    <div class="mb-3">
        <h1 class="fw-bold text-danger mb-0">📊 Historial de ventas</h1>
        <p class="text-muted mb-0">
            <c:choose>
                <c:when test="${not empty desde and desde == hasta}">Comprobantes del día <c:out value="${desde}"/></c:when>
                <c:when test="${not empty desde or not empty hasta}">Comprobantes del <c:out value="${empty desde ? 'inicio' : desde}"/> al <c:out value="${empty hasta ? 'día de hoy' : hasta}"/></c:when>
                <c:otherwise>Comprobantes de todas las fechas</c:otherwise>
            </c:choose>
            <c:if test="${not empty numero}"> · número contiene "<c:out value="${numero}"/>"</c:if>
            <c:if test="${not empty estado}"> · estado <c:out value="${estado}"/></c:if>
        </p>
    </div>

    <form class="card card-body shadow-sm mb-4 no-print" method="GET" action="CajaServlet" id="formFiltros" novalidate>
        <input type="hidden" name="accion" value="ventas">
        <div class="row g-2 align-items-end">
            <div class="col-6 col-md-2">
                <label for="desde" class="form-label small fw-semibold mb-1">Desde</label>
                <input type="date" id="desde" name="desde" value="<c:out value='${desde}'/>" max="${hoy}" class="form-control">
            </div>
            <div class="col-6 col-md-2">
                <label for="hasta" class="form-label small fw-semibold mb-1">Hasta</label>
                <input type="date" id="hasta" name="hasta" value="<c:out value='${hasta}'/>" max="${hoy}" class="form-control">
            </div>
            <div class="col-6 col-md-3">
                <label for="numero" class="form-label small fw-semibold mb-1">N.º de comprobante</label>
                <input type="text" id="numero" name="numero" value="<c:out value='${numero}'/>" maxlength="20"
                       pattern="[0-9A-Za-z\-]{1,20}" placeholder="FAC-000012 o 12" class="form-control">
            </div>
            <div class="col-6 col-md-2">
                <label for="estado" class="form-label small fw-semibold mb-1">Estado</label>
                <select id="estado" name="estado" class="form-select">
                    <option value="">Todos</option>
                    <option value="VIGENTE" ${estado == 'VIGENTE' ? 'selected' : ''}>Vigentes</option>
                    <option value="ANULADA" ${estado == 'ANULADA' ? 'selected' : ''}>Anulados</option>
                </select>
            </div>
            <div class="col-12 col-md-3 d-flex gap-2">
                <button class="btn btn-danger flex-fill">Buscar</button>
                <a class="btn btn-outline-secondary" href="CajaServlet?accion=ventas" title="Ventas de hoy">Hoy</a>
                <button type="button" class="btn btn-dark" onclick="window.print()" title="Imprimir">🖨️</button>
            </div>
        </div>
        <div class="form-text">Deja las fechas vacías para buscar en todas las fechas.</div>
        <div class="text-danger small mt-1" id="errorFiltros"></div>
    </form>

    <%@ include file="/WEB-INF/jspf/mensajesCaja.jspf" %>

    <div class="row g-3 mb-4">
        <div class="col-6 col-md-3">
            <div class="card shadow-sm h-100"><div class="card-body">
                <div class="text-muted small">Total vendido</div>
                <div class="fs-3 fw-bold text-danger">$<fmt:formatNumber value="${resumen.totalVendido}" pattern="#,##0.00"/></div>
                <div class="small text-muted">${resumen.cantidadFacturas} comprobante(s)
                    <c:if test="${resumen.cantidadAnuladas > 0}"> · ${resumen.cantidadAnuladas} anulado(s)</c:if></div>
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
        <table class="table table-sm table-hover align-middle bg-white shadow-sm border">
            <thead class="table-dark">
                <tr>
                    <th>Comprobante</th>
                    <th>Fecha y hora</th>
                    <th>Pedido</th>
                    <th>Mesa</th>
                    <th>Cliente</th>
                    <th>Pago</th>
                    <th>Estado</th>
                    <th class="text-end">IVA</th>
                    <th class="text-end">Propina</th>
                    <th class="text-end">Total</th>
                    <th class="no-print text-center">Acciones</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="f" items="${facturas}">
                    <tr class="${f.anulada ? 'text-decoration-line-through text-muted' : ''}">
                        <td class="fw-bold"><c:out value="${f.numeroFactura}"/></td>
                        <td><fmt:formatDate value="${f.fechaHora}" pattern="dd/MM/yyyy HH:mm"/></td>
                        <td>#${f.idPedido}</td>
                        <td>${f.numeroMesa}</td>
                        <td><c:out value="${f.nombreCliente}"/></td>
                        <td><span class="badge ${f.pago.metodoPago == 'TARJETA' ? 'bg-primary' : 'bg-success'}">${empty f.pago ? 'SIN PAGO' : f.pago.metodoPago}</span></td>
                        <td><span class="badge ${f.anulada ? 'bg-danger' : 'bg-secondary'}">${f.estado}</span></td>
                        <td class="text-end">$<fmt:formatNumber value="${f.iva}" pattern="#,##0.00"/></td>
                        <td class="text-end">$<fmt:formatNumber value="${f.propina}" pattern="#,##0.00"/></td>
                        <td class="text-end fw-bold">$<fmt:formatNumber value="${f.total}" pattern="#,##0.00"/></td>
                        <td class="no-print text-nowrap text-center">
                            <a class="btn btn-sm btn-outline-secondary" href="CajaServlet?accion=verFactura&amp;idFactura=${f.idFactura}">Ver</a>
                            <c:if test="${not f.anulada}">
                                <a class="btn btn-sm btn-outline-primary" href="CajaServlet?accion=editar&amp;idFactura=${f.idFactura}">Editar</a>
                                <c:if test="${esAdmin}">
                                    <a class="btn btn-sm btn-outline-danger" href="CajaServlet?accion=anular&amp;idFactura=${f.idFactura}">Anular</a>
                                </c:if>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty facturas}">
                    <tr><td colspan="11" class="text-center text-muted py-5">No se encontraron comprobantes con esos filtros.</td></tr>
                </c:if>
            </tbody>
        </table>
    </div>
</div>

<script>
    document.getElementById('formFiltros').addEventListener('submit', (ev) => {
        const desde = document.getElementById('desde').value;
        const hasta = document.getElementById('hasta').value;
        const numero = document.getElementById('numero');
        const error = document.getElementById('errorFiltros');
        error.textContent = '';
        if (desde && hasta && desde > hasta) {
            error.textContent = 'La fecha "Desde" no puede ser posterior a la fecha "Hasta".';
        } else if (numero.value.trim() !== '' && !numero.checkValidity()) {
            error.textContent = 'El número de comprobante solo puede tener letras, números y guiones.';
        }
        if (error.textContent) ev.preventDefault();
    });
</script>
</body>
</html>
