package com.hms.servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.hms.util.DBUtil;
import com.hms.util.DbErrors;
import com.hms.util.Security;

/**
 * Staff self-registration. Only the "doctor" and "receptionist" roles can be
 * created here; admin accounts can only be created by an existing admin.
 * A doctor sign-up also creates the linked row in the doctors table.
 */
@WebServlet("/signup")
public class SignupServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String VIEW = "/WEB-INF/jsp/public/signup.jsp";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String fullName = t(req.getParameter("full_name"));
        String email = t(req.getParameter("email"));
        String username = t(req.getParameter("username"));
        String role = t(req.getParameter("role"));
        String pw = t(req.getParameter("password"));
        String confirm = t(req.getParameter("confirm"));
        String specialization = t(req.getParameter("specialization"));
        String phone = t(req.getParameter("phone"));

        List<String> errors = new ArrayList<String>();
        if (fullName.isEmpty() || fullName.length() > 100) {
            errors.add("Enter your full name (max 100 characters).");
        }
        if (email.indexOf('@') < 1 || email.length() > 100) {
            errors.add("Enter a valid e-mail address.");
        }
        if (!username.matches("[A-Za-z0-9_.]{4,30}")) {
            errors.add("Username must be 4-30 characters: letters, digits, dot or underscore.");
        }
        if (pw.length() < 6) {
            errors.add("Password must be at least 6 characters.");
        } else if (!pw.equals(confirm)) {
            errors.add("Passwords do not match.");
        }
        boolean isDoctor = "doctor".equals(role);
        if (!isDoctor && !"receptionist".equals(role)) {
            errors.add("Select Doctor or Receptionist.");
        }
        if (isDoctor && (specialization.isEmpty() || phone.isEmpty())) {
            errors.add("Doctors must enter a specialization and phone number.");
        }

        try {
            if (errors.isEmpty() && !"0".equals(DBUtil.scalar("SELECT COUNT(*) FROM users WHERE LOWER(username)=LOWER(?)", username))) {
                errors.add("That username is already taken.");
            }
            if (errors.isEmpty()) {
                create(fullName, email, username, role, Security.sha256(pw), specialization, phone);
                req.getSession().setAttribute("flash", "Account created. Please log in.");
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1 || (e.getSQLState() != null && e.getSQLState().startsWith("23"))) {
                errors.add("That username is already taken.");
            } else {
                getServletContext().log("Signup database error", e);
                errors.add(DbErrors.explain(e));
            }
        }
        req.setAttribute("errors", errors);
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    private void create(String fullName, String email, String username, String role, String hash,
                        String specialization, String phone) throws SQLException {
        try (Connection c = DBUtil.get()) {
            c.setAutoCommit(false);
            try {
                Long doctorId = null;
                if ("doctor".equals(role)) {
                    doctorId = Long.valueOf(DBUtil.nextId(c, "doctors"));
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO doctors (id, name, specialization, phone, email, fee) VALUES (?,?,?,?,?,0)")) {
                        ps.setLong(1, doctorId.longValue());
                        ps.setString(2, fullName);
                        ps.setString(3, specialization);
                        ps.setString(4, phone);
                        ps.setString(5, email);
                        ps.executeUpdate();
                    }
                }
                long userId = DBUtil.nextId(c, "users");
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO users (id, username, password_hash, full_name, email, role, doctor_id) VALUES (?,?,?,?,?,?,?)")) {
                    ps.setLong(1, userId);
                    ps.setString(2, username);
                    ps.setString(3, hash);
                    ps.setString(4, fullName);
                    ps.setString(5, email);
                    ps.setString(6, role);
                    if (doctorId == null) {
                        ps.setNull(7, java.sql.Types.NUMERIC);
                    } else {
                        ps.setLong(7, doctorId.longValue());
                    }
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private static String t(String s) {
        return s == null ? "" : s.trim();
    }
}
