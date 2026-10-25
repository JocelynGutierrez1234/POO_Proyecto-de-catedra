<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="seccion" value="categorias"/>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Categorías | Taquicardia</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="<c:url value='/assets/css/taquicardia.css'/>">
</head>
<body class="bg-light">
<%@ include file="/WEB-INF/jspf/navCaja.jspf" %>
<main class="container app-shell py-4 py-lg-5">
    <section class="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-4">
        <div><span class="section-kicker">ORGANIZA TU CARTA</span><h1 class="section-title mb-1">Categorías</h1><p class="text-secondary mb-0">Agrupa productos para que el catálogo sea fácil de buscar y mantener.</p></div>
        <a class="btn btn-verde" href="<c:url value='/CategoriaServlet?accion=nuevo'/>">+ Nueva categoría</a>
    </section>
    <c:if test="${not empty mensaje}"><div class="alert alert-${tipoMensaje == 'error' ? 'danger' : (tipoMensaje == 'aviso' ? 'warning' : 'success')} alert-dismissible fade show"><c:out value="${mensaje}"/><button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Cerrar"></button></div></c:if>
    <section class="catalog-panel">
        <div class="catalog-panel__head"><div><span class="section-kicker">CATÁLOGO</span><h2 class="h4 mb-0">Categorías registradas</h2></div><a class="text-link" href="<c:url value='/MenuServlet'/>">Volver al menú →</a></div>
        <div class="table-responsive"><table class="table table-hover align-middle mb-0 product-table">
            <thead><tr><th>Categoría</th><th>Descripción</th><th class="text-center">Productos</th><th class="text-end">Acciones</th></tr></thead>
            <tbody>
            <c:forEach var="cat" items="${categorias}">
                <tr><td><strong><c:out value="${cat.nombre}"/></strong></td><td><span class="text-secondary"><c:out value="${empty cat.descripcion ? '—' : cat.descripcion}"/></span></td><td class="text-center"><span class="count-pill"><c:out value="${cat.totalProductos}"/></span></td>
                    <td class="text-end"><div class="table-actions justify-content-end"><a class="btn btn-sm btn-outline-verde" href="<c:url value='/CategoriaServlet?accion=editar&id=${cat.idCategoria}'/>">Editar</a><form action="CategoriaServlet" method="post" class="d-inline" onsubmit="return confirm('¿Eliminar la categoría? Solo se permite si no tiene productos asociados.');"><input type="hidden" name="accion" value="eliminar"><input type="hidden" name="idCategoria" value="${cat.idCategoria}"><button class="btn btn-sm btn-outline-danger" type="submit" ${cat.totalProductos > 0 ? 'disabled title="Mueve o elimina sus productos primero"' : ''}>Eliminar</button></form></div></td>
                </tr>
            </c:forEach>
            <c:if test="${empty categorias}"><tr><td colspan="4"><div class="empty-state"><span>✦</span><strong>Aún no hay categorías.</strong><p>Crea categorías para clasificar platillos y bebidas.</p><a class="btn btn-verde" href="<c:url value='/CategoriaServlet?accion=nuevo'/>">Crear primera categoría</a></div></td></tr></c:if>
            </tbody>
        </table></div>
    </section>
    <p class="menu-note mt-3">Una categoría con productos no se puede eliminar; primero reasigna o elimina sus productos.</p>
</main>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
