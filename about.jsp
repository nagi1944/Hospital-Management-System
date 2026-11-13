<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
  <jsp:param name="title" value="About"/>
  <jsp:param name="bare" value="true"/>
</jsp:include>
<h1>About MediCare Hospital</h1>
<div class="panel">
  <h3>Our Mission</h3>
  <p>To deliver affordable, compassionate and high-quality healthcare supported by modern technology.</p>
  <h3>Our Vision</h3>
  <p>To be the most trusted healthcare provider in the region.</p>
  <h3>Facilities</h3>
  <ul>
    <li>24x7 emergency and ambulance service</li>
    <li>Outpatient and inpatient departments</li>
    <li>In-house pharmacy and diagnostic laboratory</li>
    <li>Digital medical records and online billing</li>
  </ul>
  <h3>Departments</h3>
  <p>General Medicine &middot; Cardiology &middot; Orthopaedics &middot; Paediatrics &middot; Gynaecology &middot; Dermatology &middot; ENT</p>
</div>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
