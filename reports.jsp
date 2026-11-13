<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
  <jsp:param name="title" value="Reports"/>
</jsp:include>
<div class="page-head no-print">
  <h1>Reports</h1>
  <button class="btn" type="button" onclick="window.print()">Print</button>
</div>

<div class="cards stats-cards">
  <div class="card stat"><span>&#8377; ${paidTotal}</span>Revenue collected</div>
  <div class="card stat"><span>&#8377; ${pendingTotal}</span>Pending payments</div>
</div>

<div class="two-col">
  <div class="panel">
    <h3>Monthly revenue (paid bills)</h3>
    <c:forEach var="r" items="${revenue}">
      <div class="bar-row"><span class="bar-label">${r.month}</span>
        <div class="bar"><span style="width:${r.pct}%"></span></div>
        <span class="bar-val">&#8377; ${r.amount}</span></div>
    </c:forEach>
    <c:if test="${empty revenue}"><p class="muted">No paid bills yet.</p></c:if>
  </div>
  <div class="panel">
    <h3>Top doctors by appointments</h3>
    <c:forEach var="d" items="${topDoctors}">
      <div class="bar-row"><span class="bar-label">Dr. <c:out value="${d.name}"/></span>
        <div class="bar"><span style="width:${d.pct}%"></span></div>
        <span class="bar-val">${d.cnt}</span></div>
    </c:forEach>
  </div>
  <div class="panel">
    <h3>Appointments by status</h3>
    <table class="table"><tbody>
      <c:forEach var="s" items="${apptStatus}"><tr><td><span class="badge s-${s.status}">${s.status}</span></td><td>${s.cnt}</td></tr></c:forEach>
    </tbody></table>
  </div>
  <div class="panel">
    <h3>Patients by gender</h3>
    <table class="table"><tbody>
      <c:forEach var="g" items="${genders}"><tr><td><c:out value="${g.gender}"/></td><td>${g.cnt}</td></tr></c:forEach>
    </tbody></table>
  </div>
</div>

<div class="panel">
  <h3>Pharmacy alerts</h3>
  <table class="table">
    <thead><tr><th>Medicine</th><th>Quantity</th><th>Reorder level</th><th>Expiry</th><th>Alert</th></tr></thead>
    <tbody>
      <c:forEach var="m" items="${stockAlerts}">
        <tr><td><c:out value="${m.name}"/></td><td>${m.quantity}</td><td>${m.reorder_level}</td><td>${m.expiry_date}</td>
            <td><span class="badge ${m.flag eq 'Expired' ? 's-Cancelled' : 's-Pending'}">${m.flag}</span></td></tr>
      </c:forEach>
      <c:if test="${empty stockAlerts}"><tr><td colspan="5" class="center muted">No alerts.</td></tr></c:if>
    </tbody>
  </table>
</div>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
