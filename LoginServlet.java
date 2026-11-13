package com.hms.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import com.hms.model.AppUser;
import com.hms.util.DBUtil;
import com.hms.util.DbErrors;
import com.hms.util.Security;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String VIEW = "/WEB-INF/jsp/public/login.jsp";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = req.getSession(false);
        if (s != null && s.getAttribute("user") != null) {
            resp.sendRedirect(req.getContextPath() + "/dashboard");
            return;
        }
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = trim(req.getParameter("username"));
        String password = trim(req.getParameter("password"));
        String role = trim(req.getParameter("role"));
        if (username.isEmpty() || password.isEmpty() || role.isEmpty()) {
            fail(req, resp, "Enter username, password and select a role.");
            return;
        }
        try {
            Map<String, String> u = DBUtil.one(
                "SELECT id, username, full_name, role, doctor_id FROM users WHERE LOWER(username)=LOWER(?) AND password_hash=? AND role=?",
                username, Security.sha256(password), role);
            if (u == null) {
                fail(req, resp, "Invalid username, password or role.");
                return;
            }
            req.getSession(true);   // make sure a session exists before rotating its id
            req.changeSessionId();  // prevent session fixation
            HttpSession session = req.getSession();
            session.setAttribute("user", new AppUser(u.get("id"), u.get("username"), u.get("full_name"),
                u.get("role"), u.get("doctor_id")));
            resp.sendRedirect(req.getContextPath() + "/dashboard");
        } catch (SQLException e) {
            getServletContext().log("Login database error", e);
            fail(req, resp, DbErrors.explain(e));
        }
    }

    private void fail(HttpServletRequest req, HttpServletResponse resp, String msg) throws ServletException, IOException {
        req.setAttribute("error", msg);
        req.setAttribute("selectedRole", req.getParameter("role"));
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
