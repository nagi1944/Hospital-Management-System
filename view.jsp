<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
  <jsp:param name="title" value="${module.singular} Details"/>
</jsp:include>
<div class="page-head no-print">
  <h1>${module.singular} #<c:out value="${row.id}"/></h1>
  <div>
    <button class="btn" type="button" onclick="window.print()">Print</button>
    <c:if test="${canEdit}">
      <a class="btn btn-light" href="${ctx}/module?m=${module.key}&amp;action=edit&amp;id=${row.id}">Edit</a>
    </c:if>
    <a class="btn btn-light" href="${ctx}/module?m=${module.key}">Back to list</a>
  </div>
</div>

<div class="panel printable">
  <div class="print-only print-head">
    <h2>&#10010; MediCare Hospital</h2>
    <p>12 Health Avenue, Medical Nagar &middot; +91 98765 43210</p>
    <h3>${module.singular} #<c:out value="${row.id}"/></h3>
  </div>
  <table class="details">
    <c:forEach var="f" items="${module.fields}">
      <c:if test="${f.type ne 'password'}">
        <tr>
          <th><c:out value="${f.label}"/></th>
          <td class="${f.type eq 'textarea' ? 'prewrap' : ''}">
            <c:choose>
              <c:when test="${f.name eq 'status'}"><span class="badge s-${row.status}"><c:out value="${row.status}"/></span></c:when>
              <c:when test="${f.type eq 'ref'}"><c:out value="${row[f.labelKey]}"/></c:when>
              <c:otherwise><c:out value="${row[f.name]}"/></c:otherwise>
            </c:choose>
          </td>
        </tr>
      </c:if>
    </c:forEach>
  </table>
  <div class="print-only signature">Doctor / Authorised signature</div>
</div>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
