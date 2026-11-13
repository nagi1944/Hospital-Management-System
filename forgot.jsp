<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
  <jsp:param name="title" value="Forgot Password"/>
  <jsp:param name="bare" value="true"/>
</jsp:include>
<div class="panel narrow">
  <h1>Reset Password</h1>
  <p class="muted">Enter your username and the e-mail registered with your account.</p>
  <c:if test="${not empty error}"><div class="alert error"><c:out value="${error}"/></div></c:if>
  <form method="post" action="${ctx}/forgot" class="stack">
    <input type="hidden" name="csrf" value="${csrf}">
    <label>Username <input type="text" name="username" value="<c:out value='${param.username}'/>" required></label>
    <label>Registered e-mail <input type="email" name="email" value="<c:out value='${param.email}'/>" required></label>
    <label>New password <input type="password" name="password" minlength="6" required></label>
    <label>Confirm password <input type="password" name="confirm" minlength="6" required></label>
    <button class="btn" type="submit">Update Password</button>
    <a href="${ctx}/login">Back to login</a>
  </form>
</div>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
