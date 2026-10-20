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
    <title>Anular comprobante <c:out value="${factura.numeroFactura}"/> - Taquicardia</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
</head>
<body class="bg-light">
<%@ include file="/WEB-INF/jspf/navCaja.jspf" %>

<div class="container my-4" style="max-width: 640px;">
    <a href="CajaServlet?accion=verFactura&amp;idFactura=${factura.idFactura}" class="btn btn-link px-0 mb-2">← Volver al comprobante</a>
    <h1 class="fw-bold text-danger">¿Anular el comprobante <c:out value="${factura.numeroFactura}"/>?</h1>

    <%@ include file="/WEB-INF/jspf/mensajesCaja.jspf" %>

    <div class="alert alert-warning">
        El comprobante quedará como <strong>ANULADO</strong> y no se sumará en las ventas.
        El pedido #${factura.idPedido} volverá a la caja para cobrarse de nuevo.
    </div>

    <div class="card shadow-sm mb-3">
        <div class="card-body small">
            <div class="d-flex justify-content-between"><span>Fecha</span><span><fmt:formatDate value="${factura.fechaHora}" pattern="dd/MM/yyyy HH:mm"/></span></div>
            <div class="d-flex justify-content-between"><span>Pedido / Mesa</span><span>#${factura.idPedido} · Mesa ${factura.numeroMesa}</span></div>
            <div class="d-flex justify-content-between"><span>Cliente</span><span><c:out value="${factura.nombreCliente}"/></span></div>
            <div class="d-flex justify-content-between"><span>Forma de pago</span><span>${factura.pago.metodoPago}</span></div>
            <div class="d-flex justify-content-between fs-5 fw-bold mt-1"><span>Total</span><span>$<fmt:formatNumber value="${factura.total}" pattern="#,##0.00"/></span></div>
        </div>
    </div>

    <form action="CajaServlet" method="POST" class="card shadow-sm" id="formAnular" novalidate>
        <div class="card-body">
            <input type="hidden" name="accion" value="confirmarAnular">
            <input type="hidden" name="idFactura" value="${factura.idFactura}">

            <label for="motivo" class="form-label fw-semibold">Motivo de la anulación *</label>
            <textarea class="form-control" id="motivo" name="motivo" rows="3" minlength="5" maxlength="200" required
                      placeholder="Ej. Se registró con el método de pago equivocado"><c:out value="${param.motivo}"/></textarea>
            <div class="invalid-feedback">Escribe el motivo (entre 5 y 200 caracteres).</div>

            <div class="d-flex gap-2 mt-3">
                <button type="submit" class="btn btn-danger fw-bold">Sí, anular comprobante</button>
                <a href="CajaServlet?accion=verFactura&amp;idFactura=${factura.idFactura}" class="btn btn-outline-secondary">No, cancelar</a>
            </div>
        </div>
    </form>
</div>

<script>
    document.getElementById('formAnular').addEventListener('submit', (ev) => {
        const form = ev.target;
        const motivo = document.getElementById('motivo');
        motivo.value = motivo.value.trim();
        if (!form.checkValidity()) {
            ev.preventDefault();
        }
        form.classList.add('was-validated');
    });
</script>
</body>
</html>
