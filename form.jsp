<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
  <jsp:param name="title" value="${module.singular}"/>
</jsp:include>
<div class="page-head">
  <h1><c:out value="${empty row.id ? 'Add' : 'Edit'}"/> ${module.singular}</h1>
  <a class="btn btn-light" href="${ctx}/module?m=${module.key}">Back to list</a>
</div>

<c:if test="${not empty errors}">
  <div class="alert error">
    <c:forEach var="e" items="${errors}"><div><c:out value="${e}"/></div></c:forEach>
  </div>
</c:if>

<form method="post" action="${ctx}/module" class="panel form-grid" data-module="${module.key}">
  <input type="hidden" name="csrf" value="${csrf}">
  <input type="hidden" name="m" value="${module.key}">
  <input type="hidden" name="action" value="save">
  <input type="hidden" name="id" value="<c:out value='${row.id}'/>">

  <c:forEach var="f" items="${module.fields}">
    <c:if test="${f.name ne hiddenCol}">
      <div class="field ${f.type eq 'textarea' ? 'full' : ''}">
        <label for="f_${f.name}"><c:out value="${f.label}"/><c:if test="${f.required}"> *</c:if></label>
        <c:choose>
          <c:when test="${f.type eq 'textarea'}">
            <textarea id="f_${f.name}" name="${f.name}" rows="4" ${f.required ? 'required' : ''}><c:out value="${row[f.name]}"/></textarea>
          </c:when>
          <c:when test="${f.type eq 'select'}">
            <select id="f_${f.name}" name="${f.name}" ${f.required ? 'required' : ''}>
              <option value="">-- Select --</option>
              <c:forEach var="o" items="${f.options}">
                <option value="${o}" ${row[f.name] eq o ? 'selected' : ''}>${o}</option>
              </c:forEach>
            </select>
          </c:when>
          <c:when test="${f.type eq 'ref'}">
            <select id="f_${f.name}" name="${f.name}" ${f.required ? 'required' : ''}>
              <option value="">-- Select --</option>
              <c:forEach var="o" items="${refs[f.name]}">
                <option value="${o.id}" ${row[f.name] eq o.id ? 'selected' : ''}><c:out value="${o.label}"/></option>
              </c:forEach>
            </select>
          </c:when>
          <c:when test="${f.type eq 'password'}">
            <input id="f_${f.name}" type="password" name="${f.name}" autocomplete="new-password"
                   placeholder="${empty row.id ? '' : 'Leave blank to keep current password'}" ${empty row.id ? 'required' : ''}>
          </c:when>
          <c:otherwise>
            <input id="f_${f.name}" type="${f.type}" name="${f.name}" value="<c:out value='${row[f.name]}'/>"
                   step="any" ${f.type eq 'number' ? 'min="0"' : ''}
                   ${f.computed ? 'readonly' : ''} ${f.required ? 'required' : ''}>
          </c:otherwise>
        </c:choose>
      </div>
    </c:if>
  </c:forEach>

  <div class="field full">
    <button class="btn" type="submit">Save</button>
    <a class="btn btn-light" href="${ctx}/module?m=${module.key}">Cancel</a>
  </div>
</form>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
