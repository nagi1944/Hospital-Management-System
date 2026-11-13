package com.hms.servlet;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.hms.model.AppUser;
import com.hms.util.DBUtil;

/** Admin / receptionist dashboard and doctor dashboard (same URL, different data). */
@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String UPCOMING =
        "SELECT a.id, p.name AS patient, d.name AS doctor, a.appt_date, a.appt_time, a.status "
      + "FROM appointments a JOIN patients p ON p.id=a.patient_id JOIN doctors d ON d.id=a.doctor_id "
      + "WHERE a.appt_date>=TRUNC(SYSDATE) AND a.status='Scheduled' ";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        AppUser u = (AppUser) req.getSession().getAttribute("user");
        try {
            if ("doctor".equals(u.getRole())) {
                String d = u.getDoctorId();
                req.setAttribute("isDoctor", true);
                req.setAttribute("sToday", DBUtil.scalar("SELECT COUNT(*) FROM appointments WHERE doctor_id=? AND appt_date=TRUNC(SYSDATE) AND status='Scheduled'", d));
                req.setAttribute("sPatients", DBUtil.scalar("SELECT COUNT(DISTINCT patient_id) FROM appointments WHERE doctor_id=?", d));
                req.setAttribute("sLabs", DBUtil.scalar("SELECT COUNT(*) FROM lab_tests WHERE doctor_id=? AND status='Pending'", d));
                req.setAttribute("sRecords", DBUtil.scalar("SELECT COUNT(*) FROM medical_records WHERE doctor_id=?", d));
                req.setAttribute("upcoming", DBUtil.query("SELECT * FROM (" + UPCOMING + "AND a.doctor_id=? ORDER BY a.appt_date, a.appt_time) WHERE ROWNUM<=8", d));
            } else {
                req.setAttribute("isDoctor", false);
                req.setAttribute("sPatients", DBUtil.scalar("SELECT COUNT(*) FROM patients"));
                req.setAttribute("sDoctors", DBUtil.scalar("SELECT COUNT(*) FROM doctors"));
                req.setAttribute("sToday", DBUtil.scalar("SELECT COUNT(*) FROM appointments WHERE appt_date=TRUNC(SYSDATE) AND status<>'Cancelled'"));
                req.setAttribute("sPending", DBUtil.scalar("SELECT COUNT(*) FROM bills WHERE status='Pending'"));
                req.setAttribute("sRevenue", DBUtil.scalar("SELECT COALESCE(SUM(total),0) FROM bills WHERE status='Paid'"));
                req.setAttribute("sLow", DBUtil.scalar("SELECT COUNT(*) FROM medicines WHERE quantity<=reorder_level OR expiry_date<TRUNC(SYSDATE)"));
                req.setAttribute("upcoming", DBUtil.query("SELECT * FROM (" + UPCOMING + "ORDER BY a.appt_date, a.appt_time) WHERE ROWNUM<=8"));
            }
        } catch (SQLException e) {
            throw new ServletException("Database error: " + e.getMessage(), e);
        }
        req.getRequestDispatcher("/WEB-INF/jsp/dashboard.jsp").forward(req, resp);
    }
}
