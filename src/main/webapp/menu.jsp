<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>
<fmt:setLocale value="es_SV"/>
<c:set var="seccion" value="menu"/>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Menú | Taquicardia</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="<c:url value='/assets/css/taquicardia.css'/>">
</head>
<body class="bg-light">
<%@ include file="/WEB-INF/jspf/navCaja.jspf" %>
<main class="container app-shell py-4 py-lg-5">
    <section class="menu-hero mb-4 mb-lg-5">
        <div class="menu-hero__content">
            <span class="eyebrow">SABOR HECHO EN CASA</span>
            <h1>El menú que reúne a todos</h1>
            <p>Administra platillos y bebidas, cuida sus precios y decide qué está listo para servirse.</p>
            <div class="d-flex flex-wrap gap-2 mt-4">
                <a href="<c:url value='/MenuServlet?accion=nuevo'/>" class="btn btn-maiz btn-lg">+ Nuevo producto</a>
                <a href="<c:url value='/CategoriaServlet'/>" class="btn btn-outline-light btn-lg">Gestionar categorías</a>
            </div>
        </div>
        <div class="menu-hero__badge"><span>COCINA</span><strong>con corazón</strong><span>mexicano</span></div>
    </section>

    <c:if test="${not empty mensaje}">
        <div class="alert alert-${tipoMensaje == 'error' ? 'danger' : (tipoMensaje == 'aviso' ? 'warning' : 'success')} alert-dismissible fade show" role="status">
            <c:out value="${mensaje}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Cerrar"></button>
        </div>
    </c:if>

    <section class="d-flex flex-wrap align-items-end justify-content-between gap-3 mb-3">
        <div>
            <span class="section-kicker">CATÁLOGO</span>
            <h2 class="section-title mb-1">Platillos y bebidas</h2>
            <p class="text-secondary mb-0">El precio del menú se administra por separado del IVA y la propina.</p>
        </div>
        <a class="text-link" href="<c:url value='/CategoriaServlet'/>">Ver categorías <span aria-hidden="true">→</span></a>
    </section>

    <form class="filter-panel row g-2 align-items-end mb-4" action="MenuServlet" method="get">
        <input type="hidden" name="accion" value="listar">
        <div class="col-12 col-lg-3">
            <label for="q" class="form-label">Buscar producto</label>
            <input id="q" name="q" type="search" class="form-control" maxlength="100" placeholder="Nombre o descripción" value="<c:out value='${filtroQ}'/>">
        </div>
        <div class="col-6 col-lg-2">
            <label for="tipo" class="form-label">Tipo</label>
            <select id="tipo" name="tipo" class="form-select">
                <option value="">Todos</option>
                <option value="PLATILLO" ${filtroTipo == 'PLATILLO' ? 'selected' : ''}>Platillos</option>
                <option value="BEBIDA" ${filtroTipo == 'BEBIDA' ? 'selected' : ''}>Bebidas</option>
            </select>
        </div>
        <div class="col-6 col-lg-2">
            <label for="categoria" class="form-label">Categoría</label>
            <select id="categoria" name="categoria" class="form-select">
                <option value="">Todas</option>
                <c:forEach var="cat" items="${categorias}">
                    <option value="${cat.idCategoria}" ${filtroCategoria == cat.idCategoria ? 'selected' : ''}><c:out value="${cat.nombre}"/></option>
                </c:forEach>
            </select>
        </div>
        <div class="col-6 col-lg-2">
            <label for="disponible" class="form-label">Disponibilidad</label>
            <select id="disponible" name="disponible" class="form-select">
                <option value="">Todas</option>
                <option value="si" ${filtroDisponible == 'si' ? 'selected' : ''}>Disponible</option>
                <option value="no" ${filtroDisponible == 'no' ? 'selected' : ''}>Agotado</option>
            </select>
        </div>
        <div class="col-6 col-lg-2">
            <label for="estado" class="form-label">Estado del catálogo</label>
            <select id="estado" name="estado" class="form-select">
                <option value="activos" ${filtroEstado == 'activos' ? 'selected' : ''}>Activos</option>
                <option value="inactivos" ${filtroEstado == 'inactivos' ? 'selected' : ''}>Archivados</option>
                <option value="todos" ${filtroEstado == 'todos' ? 'selected' : ''}>Todos</option>
            </select>
        </div>
        <div class="col-12 col-lg-1 d-flex gap-2">
            <button class="btn btn-verde w-100" type="submit">Filtrar</button>
        </div>
    </form>

    <section class="catalog-panel">
        <div class="catalog-panel__head">
            <div><span class="section-kicker">GESTIÓN</span><h2 class="h4 mb-0">Productos registrados</h2></div>
            <span class="count-pill"><c:out value="${productos.size()}"/> resultados</span>
        </div>
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0 product-table">
                <thead><tr><th>Producto</th><th>Categoría</th><th>Tipo</th><th class="text-end">Precio</th><th>Disponibilidad</th><th>Catálogo</th><th class="text-end">Acciones</th></tr></thead>
                <tbody>
                <c:forEach var="p" items="${productos}">
                    <tr class="${not p.activo ? 'product-row--archived' : ''}">
                        <td><div class="product-name"><span class="product-mark ${p.tipo == 'BEBIDA' ? 'product-mark--drink' : ''}">${p.tipo == 'BEBIDA' ? '✿' : '✦'}</span><div><strong><c:out value="${p.nombre}"/></strong><c:if test="${not empty p.descripcion}"><small><c:out value="${p.descripcion}"/></small></c:if></div></div></td>
                        <td><c:out value="${p.categoriaNombre}"/></td>
                        <td><span class="type-pill ${p.tipo == 'BEBIDA' ? 'type-pill--drink' : ''}"><c:out value="${p.tipo == 'BEBIDA' ? 'Bebida' : 'Platillo'}"/></span></td>
                        <td class="text-end price-cell">$<fmt:formatNumber value="${p.precio}" pattern="#,##0.00"/></td>
                        <td><span class="status-pill ${p.disponible ? 'status-pill--available' : 'status-pill--soldout'}"><c:out value="${p.disponible ? 'Disponible' : 'Agotado'}"/></span></td>
                        <td><span class="status-pill ${p.activo ? 'status-pill--active' : 'status-pill--inactive'}"><c:out value="${p.activo ? 'Activo' : 'Archivado'}"/></span></td>
                        <td><div class="table-actions">
                            <c:if test="${p.activo}">
                                <a class="btn btn-sm btn-outline-verde" href="<c:url value='/MenuServlet?accion=editar&id=${p.idProducto}'/>">Editar</a>
                                <form action="MenuServlet" method="post" class="d-inline">
                                    <input type="hidden" name="accion" value="disponibilidad"><input type="hidden" name="idProducto" value="${p.idProducto}">
                                    <input type="hidden" name="disponible" value="${p.disponible ? '0' : '1'}">
                                    <button class="btn btn-sm btn-outline-secondary" type="submit"><c:out value="${p.disponible ? 'Agotar' : 'Ofrecer'}"/></button>
                                </form>
                            </c:if>
                            <form action="MenuServlet" method="post" class="d-inline" onsubmit="return confirm('¿Eliminar o archivar este producto? Si ya aparece en pedidos, se archivará para conservar el historial.');">
                                <input type="hidden" name="accion" value="eliminar"><input type="hidden" name="idProducto" value="${p.idProducto}">
                                <button class="btn btn-sm btn-outline-danger" type="submit"><c:out value="${p.activo ? 'Eliminar' : 'Borrar'}"/></button>
                            </form>
                        </div></td>
                    </tr>
                </c:forEach>
                <c:if test="${empty productos}"><tr><td colspan="7"><div class="empty-state"><span>✦</span><strong>No hay productos con esos filtros.</strong><p>Prueba otra búsqueda o registra un nuevo platillo o bebida.</p><a class="btn btn-verde" href="<c:url value='/MenuServlet?accion=nuevo'/>">Registrar producto</a></div></td></tr></c:if>
                </tbody>
            </table>
        </div>
    </section>
    <p class="menu-note mt-3"><strong>Integridad del historial:</strong> los productos usados en pedidos se archivan, no se borran físicamente.</p>
</main>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
