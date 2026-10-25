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
    <title>Factura ${factura.numeroFactura} - Taquicardia</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="<c:url value='/assets/css/taquicardia.css'/>">
    <style>
        .ticket { max-width: 420px; margin: 0 auto; font-family: "Courier New", monospace; }
        .ticket hr { border-top: 1px dashed #000; opacity: 1; }
        @media print {
            .no-print { display: none !important; }
            body { background: #fff !important; }
            .ticket { box-shadow: none !important; border: none !important; }
        }
    </style>
</head>
<body class="bg-light">
<%@ include file="/WEB-INF/jspf/navCaja.jspf" %>

<div class="container my-4">
    <c:if test="${nueva}">
        <div class="alert alert-success no-print text-center">
            ✅ Pago registrado. Factura <strong>${factura.numeroFactura}</strong> generada.
            <c:if test="${factura.pago.metodoPago == 'EFECTIVO'}">
                Cambio a entregar: <strong>$<fmt:formatNumber value="${factura.pago.cambio}" pattern="#,##0.00"/></strong>
            </c:if>
        </div>
    </c:if>

    <div class="ticket card shadow-sm p-4 bg-white">
        <div class="text-center">
            <h4 class="fw-bold mb-0">🌮 TAQUICARDIA 🌮</h4>
            <small>Restaurante Mexicano</small><br>
            <small>FACTURA - CONSUMIDOR FINAL</small>
        </div>
        <hr>
        <div class="small">
            <div class="d-flex justify-content-between"><span>Factura:</span><strong>${factura.numeroFactura}</strong></div>
            <div class="d-flex justify-content-between"><span>Fecha:</span><span><fmt:formatDate value="${factura.fechaHora}" pattern="dd/MM/yyyy HH:mm"/></span></div>
            <div class="d-flex justify-content-between"><span>Pedido:</span><span>#${factura.idPedido}</span></div>
            <div class="d-flex justify-content-between"><span>Mesa:</span><span>${factura.numeroMesa}</span></div>
            <div class="d-flex justify-content-between"><span>Cliente:</span><span><c:out value="${factura.nombreCliente}"/></span></div>
            <c:if test="${not empty factura.documentoCliente}">
                <div class="d-flex justify-content-between"><span>DUI/NIT:</span><span><c:out value="${factura.documentoCliente}"/></span></div>
            </c:if>
        </div>
        <hr>
        <table class="table table-sm table-borderless small mb-0">
            <thead>
                <tr><th>Cant</th><th>Descripción</th><th class="text-end">Importe</th></tr>
            </thead>
            <tbody>
                <c:forEach var="l" items="${factura.lineas}">
                    <tr>
                        <td>${l.cantidad}</td>
                        <td><c:out value="${l.nombreProducto}"/><br>
                            <small class="text-muted">@ $<fmt:formatNumber value="${l.precioUnitario}" pattern="#,##0.00"/></small></td>
                        <td class="text-end">$<fmt:formatNumber value="${l.importe}" pattern="#,##0.00"/></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
        <hr>
        <div class="small">
            <div class="d-flex justify-content-between"><span>Subtotal</span><span>$<fmt:formatNumber value="${factura.subtotal}" pattern="#,##0.00"/></span></div>
            <div class="d-flex justify-content-between"><span>IVA</span><span>$<fmt:formatNumber value="${factura.iva}" pattern="#,##0.00"/></span></div>
            <c:if test="${factura.propina > 0}">
                <div class="d-flex justify-content-between"><span>Propina</span><span>$<fmt:formatNumber value="${factura.propina}" pattern="#,##0.00"/></span></div>
            </c:if>
            <div class="d-flex justify-content-between fs-5 fw-bold mt-1"><span>TOTAL</span><span>$<fmt:formatNumber value="${factura.total}" pattern="#,##0.00"/></span></div>
        </div>
        <c:if test="${not empty factura.pago}">
            <hr>
            <div class="small">
                <div class="d-flex justify-content-between"><span>Forma de pago</span><span>${factura.pago.metodoPago}</span></div>
                <div class="d-flex justify-content-between"><span>Recibido</span><span>$<fmt:formatNumber value="${factura.pago.montoRecibido}" pattern="#,##0.00"/></span></div>
                <div class="d-flex justify-content-between"><span>Cambio</span><span>$<fmt:formatNumber value="${factura.pago.cambio}" pattern="#,##0.00"/></span></div>
            </div>
        </c:if>
        <hr>
        <p class="text-center small mb-0">¡Gracias por su visita! ¡Vuelva pronto, compadre! 🌶️</p>
    </div>

    <div class="text-center mt-4 no-print">
        <button onclick="window.print()" class="btn btn-dark">🖨️ Imprimir</button>
        <a href="CajaServlet" class="btn btn-danger">Volver a la caja</a>
    </div>
</div>
</body>
</html>
