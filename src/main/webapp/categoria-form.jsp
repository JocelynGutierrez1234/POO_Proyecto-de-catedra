<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="seccion" value="categorias"/>
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
    <a class="back-link" href="<c:url value='/CategoriaServlet'/>">← Volver a categorías</a>
    <section class="form-card mt-3">
        <div class="form-card__intro"><span class="section-kicker">ORGANIZACIÓN DEL MENÚ</span><h1><c:out value="${tituloFormulario}"/></h1><p>Usa nombres claros: por ejemplo, Antojitos, Platos fuertes o Bebidas.</p></div>
        <c:if test="${not empty errores}"><div class="alert alert-danger" role="alert">Revisa los campos señalados antes de guardar.</div></c:if>
        <form action="CategoriaServlet" method="post" class="row g-3">
            <input type="hidden" name="accion" value="guardar"><input type="hidden" name="idCategoria" value="${categoria.idCategoria}">
            <div class="col-12"><label for="nombre" class="form-label">Nombre <span class="required-mark">*</span></label><input id="nombre" name="nombre" class="form-control ${not empty errores.nombre ? 'is-invalid' : ''}" required maxlength="80" value="<c:out value='${categoria.nombre}'/>" placeholder="Ej. Bebidas"><c:if test="${not empty errores.nombre}"><div class="invalid-feedback"><c:out value="${errores.nombre}"/></div></c:if></div>
            <div class="col-12"><label for="descripcion" class="form-label">Descripción</label><textarea id="descripcion" name="descripcion" class="form-control ${not empty errores.descripcion ? 'is-invalid' : ''}" rows="3" maxlength="250" placeholder="Descripción breve y útil"><c:out value="${categoria.descripcion}"/></textarea><c:if test="${not empty errores.descripcion}"><div class="invalid-feedback d-block"><c:out value="${errores.descripcion}"/></div></c:if></div>
            <div class="col-12 d-flex justify-content-end gap-2 pt-2"><a class="btn btn-outline-secondary" href="<c:url value='/CategoriaServlet'/>">Cancelar</a><button class="btn btn-verde btn-lg" type="submit">Guardar categoría</button></div>
        </form>
    </section>
</main>
</body>
</html>
