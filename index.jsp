<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:include page="/WEB-INF/jsp/layout/header.jsp">
  <jsp:param name="title" value="Home"/>
  <jsp:param name="bare" value="true"/>
</jsp:include>
<section class="hero">
  <h1>Caring for you, every step of the way</h1>
  <p>MediCare Hospital brings patients, doctors and staff together on one simple platform &mdash;
     appointments, records, billing, pharmacy and laboratory.</p>
  <a class="btn" href="${ctx}/signup">Sign Up</a>
  <a class="btn" href="${ctx}/login">Staff Login</a>
  <a class="btn btn-light" href="${ctx}/contact">Contact Us</a>
</section>

<h2 class="center">Our Services</h2>
<div class="cards">
  <div class="card"><h3>Patient Care</h3><p>Registration, history and medical records kept safely in one place.</p></div>
  <div class="card"><h3>Appointments</h3><p>Easy booking with double-booking protection for every doctor.</p></div>
  <div class="card"><h3>Pharmacy</h3><p>Stock tracking with low-stock and expiry alerts.</p></div>
  <div class="card"><h3>Laboratory</h3><p>Order tests and record results for doctors and patients.</p></div>
  <div class="card"><h3>Billing</h3><p>Itemised bills, automatic totals and payment tracking.</p></div>
  <div class="card"><h3>Reports</h3><p>Revenue, appointment and stock reports for administrators.</p></div>
</div>

<section class="stats">
  <div><strong>24x7</strong><span>Emergency care</span></div>
  <div><strong>50+</strong><span>Specialist doctors</span></div>
  <div><strong>100+</strong><span>Beds</span></div>
  <div><strong>15 yrs</strong><span>Of trusted service</span></div>
</section>
<jsp:include page="/WEB-INF/jsp/layout/footer.jsp"/>
