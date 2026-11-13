<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
    request.setAttribute("csrf", com.hms.util.Security.token(session));
    request.setAttribute("ctx", request.getContextPath());
    com.hms.model.AppUser au = (com.hms.model.AppUser) session.getAttribute("user");
    if (au != null) {
        request.setAttribute("menu", com.hms.model.Entities.menuFor(au.getRole()));
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title><c:out value="${param.title}"/> | MediCare HMS</title>
  <link rel="stylesheet" href="${ctx}/css/style.css">
</head>
<body>
<header class="topbar no-print">
  <a class="brand" href="${ctx}/">&#10010; MediCare HMS</a>
  <button class="nav-toggle" type="button" aria-label="Menu">&#9776;</button>
  <nav class="topnav">
    <a href="${ctx}/">Home</a>
    <a href="${ctx}/about">About</a>
    <a href="${ctx}/contact">Contact</a>
    <c:choose>
      <c:when test="${not empty user}">
        <a href="${ctx}/dashboard">Dashboard</a>
        <span class="who"><c:out value="${user.fullName}"/> (<c:out value="${user.role}"/>)</span>
        <a class="btn btn-sm" href="${ctx}/logout">Logout</a>
      </c:when>
      <c:otherwise>
        <a href="${ctx}/signup">Sign Up</a>
        <a class="btn btn-sm" href="${ctx}/login">Login</a>
      </c:otherwise>
    </c:choose>
  </nav>
</header>

<div class="layout">
<c:if test="${not empty user and param.bare ne 'true'}">
  <aside class="sidebar no-print">
    <a href="${ctx}/dashboard">&#9632; Dashboard</a>
    <c:forEach var="item" items="${menu}">
      <a href="${ctx}/module?m=${item.key}"><c:out value="${item.title}"/></a>
    </c:forEach>
    <c:if test="${user.role eq 'admin'}">
      <a href="${ctx}/reports">Reports</a>
    </c:if>
  </aside>
</c:if>
<main class="content">
<c:if test="${not empty sessionScope.flash}">
  <div class="alert ${empty sessionScope.flashType ? 'ok' : sessionScope.flashType}"><c:out value="${sessionScope.flash}"/></div>
  <c:remove var="flash" scope="session"/>
  <c:remove var="flashType" scope="session"/>
</c:if>
