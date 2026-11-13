<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
  <jsp:param name="title" value="Contact"/>
  <jsp:param name="bare" value="true"/>
</jsp:include>
<h1>Contact Us</h1>
<div class="two-col">
  <div class="panel">
    <h3>Hospital Details</h3>
    <p><strong>Address:</strong> 12 Health Avenue, Medical Nagar</p>
    <p><strong>Phone:</strong> +91 98765 43210</p>
    <p><strong>Emergency:</strong> 108</p>
    <p><strong>E-mail:</strong> info@medicare.example</p>
    <p><strong>Hours:</strong> OPD 9 AM &ndash; 6 PM &middot; Emergency 24x7</p>
  </div>
  <div class="panel">
    <h3>Send us a message</h3>
    <c:if test="${not empty error}"><div class="alert error"><c:out value="${error}"/></div></c:if>
    <form method="post" action="${ctx}/contact" class="stack">
      <input type="hidden" name="csrf" value="${csrf}">
      <label>Your name <input type="text" name="name" value="<c:out value='${param.name}'/>" required maxlength="100"></label>
      <label>E-mail <input type="email" name="email" value="<c:out value='${param.email}'/>" required maxlength="100"></label>
      <label>Subject <input type="text" name="subject" value="<c:out value='${param.subject}'/>" required maxlength="150"></label>
      <label>Message <textarea name="message" rows="5" required maxlength="3000"><c:out value="${param.message}"/></textarea></label>
      <button class="btn" type="submit">Send Message</button>
    </form>
  </div>
</div>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
