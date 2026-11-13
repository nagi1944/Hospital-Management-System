package com.hms.filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import com.hms.model.AppUser;

/** UTF-8 encoding, CSRF check on every POST, and login / role check on protected pages. */
@WebFilter("/*")
public class SecurityFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) { }

    @Override
    public void destroy() { }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        request.setCharacterEncoding("UTF-8");

        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");

        String path = request.getServletPath();
        HttpSession session = request.getSession(false);

        // CSRF protection for every state-changing request
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            String expected = session == null ? null : (String) session.getAttribute("csrf");
            String actual = request.getParameter("csrf");
            if (expected == null || !expected.equals(actual)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing security token. Reload the page and try again.");
                return;
            }
        }

        boolean protectedPath = path.equals("/module") || path.equals("/dashboard") || path.equals("/reports");
        if (protectedPath) {
            AppUser user = session == null ? null : (AppUser) session.getAttribute("user");
            if (user == null) {
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }
            if (path.equals("/reports") && !"admin".equals(user.getRole())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            // do not let the browser show protected pages after logout
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
        }
        chain.doFilter(req, res);
    }
}
