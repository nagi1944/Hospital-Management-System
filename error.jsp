<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" session="false" import="java.io.*" %>
<%!
  private static String esc(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
%>
<%
  // Deliberately dependency-free (no JSTL, no includes) so it still renders when the rest of the app is broken.
  Object codeObj = request.getAttribute("javax.servlet.error.status_code");
  String msg = (String) request.getAttribute("javax.servlet.error.message");
  Throwable t = (Throwable) request.getAttribute("javax.servlet.error.exception");
  String ctx = request.getContextPath();
  int code = codeObj instanceof Integer ? ((Integer) codeObj).intValue() : 500;
  String title = code == 404 ? "Page not found" : (code == 403 ? "Access denied" : "Something went wrong");
%>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title><%= esc(title) %> | MediCare HMS</title>
<style>
  body{font-family:Segoe UI,Arial,sans-serif;background:#f4f7fb;color:#1f2937;margin:0;padding:2rem}
  .box{max-width:900px;margin:0 auto;background:#fff;border:1px solid #e5e7eb;border-radius:8px;padding:1.5rem}
  a.btn{display:inline-block;background:#0d6efd;color:#fff;padding:.5rem 1rem;border-radius:6px;text-decoration:none}
  pre{background:#f1f5f9;padding:1rem;overflow:auto;font-size:.85rem;white-space:pre-wrap}
  .muted{color:#6b7280}
</style>
</head>
<body>
<div class="box">
  <h1><%= esc(title) %> <span class="muted">(HTTP <%= code %>)</span></h1>
  <% if (msg != null && msg.length() > 0) { %><p><%= esc(msg) %></p><% } %>
  <% String ra = request.getRemoteAddr();
     boolean local = "127.0.0.1".equals(ra) || "0:0:0:0:0:0:0:1".equals(ra) || "::1".equals(ra);
     if (t != null && local) {
       Throwable root = t;
       while (root.getCause() != null && root.getCause() != root) root = root.getCause();
       StringWriter sw = new StringWriter();
       t.printStackTrace(new PrintWriter(sw));
       String trace = sw.toString();
       if (trace.length() > 6000) trace = trace.substring(0, 6000) + "\n...";
  %>
    <h3>Cause</h3>
    <pre><%= esc(root.getClass().getName() + ": " + root.getMessage()) %></pre>
    <h3>Details (send this to your helper)</h3>
    <pre><%= esc(trace) %></pre>
  <% } %>
  <p><a class="btn" href="<%= esc(ctx) %>/">Go to Home</a></p>
</div>
</body>
</html>
