<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="seccion" value="menu"/>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${tituloFormulario}"/> | Taquicardia</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="<c:url value='/assets/css/taquicardia.css'/>">
</head>
<body class="bg-light">
<%@ include file="/WEB-INF/jspf/navCaja.jspf" %>
<main class="container app-shell py-4 py-lg-5 form-shell">
    <a class="back-link" href="<c:url value='/MenuServlet'/>">← Volver al catálogo</a>
    <section class="form-card mt-3">
        <div class="form-card__intro"><span class="section-kicker">CATÁLOGO</span><h1><c:out value="${tituloFormulario}"/></h1><p>Los cambios de precio no modifican el precio histórico de pedidos ya registrados.</p></div>
        <c:if test="${not empty errores}"><div class="alert alert-danger" role="alert">Revisa los campos señalados antes de guardar.</div></c:if>
        <form action="MenuServlet" method="post" class="row g-3" novalidate>
            <input type="hidden" name="accion" value="guardar">
            <input type="hidden" name="idProducto" value="${producto.idProducto}">
            <div class="col-12">
                <label for="nombre" class="form-label">Nombre del producto <span class="required-mark">*</span></label>
                <input id="nombre" name="nombre" class="form-control ${not empty errores.nombre ? 'is-invalid' : ''}" required maxlength="100" value="<c:out value='${producto.nombre}'/>" placeholder="Ej. Tacos al pastor">
                <c:if test="${not empty errores.nombre}"><div class="invalid-feedback"><c:out value="${errores.nombre}"/></div></c:if>
            </div>
            <div class="col-12">
                <label for="descripcion" class="form-label">Descripción</label>
                <textarea id="descripcion" name="descripcion" class="form-control ${not empty errores.descripcion ? 'is-invalid' : ''}" rows="3" maxlength="500" placeholder="Ingredientes o una breve descripción"><c:out value="${producto.descripcion}"/></textarea>
                <div class="form-text">Máximo 500 caracteres.</div>
                <c:if test="${not empty errores.descripcion}"><div class="invalid-feedback d-block"><c:out value="${errores.descripcion}"/></div></c:if>
            </div>
            <div class="col-md-6">
                <label for="idCategoria" class="form-label">Categoría <span class="required-mark">*</span></label>
                <select id="idCategoria" name="idCategoria" class="form-select ${not empty errores.idCategoria ? 'is-invalid' : ''}" required>
                    <option value="">Seleccionar categoría</option>
                    <c:forEach var="cat" items="${categorias}"><option value="${cat.idCategoria}" ${producto.idCategoria == cat.idCategoria ? 'selected' : ''}><c:out value="${cat.nombre}"/></option></c:forEach>
                </select>
                <c:if test="${not empty errores.idCategoria}"><div class="invalid-feedback"><c:out value="${errores.idCategoria}"/></div></c:if>
                <c:if test="${empty categorias}"><div class="form-text text-danger">Primero registra una categoría.</div></c:if>
            </div>
            <div class="col-md-6">
                <label for="tipo" class="form-label">Tipo de producto <span class="required-mark">*</span></label>
                <select id="tipo" name="tipo" class="form-select ${not empty errores.tipo ? 'is-invalid' : ''}" required>
                    <option value="">Seleccionar tipo</option>
                    <option value="PLATILLO" ${producto.tipo == 'PLATILLO' ? 'selected' : ''}>Platillo</option>
                    <option value="BEBIDA" ${producto.tipo == 'BEBIDA' ? 'selected' : ''}>Bebida</option>
                </select>
                <c:if test="${not empty errores.tipo}"><div class="invalid-feedback"><c:out value="${errores.tipo}"/></div></c:if>
            </div>
            <div class="col-md-6">
                <label for="precio" class="form-label">Precio (USD) <span class="required-mark">*</span></label>
                <div class="input-group"><span class="input-group-text">$</span><input id="precio" name="precio" type="number" class="form-control ${not empty errores.precio ? 'is-invalid' : ''}" required min="0.01" max="99999999.99" step="0.01" value="<c:out value='${producto.precio}'/>" placeholder="0.00"></div>
                <c:if test="${not empty errores.precio}"><div class="field-error"><c:out value="${errores.precio}"/></div></c:if>
                <div class="form-text">IVA y propina se calculan en caja, no en el precio del producto.</div>
            </div>
            <div class="col-md-6">
                <label for="activo" class="form-label">Estado del catálogo</label>
                <select id="activo" name="activo" class="form-select"><option value="1" ${producto.activo ? 'selected' : ''}>Activo</option><option value="0" ${not producto.activo ? 'selected' : ''}>Archivado</option></select>
            </div>
            <div class="col-12"><div class="availability-control form-check form-switch">
                <input id="disponible" name="disponible" value="1" type="checkbox" role="switch" class="form-check-input" ${producto.disponible ? 'checked' : ''}>
                <label for="disponible" class="form-check-label"><strong>Disponible para ofrecer</strong><span>Desactívalo temporalmente si se agotó.</span></label>
            </div></div>
            <div class="col-12 d-flex flex-wrap justify-content-end gap-2 pt-2">
                <a href="<c:url value='/MenuServlet'/>" class="btn btn-outline-secondary">Cancelar</a>
                <button class="btn btn-verde btn-lg" type="submit" ${empty categorias ? 'disabled' : ''}>Guardar producto</button>
            </div>
        </form>
    </section>
</main>
</body>
</html>
