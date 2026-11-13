<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
  <jsp:param name="title" value="Sign Up"/>
  <jsp:param name="bare" value="true"/>
</jsp:include>
<div class="panel narrow">
  <h1>Create Account</h1>
  <p class="muted">Staff registration. Administrator accounts are created by an existing admin.</p>
  <c:if test="${not empty errors}">
    <div class="alert error">
      <c:forEach var="e" items="${errors}"><div><c:out value="${e}"/></div></c:forEach>
    </div>
  </c:if>
  <form method="post" action="${ctx}/signup" class="stack" id="signupForm">
    <input type="hidden" name="csrf" value="${csrf}">
    <label>Register as
      <select name="role" id="roleSelect" required>
        <option value="">-- Select role --</option>
        <option value="doctor" ${param.role eq 'doctor' ? 'selected' : ''}>Doctor</option>
        <option value="receptionist" ${param.role eq 'receptionist' ? 'selected' : ''}>Receptionist</option>
      </select>
    </label>
    <label>Full name <input type="text" name="full_name" value="<c:out value='${param.full_name}'/>" required maxlength="100"></label>
    <label>E-mail <input type="email" name="email" value="<c:out value='${param.email}'/>" required maxlength="100"></label>
    <div class="doctor-only" style="display:none">
      <label>Specialization <input type="text" name="specialization" value="<c:out value='${param.specialization}'/>" maxlength="100"></label>
      <label>Phone <input type="text" name="phone" value="<c:out value='${param.phone}'/>" maxlength="20"></label>
    </div>
    <label>Username <input type="text" name="username" value="<c:out value='${param.username}'/>" required minlength="4" maxlength="30" pattern="[A-Za-z0-9_.]{4,30}" title="4-30 letters, digits, dot or underscore"></label>
    <label>Password <input type="password" name="password" required minlength="6" autocomplete="new-password"></label>
    <label>Confirm password <input type="password" name="confirm" required minlength="6" autocomplete="new-password"></label>
    <button class="btn" type="submit">Sign Up</button>
    <a href="${ctx}/login">Already have an account? Login</a>
  </form>
</div>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
