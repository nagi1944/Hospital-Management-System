<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" import="java.sql.*,java.util.*,java.io.*" %>
<%!
  private static String esc(String s) { return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;"); }
  private static String has(String cls) {
    try { Class.forName(cls, false, Thread.currentThread().getContextClassLoader()); return "OK"; }
    catch (Throwable t) { return "MISSING  (" + t.getClass().getSimpleName() + ")"; }
  }
%>
<%
  // Diagnostics reveal server details: only answer to the machine Tomcat runs on.
  String ra = request.getRemoteAddr();
  if (!("127.0.0.1".equals(ra) || "0:0:0:0:0:0:0:1".equals(ra) || "::1".equals(ra))) {
    response.sendError(403, "Diagnostics are only available from the server itself.");
    return;
  }
%>
<!DOCTYPE html>
<html><head><meta charset="UTF-8"><title>HMS diagnostics</title>
<style>body{font-family:Segoe UI,Arial;margin:2rem}pre{background:#f1f5f9;padding:1rem;white-space:pre-wrap}</style></head>
<body>
<h2>HMS diagnostics</h2>
<p>If you can read this page, Tomcat and the JSP engine work. Add <code>?retry=1</code> to the address to re-test the database after changing the password.</p>
<pre>
Java version      : <%= esc(System.getProperty("java.version")) %>
Tomcat            : <%= esc(application.getServerInfo()) %>
Context path      : <%= esc(request.getContextPath()) %>

JSTL library      : <%= has("org.apache.taglibs.standard.tag.rt.core.IfTag") %>
Oracle driver     : <%= has("oracle.jdbc.OracleDriver") %>
My classes        : Entities=<%= has("com.hms.model.Entities") %>  DBUtil=<%= has("com.hms.util.DBUtil") %>  DbInit=<%= has("com.hms.util.DbInit") %>

<%
  String dbResult;
  Connection c = null;
  try {
    c = com.hms.util.DBUtil.get();      // same code path (and URL fallbacks) the login page uses
    Statement st = c.createStatement();
    StringBuilder sb = new StringBuilder();
    sb.append(com.hms.util.DBUtil.describe()).append("\n\n");
    ResultSet w = st.executeQuery("SELECT SYS_CONTEXT('USERENV','SESSION_USER'), SYS_CONTEXT('USERENV','SERVICE_NAME'), "
        + "NVL(SYS_CONTEXT('USERENV','CON_NAME'),'(n/a - old Oracle)') FROM dual");
    w.next();
    sb.append("Logged in as user       : ").append(w.getString(1)).append("\n");
    sb.append("Service name            : ").append(w.getString(2)).append("\n");
    sb.append("Container (CON_NAME)    : ").append(w.getString(3)).append("   (CDB$ROOT / XE = root, XEPDB1 = pluggable DB)\n");
    w.close();
    ResultSet t = st.executeQuery("SELECT COUNT(*) FROM user_tables WHERE table_name IN ('USERS','DOCTORS','PATIENTS','APPOINTMENTS','MEDICAL_RECORDS','PRESCRIPTIONS','BILLS','MEDICINES','LAB_TESTS','MESSAGES')");
    t.next();
    sb.append("HMS tables in this schema: ").append(t.getInt(1)).append(" of 10\n");
    t.close();
    try {
      ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users");
      rs.next();
      sb.append("users table rows        : ").append(rs.getInt(1)).append("\n");
      rs.close();
      sb.append("RESULT: OK - the application can use this database.");
    } catch (SQLException ex) {
      sb.append("RESULT: users table NOT visible -> ").append(ex.getMessage().trim()).append("\n");
      sb.append("        Restart Tomcat (the app creates missing tables on start-up).");
    }
    st.close();
    dbResult = sb.toString();
  } catch (Throwable e) {
    dbResult = (e instanceof SQLException ? com.hms.util.DbErrors.explain((SQLException) e) : "FAILED: " + e)
             + "\n\n" + com.hms.util.DBUtil.describe();
  } finally {
    if (c != null) { try { c.close(); } catch (Exception ignore) { } }
  }
%>
Database
<%= esc(dbResult) %>

Start-up schema check
Status : <%= esc(com.hms.util.DbInit.getStatus()) %>
<%= esc(com.hms.util.DbInit.getReport()) %>
</pre>
</body></html>
