<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
  <jsp:param name="title" value="Dashboard"/>
</jsp:include>
<h1>Welcome, <c:out value="${user.fullName}"/></h1>
<p class="muted">Signed in as <c:out value="${user.role}"/></p>

<div class="cards stats-cards">
<c:choose>
  <c:when test="${isDoctor}">
    <div class="card stat"><span>${sToday}</span>Today's appointments</div>
    <div class="card stat"><span>${sPatients}</span>My patients</div>
    <div class="card stat"><span>${sLabs}</span>Pending lab tests</div>
    <div class="card stat"><span>${sRecords}</span>Medical records</div>
  </c:when>
  <c:otherwise>
    <div class="card stat"><span>${sPatients}</span>Patients</div>
    <div class="card stat"><span>${sDoctors}</span>Doctors</div>
    <div class="card stat"><span>${sToday}</span>Appointments today</div>
    <div class="card stat"><span>${sPending}</span>Pending bills</div>
    <c:if test="${user.role eq 'admin'}">
      <div class="card stat"><span>&#8377; ${sRevenue}</span>Revenue collected</div>
    </c:if>
    <div class="card stat"><span>${sLow}</span>Stock alerts</div>
  </c:otherwise>
</c:choose>
</div>

<h2>Upcoming appointments</h2>
<div class="table-wrap">
<table class="table">
  <thead><tr><th>ID</th><th>Patient</th><th>Doctor</th><th>Date</th><th>Time</th><th>Status</th></tr></thead>
  <tbody>
    <c:forEach var="a" items="${upcoming}">
      <tr>
        <td>${a.id}</td>
        <td><c:out value="${a.patient}"/></td>
        <td>Dr. <c:out value="${a.doctor}"/></td>
        <td>${a.appt_date}</td>
        <td>${a.appt_time}</td>
        <td><span class="badge s-${a.status}">${a.status}</span></td>
      </tr>
    </c:forEach>
    <c:if test="${empty upcoming}"><tr><td colspan="6" class="center muted">No upcoming appointments.</td></tr></c:if>
  </tbody>
</table>
</div>
<p><a class="btn" href="${ctx}/module?m=appointments">Open Appointments</a></p>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
