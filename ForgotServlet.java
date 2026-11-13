package com.hms.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.hms.util.DBUtil;
import com.hms.util.DbErrors;
import com.hms.util.Security;

/**
 * Demo password reset: the user proves identity with username + registered e-mail.
 * (A production system would e-mail a one-time link instead.)
 */
@WebServlet("/forgot")
public class ForgotServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String VIEW = "/WEB-INF/jsp/public/forgot.jsp";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = t(req.getParameter("username"));
        String email = t(req.getParameter("email"));
        String pw = t(req.getParameter("password"));
        String confirm = t(req.getParameter("confirm"));
        String error = null;
        if (username.isEmpty() || email.isEmpty() || pw.isEmpty()) {
            error = "All fields are required.";
        } else if (pw.length() < 6) {
            error = "Password must be at least 6 characters.";
        } else if (!pw.equals(confirm)) {
            error = "Passwords do not match.";
        }
        try {
            if (error == null) {
                Map<String, String> u = DBUtil.one("SELECT id, role FROM users WHERE LOWER(username)=LOWER(?) AND LOWER(email)=LOWER(?)", username, email);
                if (u == null) {
                    error = "No account matches that username and e-mail.";
                } else if ("admin".equals(u.get("role"))) {
                    // the admin e-mail is not secret, so this demo reset must never work on admin accounts
                    error = "Administrator passwords cannot be reset here. Ask another administrator (Staff Users) or the database owner.";
                } else {
                    DBUtil.update("UPDATE users SET password_hash=? WHERE id=?", Security.sha256(pw), u.get("id"));
                    req.getSession().setAttribute("flash", "Password updated. You can log in now.");
                    resp.sendRedirect(req.getContextPath() + "/login");
                    return;
                }
            }
        } catch (SQLException e) {
            getServletContext().log("Forgot-password database error", e);
            error = DbErrors.explain(e);
        }
        req.setAttribute("error", error);
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    private static String t(String s) {
        return s == null ? "" : s.trim();
    }
}
