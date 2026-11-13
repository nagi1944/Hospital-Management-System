<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
  <jsp:param name="title" value="${module.title}"/>
</jsp:include>
<div class="page-head">
  <h1><c:out value="${module.title}"/></h1>
  <c:if test="${canCreate}">
    <a class="btn" href="${ctx}/module?m=${module.key}&amp;action=new">+ Add ${module.singular}</a>
  </c:if>
</div>

<form method="get" action="${ctx}/module" class="search no-print">
  <input type="hidden" name="m" value="${module.key}">
  <input type="text" name="q" value="<c:out value='${q}'/>" placeholder="Search ${module.title}...">
  <button class="btn" type="submit">Search</button>
  <c:if test="${not empty q}"><a class="btn btn-light" href="${ctx}/module?m=${module.key}">Clear</a></c:if>
</form>

<div class="table-wrap">
<table class="table">
  <thead>
    <tr>
      <th>ID</th>
      <c:forEach var="f" items="${module.fields}">
        <c:if test="${f.list}"><th><c:out value="${f.label}"/></th></c:if>
      </c:forEach>
      <c:if test="${module.key eq 'medicines'}"><th>Alert</th></c:if>
      <th class="no-print">Actions</th>
    </tr>
  </thead>
  <tbody>
    <c:forEach var="row" items="${rows}">
      <tr class="${row.flag eq 'Expired' ? 'row-expired' : (row.flag eq 'Low stock' ? 'row-low' : '')}">
        <td>${row.id}</td>
        <c:forEach var="f" items="${module.fields}">
          <c:if test="${f.list}">
            <td>
              <c:choose>
                <c:when test="${f.name eq 'status'}"><span class="badge s-${row.status}"><c:out value="${row.status}"/></span></c:when>
                <c:when test="${f.type eq 'ref'}"><c:out value="${row[f.labelKey]}"/></c:when>
                <c:otherwise><c:out value="${row[f.name]}"/></c:otherwise>
              </c:choose>
            </td>
          </c:if>
        </c:forEach>
        <c:if test="${module.key eq 'medicines'}">
          <td><c:if test="${not empty row.flag}"><span class="badge ${row.flag eq 'Expired' ? 's-Cancelled' : 's-Pending'}">${row.flag}</span></c:if></td>
        </c:if>
        <td class="actions no-print">
          <a class="btn btn-sm btn-light" href="${ctx}/module?m=${module.key}&amp;action=view&amp;id=${row.id}">View</a>
          <c:if test="${canEdit}">
            <a class="btn btn-sm btn-light" href="${ctx}/module?m=${module.key}&amp;action=edit&amp;id=${row.id}">Edit</a>
          </c:if>
          <c:if test="${module.key eq 'bills' and canWrite and row.status eq 'Pending'}">
            <form method="post" action="${ctx}/module" class="inline">
              <input type="hidden" name="csrf" value="${csrf}">
              <input type="hidden" name="m" value="${module.key}">
              <input type="hidden" name="action" value="pay">
              <input type="hidden" name="id" value="${row.id}">
              <button class="btn btn-sm btn-ok" type="submit">Pay</button>
            </form>
          </c:if>
          <c:if test="${canWrite}">
            <form method="post" action="${ctx}/module" class="inline" data-confirm="Delete this ${module.singular}?">
              <input type="hidden" name="csrf" value="${csrf}">
              <input type="hidden" name="m" value="${module.key}">
              <input type="hidden" name="action" value="delete">
              <input type="hidden" name="id" value="${row.id}">
              <button class="btn btn-sm btn-danger" type="submit">Delete</button>
            </form>
          </c:if>
        </td>
      </tr>
    </c:forEach>
    <c:if test="${empty rows}">
      <tr><td colspan="20" class="center muted">No records found.</td></tr>
    </c:if>
  </tbody>
</table>
</div>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
