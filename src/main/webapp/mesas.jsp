<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="seccion" value="mesas"/>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Taquicardia - Control de Mesas</title>
    <!-- Bootstrap para diseño rápido y responsivo -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="<c:url value='/assets/css/taquicardia.css'/>">
    <style>
        .tarjeta-mesa {
            transition: transform 0.2s;
            cursor: pointer;
        }
        .tarjeta-mesa:hover {
            transform: scale(1.05);
        }
    </style>
</head>
<body class="bg-light">
<%@ include file="/WEB-INF/jspf/navCaja.jspf" %>

<div class="container mt-5">
    <div class="text-center mb-5">
        <h1 class="display-4 fw-bold text-danger">🌮 TAQUICARDIA 🌮</h1>
        <p class="lead text-muted">Panel de Gestión y Control de Mesas</p>
        <a href="PedidoServlet?accion=verCocina" class="btn btn-outline-dark">Ir al Monitor de Cocina 🍳</a>
    </div>

    <div class="row row-cols-1 row-cols-md-3 row-cols-lg-4 g-4">
        <!-- Bucle JSTL para recorrer las mesas enviadas por tu Servlet -->
        <c:forEach var="mesa" items="${mesas}">
            <div class="col">
                <div class="card h-100 tarjeta-mesa text-center shadow-sm 
                    ${mesa.estado == 'LIBRE' ? 'border-success bg-white' : 'border-danger bg-white'}">
                    
                    <div class="card-header fw-bold text-white 
                        ${mesa.estado == 'LIBRE' ? 'bg-success' : 'bg-danger'}">
                        Mesa N° ${mesa.numeroMesa}
                    </div>
                    
                    <div class="card-body">
                        <p class="card-text text-muted mb-2">Capacidad: <strong>${mesa.capacidad} personas</strong></p>
                        <span class="badge ${mesa.estado == 'LIBRE' ? 'bg-success' : 'bg-danger'} fs-6 mb-3">
                            ${mesa.estado}
                        </span>
                        
                        <!-- Formulario para que el mesero cambie el estado en un clic -->
                        <form action="PedidoServlet" method="POST">
                            <input type="hidden" name="idMesa" value="${mesa.idMesa}">
                            <input type="hidden" name="accion" value="cambiarMesa">
                            
                            <c:choose>
                                <c:when test="${mesa.estado == 'LIBRE'}">
                                    <input type="hidden" name="estado" value="OCUPADA">
                                    <button type="submit" class="btn btn-sm btn-danger w-100">Ocupar Mesa</button>
                                </c:when>
                                <c:otherwise>
                                    <input type="hidden" name="estado" value="LIBRE">
                                    <button type="submit" class="btn btn-sm btn-success w-100">Liberar Mesa</button>
                                </c:otherwise>
                            </c:choose>
                        </form>
                    </div>
                </div>
            </div>
        </c:forEach>
    </div>
</div>

</body>
</html>
