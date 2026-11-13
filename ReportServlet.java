package com.hms.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.hms.util.DBUtil;

/** Admin reports (access restricted to admin by SecurityFilter). */
@WebServlet("/reports")
public class ReportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            List<Map<String, String>> revenue = DBUtil.query(
                "SELECT * FROM (SELECT TO_CHAR(bill_date,'YYYY-MM') AS month, SUM(total) AS amount FROM bills "
              + "WHERE status='Paid' GROUP BY TO_CHAR(bill_date,'YYYY-MM') ORDER BY month DESC) WHERE ROWNUM<=6");
            addPercent(revenue, "amount");
            List<Map<String, String>> doctors = DBUtil.query(
                "SELECT * FROM (SELECT d.name, d.specialization, COUNT(a.id) AS cnt FROM doctors d "
              + "LEFT JOIN appointments a ON a.doctor_id=d.id GROUP BY d.id, d.name, d.specialization "
              + "ORDER BY cnt DESC) WHERE ROWNUM<=5");
            addPercent(doctors, "cnt");

            req.setAttribute("revenue", revenue);
            req.setAttribute("topDoctors", doctors);
            req.setAttribute("apptStatus", DBUtil.query("SELECT status, COUNT(*) AS cnt FROM appointments GROUP BY status"));
            req.setAttribute("genders", DBUtil.query("SELECT gender, COUNT(*) AS cnt FROM patients GROUP BY gender"));
            req.setAttribute("stockAlerts", DBUtil.query(
                "SELECT name, quantity, reorder_level, expiry_date, "
              + "CASE WHEN expiry_date<TRUNC(SYSDATE) THEN 'Expired' ELSE 'Low stock' END AS flag "
              + "FROM medicines WHERE quantity<=reorder_level OR expiry_date<TRUNC(SYSDATE) ORDER BY name"));
            req.setAttribute("paidTotal", DBUtil.scalar("SELECT COALESCE(SUM(total),0) FROM bills WHERE status='Paid'"));
            req.setAttribute("pendingTotal", DBUtil.scalar("SELECT COALESCE(SUM(total),0) FROM bills WHERE status='Pending'"));
        } catch (SQLException e) {
            throw new ServletException("Database error: " + e.getMessage(), e);
        }
        req.getRequestDispatcher("/WEB-INF/jsp/reports.jsp").forward(req, resp);
    }

    /** Adds a "pct" entry (0-100) to every row so the JSP can draw simple bars. */
    private static void addPercent(List<Map<String, String>> rows, String key) {
        double max = 0;
        for (Map<String, String> r : rows) {
            max = Math.max(max, Double.parseDouble(r.get(key)));
        }
        for (Map<String, String> r : rows) {
            double v = Double.parseDouble(r.get(key));
            r.put("pct", String.valueOf(max <= 0 ? 0 : Math.round(v * 100 / max)));
        }
    }
}
