package com.hms.servlet;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.hms.util.DBUtil;

@WebServlet("/contact")
public class ContactServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String VIEW = "/WEB-INF/jsp/public/contact.jsp";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String name = t(req.getParameter("name"));
        String email = t(req.getParameter("email"));
        String subject = t(req.getParameter("subject"));
        String message = t(req.getParameter("message"));
        if (name.isEmpty() || email.isEmpty() || subject.isEmpty() || message.isEmpty() || email.indexOf('@') < 1) {
            req.setAttribute("error", "Please fill in all fields with a valid e-mail address.");
            req.getRequestDispatcher(VIEW).forward(req, resp);
            return;
        }
        if (message.length() > 3000) {
            message = message.substring(0, 3000);
        }
        try {
            DBUtil.update("INSERT INTO messages (id, name, email, subject, message) VALUES (?,?,?,?,?)",
                DBUtil.nextId("messages"), name, email, subject, message);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.getSession().setAttribute("flash", "Thank you! Your message has been sent.");
        resp.sendRedirect(req.getContextPath() + "/contact");
    }

    private static String t(String s) {
        return s == null ? "" : s.trim();
    }
}
