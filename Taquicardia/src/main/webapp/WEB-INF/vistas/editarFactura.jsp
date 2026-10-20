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
    <title>Editar comprobante <c:out value="${factura.numeroFactura}"/> - Taquicardia</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
</head>
<body class="bg-light">
<%@ include file="/WEB-INF/jspf/navCaja.jspf" %>

<div class="container my-4" style="max-width: 640px;">
    <a href="CajaServlet?accion=verFactura&amp;idFactura=${factura.idFactura}" class="btn btn-link px-0 mb-2">← Volver al comprobante</a>
    <h1 class="fw-bold text-danger">Editar comprobante <c:out value="${factura.numeroFactura}"/></h1>

    <%@ include file="/WEB-INF/jspf/mensajesCaja.jspf" %>

    <div class="card shadow-sm mb-3">
        <div class="card-body small">
            <div class="d-flex justify-content-between"><span>Pedido</span><span>#${factura.idPedido} · Mesa ${factura.numeroMesa}</span></div>
            <div class="d-flex justify-content-between"><span>Fecha</span><span><fmt:formatDate value="${factura.fechaHora}" pattern="dd/MM/yyyy HH:mm"/></span></div>
            <div class="d-flex justify-content-between"><span>Total</span><strong>$<fmt:formatNumber value="${factura.total}" pattern="#,##0.00"/></strong></div>
            <div class="d-flex justify-content-between"><span>Forma de pago</span><span>${factura.pago.metodoPago}</span></div>
            <p class="text-muted mb-0 mt-2">Los importes y el pago no se pueden modificar.</p>
        </div>
    </div>

    <form action="CajaServlet" method="POST" class="card shadow-sm" id="formEditar" novalidate>
        <div class="card-body">
            <input type="hidden" name="accion" value="actualizar">
            <input type="hidden" name="idFactura" value="${factura.idFactura}">

            <label for="nombreCliente" class="form-label fw-semibold">Nombre del cliente *</label>
            <input type="text" class="form-control mb-1" id="nombreCliente" name="nombreCliente" maxlength="100" required
                   value="<c:out value='${empty param.nombreCliente ? factura.nombreCliente : param.nombreCliente}'/>">
            <div class="invalid-feedback mb-2">El nombre del cliente es obligatorio.</div>

            <label for="documentoCliente" class="form-label fw-semibold mt-2">DUI / NIT</label>
            <input type="text" class="form-control mb-1" id="documentoCliente" name="documentoCliente" maxlength="20"
                   pattern="[0-9A-Za-z\-]{1,20}" placeholder="Opcional"
                   value="<c:out value='${empty param.documentoCliente ? factura.documentoCliente : param.documentoCliente}'/>">
            <div class="invalid-feedback mb-2">Solo números, letras y guiones (máximo 20).</div>

            <div class="d-flex gap-2 mt-3">
                <button type="submit" class="btn btn-danger fw-bold">Guardar cambios</button>
                <a href="CajaServlet?accion=verFactura&amp;idFactura=${factura.idFactura}" class="btn btn-outline-secondary">Cancelar</a>
            </div>
        </div>
    </form>
</div>

<script>
    document.getElementById('formEditar').addEventListener('submit', (ev) => {
        const form = ev.target;
        const nombre = document.getElementById('nombreCliente');
        nombre.value = nombre.value.trim();
        if (!form.checkValidity()) {
            ev.preventDefault();
        }
        form.classList.add('was-validated');
    });
</script>
</body>
</html>
