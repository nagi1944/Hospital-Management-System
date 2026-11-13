<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
  <jsp:param name="title" value="Login"/>
  <jsp:param name="bare" value="true"/>
</jsp:include>
<div class="panel narrow">
  <h1>Login</h1>
  <c:if test="${not empty error}"><div class="alert error"><c:out value="${error}"/></div></c:if>
  <form method="post" action="${ctx}/login" class="stack">
    <input type="hidden" name="csrf" value="${csrf}">
    <label>Login as
      <select name="role" required>
        <option value="">-- Select role --</option>
        <option value="admin" ${selectedRole eq 'admin' ? 'selected' : ''}>Admin</option>
        <option value="doctor" ${selectedRole eq 'doctor' ? 'selected' : ''}>Doctor</option>
        <option value="receptionist" ${selectedRole eq 'receptionist' ? 'selected' : ''}>Receptionist</option>
      </select>
    </label>
    <label>Username <input type="text" name="username" value="<c:out value='${param.username}'/>" required autofocus></label>
    <label>Password <input type="password" name="password" required></label>
    <button class="btn" type="submit">Login</button>
    <a href="${ctx}/forgot">Forgot password?</a>
    <a href="${ctx}/signup">New user? Sign up</a>
  </form>
</div>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
