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
    <title>Taquicardia - Cobrar pedido #${pedido.idPedido}</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <style>
        .total-grande { font-size: 2rem; font-weight: 800; }
        .con-propina { display: none; }
        body.propina .con-propina { display: inline; }
        body.propina .sin-propina { display: none; }
    </style>
</head>
<body class="bg-light">
<%@ include file="/WEB-INF/jspf/navCaja.jspf" %>

<div class="container my-4">
    <a href="CajaServlet" class="btn btn-link px-0 mb-2">← Volver a la caja</a>
    <h1 class="fw-bold text-danger">Cobrar pedido #${pedido.idPedido}</h1>
    <p class="text-muted">Mesa ${pedido.numeroMesa != 0 ? pedido.numeroMesa : pedido.idMesa} ·
        <fmt:formatDate value="${pedido.fechaHora}" pattern="dd/MM/yyyy HH:mm"/></p>

    <c:if test="${not empty mensajeError}">
        <div class="alert alert-danger"><c:out value="${mensajeError}"/></div>
    </c:if>

    <div class="row g-4">
        <div class="col-lg-7">
            <div class="card shadow-sm">
                <div class="card-header fw-bold">Detalle de la cuenta</div>
                <div class="table-responsive">
                    <table class="table mb-0 align-middle">
                        <thead class="table-light">
                            <tr>
                                <th>Producto</th>
                                <th class="text-center">Cant.</th>
                                <th class="text-end">P. unitario</th>
                                <th class="text-end">Importe</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="l" items="${lineas}">
                                <tr>
                                    <td><c:out value="${l.nombreProducto}"/></td>
                                    <td class="text-center">${l.cantidad}</td>
                                    <td class="text-end">$<fmt:formatNumber value="${l.precioUnitario}" pattern="#,##0.00"/></td>
                                    <td class="text-end">$<fmt:formatNumber value="${l.importe}" pattern="#,##0.00"/></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty lineas}">
                                <tr><td colspan="4" class="text-center text-muted py-4">Este pedido no tiene productos registrados.</td></tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
                <div class="card-body border-top">
                    <div class="d-flex justify-content-between"><span>Subtotal</span>
                        <span>$<fmt:formatNumber value="${sinPropina.subtotal}" pattern="#,##0.00"/></span></div>
                    <div class="d-flex justify-content-between"><span>IVA (${tasaIva} %)</span>
                        <span>$<fmt:formatNumber value="${sinPropina.iva}" pattern="#,##0.00"/></span></div>
                    <div class="d-flex justify-content-between"><span>Propina (${tasaPropina} %)</span>
                        <span><span class="sin-propina">$0.00</span><span class="con-propina">$<fmt:formatNumber value="${conPropina.propina}" pattern="#,##0.00"/></span></span></div>
                    <hr>
                    <div class="d-flex justify-content-between align-items-center">
                        <span class="fs-5 fw-bold">TOTAL</span>
                        <span class="total-grande text-danger">
                            <span class="sin-propina">$<fmt:formatNumber value="${sinPropina.total}" pattern="#,##0.00"/></span>
                            <span class="con-propina">$<fmt:formatNumber value="${conPropina.total}" pattern="#,##0.00"/></span>
                        </span>
                    </div>
                </div>
            </div>
        </div>

        <div class="col-lg-5">
            <form action="CajaServlet" method="POST" class="card shadow-sm" id="formPago">
                <div class="card-header fw-bold">Pago</div>
                <div class="card-body">
                    <input type="hidden" name="accion" value="pagar">
                    <input type="hidden" name="idPedido" value="${pedido.idPedido}">

                    <div class="form-check form-switch mb-3">
                        <input class="form-check-input" type="checkbox" role="switch" id="incluirPropina"
                               name="incluirPropina" value="1" ${not empty param.incluirPropina ? 'checked' : ''}>
                        <label class="form-check-label" for="incluirPropina">Incluir propina sugerida (${tasaPropina} %)</label>
                    </div>

                    <label class="form-label fw-semibold">Método de pago</label>
                    <div class="btn-group w-100 mb-3" role="group">
                        <input type="radio" class="btn-check" name="metodoPago" id="efectivo" value="EFECTIVO"
                               ${param.metodoPago != 'TARJETA' ? 'checked' : ''}>
                        <label class="btn btn-outline-success" for="efectivo">💵 Efectivo</label>
                        <input type="radio" class="btn-check" name="metodoPago" id="tarjeta" value="TARJETA"
                               ${param.metodoPago == 'TARJETA' ? 'checked' : ''}>
                        <label class="btn btn-outline-primary" for="tarjeta">💳 Tarjeta</label>
                    </div>

                    <div id="bloqueEfectivo">
                        <label for="montoRecibido" class="form-label fw-semibold">Monto recibido</label>
                        <div class="input-group mb-2">
                            <span class="input-group-text">$</span>
                            <input type="number" step="0.01" min="0" class="form-control form-control-lg"
                                   id="montoRecibido" name="montoRecibido" value="<c:out value='${param.montoRecibido}'/>">
                        </div>
                        <div class="d-flex gap-2 mb-3 flex-wrap">
                            <button type="button" class="btn btn-sm btn-outline-secondary" data-monto="exacto">Exacto</button>
                            <button type="button" class="btn btn-sm btn-outline-secondary" data-monto="5">$5</button>
                            <button type="button" class="btn btn-sm btn-outline-secondary" data-monto="10">$10</button>
                            <button type="button" class="btn btn-sm btn-outline-secondary" data-monto="20">$20</button>
                            <button type="button" class="btn btn-sm btn-outline-secondary" data-monto="50">$50</button>
                        </div>
                        <div class="alert alert-secondary d-flex justify-content-between mb-3" id="cajaCambio">
                            <span>Cambio</span><strong id="textoCambio">$0.00</strong>
                        </div>
                    </div>

                    <label class="form-label fw-semibold">Datos del cliente (opcional)</label>
                    <input type="text" class="form-control mb-2" name="nombreCliente" maxlength="100"
                           placeholder="Consumidor Final" value="<c:out value='${param.nombreCliente}'/>">
                    <input type="text" class="form-control mb-3" name="documentoCliente" maxlength="20"
                           placeholder="DUI / NIT" value="<c:out value='${param.documentoCliente}'/>">

                    <button type="submit" class="btn btn-danger btn-lg w-100 fw-bold" id="btnPagar"
                            ${empty lineas ? 'disabled' : ''}>Registrar pago y facturar</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
    const TOTAL_SIN_PROPINA = ${sinPropina.total};
    const TOTAL_CON_PROPINA = ${conPropina.total};

    const chkPropina = document.getElementById('incluirPropina');
    const inputMonto = document.getElementById('montoRecibido');
    const bloqueEfectivo = document.getElementById('bloqueEfectivo');
    const textoCambio = document.getElementById('textoCambio');
    const cajaCambio = document.getElementById('cajaCambio');

    function totalActual() {
        return chkPropina.checked ? TOTAL_CON_PROPINA : TOTAL_SIN_PROPINA;
    }
    function esEfectivo() {
        return document.getElementById('efectivo').checked;
    }
    function actualizar() {
        document.body.classList.toggle('propina', chkPropina.checked);
        bloqueEfectivo.style.display = esEfectivo() ? '' : 'none';
        inputMonto.required = esEfectivo();

        const recibido = parseFloat(inputMonto.value);
        if (isNaN(recibido)) {
            textoCambio.textContent = '$0.00';
            cajaCambio.className = 'alert alert-secondary d-flex justify-content-between mb-3';
            return;
        }
        // en centavos para que no salgan decimales raros
        const cambio = Math.round(recibido * 100) - Math.round(totalActual() * 100);
        if (cambio < 0) {
            textoCambio.textContent = 'Faltan $' + (-cambio / 100).toFixed(2);
            cajaCambio.className = 'alert alert-danger d-flex justify-content-between mb-3';
        } else {
            textoCambio.textContent = '$' + (cambio / 100).toFixed(2);
            cajaCambio.className = 'alert alert-success d-flex justify-content-between mb-3';
        }
    }

    document.querySelectorAll('[data-monto]').forEach(btn => {
        btn.addEventListener('click', () => {
            const m = btn.dataset.monto;
            inputMonto.value = (m === 'exacto') ? totalActual().toFixed(2) : m;
            actualizar();
        });
    });
    document.querySelectorAll('input[name=metodoPago]').forEach(r => r.addEventListener('change', actualizar));
    chkPropina.addEventListener('change', actualizar);
    inputMonto.addEventListener('input', actualizar);
    document.getElementById('formPago').addEventListener('submit', () => {
        document.getElementById('btnPagar').disabled = true;
    });
    actualizar();
</script>
</body>
</html>
