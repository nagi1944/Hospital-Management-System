package com.hms.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.hms.model.AppUser;
import com.hms.model.Entities;
import com.hms.model.Entities.Field;
import com.hms.model.Entities.Module;
import com.hms.util.DBUtil;
import com.hms.util.Security;

/**
 * One servlet for all CRUD modules: /module?m=patients&action=list|new|edit|view|save|delete|pay
 * Access rules come from the Module definition in Entities.
 */
@WebServlet("/module")
public class ModuleServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String LIST = "/WEB-INF/jsp/module/list.jsp";
    private static final String FORM = "/WEB-INF/jsp/module/form.jsp";
    private static final String VIEW = "/WEB-INF/jsp/module/view.jsp";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch(req, resp, false);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dispatch(req, resp, true);
    }

    private void dispatch(HttpServletRequest req, HttpServletResponse resp, boolean post) throws ServletException, IOException {
        AppUser u = (AppUser) req.getSession().getAttribute("user");
        Module m = Entities.get(req.getParameter("m"));
        if (m == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!m.canAccess(u.getRole())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String action = req.getParameter("action");
        if (action == null || action.isEmpty()) {
            action = "list";
        }
        boolean canWrite = m.canWrite(u.getRole());
        boolean canCreate = canWrite && m.isAllowCreate();
        boolean canEdit = canWrite && m.isAllowEdit();
        // doctors only see / change rows that belong to them
        String scopeCol = ("doctor".equals(u.getRole()) && m.getDoctorCol() != null) ? m.getDoctorCol() : null;
        String scopeVal = u.getDoctorId();

        req.setAttribute("module", m);
        req.setAttribute("canWrite", canWrite);
        req.setAttribute("canCreate", canCreate);
        req.setAttribute("canEdit", canEdit);
        req.setAttribute("hiddenCol", scopeCol == null ? "" : scopeCol);

        try {
            if (post) {
                if (!canWrite) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                if ("save".equals(action)) {
                    save(req, resp, m, scopeCol, scopeVal);
                } else if ("delete".equals(action)) {
                    delete(req, resp, m, u, scopeCol, scopeVal);
                } else if ("pay".equals(action) && "bills".equals(m.getKey())) {
                    pay(req, resp, m);
                } else {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                }
                return;
            }
            switch (action) {
                case "list":
                    list(req, resp, m, scopeCol, scopeVal);
                    break;
                case "view": {
                    Map<String, String> row = loadRow(req, m, scopeCol, scopeVal);
                    if (row == null) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                    req.setAttribute("row", row);
                    req.getRequestDispatcher(VIEW).forward(req, resp);
                    break;
                }
                case "new": {
                    if (!canCreate) {
                        resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    Map<String, String> row = new LinkedHashMap<String, String>();
                    for (Field f : m.getFields()) {
                        row.put(f.getName(), f.getDef());
                    }
                    showForm(req, resp, m, row);
                    break;
                }
                case "edit": {
                    if (!canEdit) {
                        resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    Map<String, String> row = loadRow(req, m, scopeCol, scopeVal);
                    if (row == null) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                    row.put("password_hash", "");
                    showForm(req, resp, m, row);
                    break;
                }
                default:
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new ServletException("Database error: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------- list

    private void list(HttpServletRequest req, HttpServletResponse resp, Module m, String scopeCol, String scopeVal)
            throws SQLException, ServletException, IOException {
        String q = req.getParameter("q") == null ? "" : req.getParameter("q").trim();
        StringBuilder sql = selectClause(m);
        sql.append(" FROM ").append(m.getTable()).append(" t WHERE 1=1");
        List<Object> p = new ArrayList<Object>();
        if (scopeCol != null) {
            sql.append(" AND t.").append(scopeCol).append("=?");
            p.add(scopeVal);
        }
        if (!q.isEmpty()) {
            StringBuilder s = new StringBuilder();
            for (Field f : m.getFields()) {
                if (!f.isSearch()) {
                    continue;
                }
                if (s.length() > 0) {
                    s.append(" OR ");
                }
                if ("ref".equals(f.getType())) {
                    s.append("UPPER((SELECT ").append(f.getRefLabel()).append(" FROM ").append(f.getRefTable())
                     .append(" x WHERE x.id=t.").append(f.getName()).append(")) LIKE UPPER(?)");
                } else {
                    s.append("UPPER(t.").append(f.getName()).append(") LIKE UPPER(?)");
                }
                p.add("%" + q + "%");
            }
            if (s.length() > 0) {
                sql.append(" AND (").append(s).append(")");
            }
        }
        sql.append(" ORDER BY ").append(m.getOrderBy());
        List<Map<String, String>> rows = DBUtil.query(sql.toString(), p.toArray());

        if ("medicines".equals(m.getKey())) {
            String today = LocalDate.now().toString();
            for (Map<String, String> r : rows) {
                String flag = "";
                String exp = r.get("expiry_date");
                if (exp != null && exp.compareTo(today) < 0) {
                    flag = "Expired";
                } else if (toInt(r.get("quantity")) <= toInt(r.get("reorder_level"))) {
                    flag = "Low stock";
                }
                r.put("flag", flag);
            }
        }
        req.setAttribute("rows", rows);
        req.setAttribute("q", q);
        req.getRequestDispatcher(LIST).forward(req, resp);
    }

    // ---------------------------------------------------------------- save

    private void save(HttpServletRequest req, HttpServletResponse resp, Module m, String scopeCol, String scopeVal)
            throws SQLException, ServletException, IOException {
        String id = t(req.getParameter("id"));
        boolean isNew = id.isEmpty();
        if ((isNew && !m.isAllowCreate()) || (!isNew && !m.isAllowEdit())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!isNew) {
            if (!id.matches("\\d+") || loadById(m, id, scopeCol, scopeVal) == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }
        Map<String, String> form = new LinkedHashMap<String, String>();   // redisplayed on error
        Map<String, Object> vals = new LinkedHashMap<String, Object>();   // saved to database
        List<String> errors = new ArrayList<String>();
        form.put("id", id);

        for (Field f : m.getFields()) {
            String v = t(req.getParameter(f.getName()));
            if (f.getName().equals(scopeCol)) {
                v = scopeVal == null ? "" : scopeVal;
            }
            if ("password".equals(f.getType())) {
                form.put(f.getName(), "");
                if (v.isEmpty()) {
                    if (isNew) {
                        errors.add(f.getLabel() + " is required.");
                    }
                } else if (v.length() < 6) {
                    errors.add("Password must be at least 6 characters.");
                } else {
                    vals.put(f.getName(), Security.sha256(v));
                }
                continue;
            }
            form.put(f.getName(), v);
            if (v.isEmpty() && f.isToday()) {
                v = LocalDate.now().toString();
                form.put(f.getName(), v);
            }
            if (v.isEmpty()) {
                if (f.isRequired()) {
                    errors.add(f.getLabel() + " is required.");
                }
                vals.put(f.getName(), null);
                continue;
            }
            String err = validate(f, v);
            if (err != null) {
                errors.add(err);
            }
            vals.put(f.getName(), err == null ? typed(f, v) : v);
        }

        if ("bills".equals(m.getKey())) {
            double total = num(vals.get("consultation")) + num(vals.get("medicine"))
                         + num(vals.get("lab")) + num(vals.get("other"));
            vals.put("total", java.math.BigDecimal.valueOf(total));
            form.put("total", String.valueOf(total));
        }

        // one doctor cannot have two active appointments at the same date and time
        if (errors.isEmpty() && "appointments".equals(m.getKey()) && !"Cancelled".equals(vals.get("status"))) {
            String sql = "SELECT COUNT(*) FROM appointments WHERE doctor_id=? AND appt_date=? AND appt_time=? AND status<>'Cancelled'";
            List<Object> p = new ArrayList<Object>();
            p.add(vals.get("doctor_id"));
            p.add(vals.get("appt_date"));
            p.add(vals.get("appt_time"));
            if (!isNew) {
                sql += " AND id<>?";
                p.add(id);
            }
            if (toInt(DBUtil.scalar(sql, p.toArray())) > 0) {
                errors.add("This doctor already has an appointment at that date and time. Choose another slot.");
            }
        }

        if (errors.isEmpty()) {
            try {
                if (isNew) {
                    StringBuilder cols = new StringBuilder();
                    StringBuilder marks = new StringBuilder();
                    for (String c : vals.keySet()) {
                        if (cols.length() > 0) {
                            cols.append(",");
                            marks.append(",");
                        }
                        cols.append(c);
                        marks.append("?");
                    }
                    List<Object> ins = new ArrayList<Object>();
                    ins.add(DBUtil.nextId(m.getTable()));
                    ins.addAll(vals.values());
                    DBUtil.update("INSERT INTO " + m.getTable() + " (id," + cols + ") VALUES (?," + marks + ")",
                        ins.toArray());
                } else {
                    StringBuilder set = new StringBuilder();
                    List<Object> p = new ArrayList<Object>();
                    for (Map.Entry<String, Object> e : vals.entrySet()) {
                        if (set.length() > 0) {
                            set.append(",");
                        }
                        set.append(e.getKey()).append("=?");
                        p.add(e.getValue());
                    }
                    p.add(id);
                    DBUtil.update("UPDATE " + m.getTable() + " SET " + set + " WHERE id=?", p.toArray());
                }
                req.getSession().setAttribute("flash", m.getSingular() + (isNew ? " added." : " updated."));
                resp.sendRedirect(req.getContextPath() + "/module?m=" + m.getKey());
                return;
            } catch (SQLException e) {
                if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                    errors.add("Could not save: a value is duplicated (for example the username) or a linked record is missing.");
                } else if (e.getErrorCode() == 12899 || e.getErrorCode() == 1438 || e.getErrorCode() == 1401
                        || (e.getSQLState() != null && e.getSQLState().startsWith("22"))) {
                    errors.add("A value is too long or a number is too large for its field.");
                } else {
                    throw e;
                }
            }
        }
        req.setAttribute("errors", errors);
        showForm(req, resp, m, form);
    }

    /** Converts the validated text into the right Java type so Oracle never has to guess. */
    private static Object typed(Field f, String v) {
        String type = f.getType();
        if ("number".equals(type)) {
            return new java.math.BigDecimal(v);
        } else if ("ref".equals(type)) {
            return Long.valueOf(v);
        } else if ("date".equals(type)) {
            return java.sql.Date.valueOf(v);
        } else if ("time".equals(type)) {
            return v.substring(0, 5);                // stored as HH:mm text
        }
        return v;
    }

    private String validate(Field f, String v) {
        String type = f.getType();
        if ("number".equals(type)) {
            try {
                if (new java.math.BigDecimal(v).signum() < 0) {
                    return f.getLabel() + " cannot be negative.";
                }
            } catch (NumberFormatException e) {
                return f.getLabel() + " must be a number.";
            }
        } else if ("date".equals(type)) {
            try {
                LocalDate.parse(v);
            } catch (Exception e) {
                return f.getLabel() + " must be a valid date.";
            }
        } else if ("time".equals(type)) {
            if (!v.matches("\\d{2}:\\d{2}(:\\d{2})?")) {
                return f.getLabel() + " must be a valid time.";
            }
        } else if ("email".equals(type)) {
            if (v.indexOf('@') < 1 || v.length() > 100) {
                return f.getLabel() + " must be a valid e-mail address.";
            }
        } else if ("select".equals(type)) {
            boolean ok = false;
            for (String o : f.getOptions()) {
                if (o.equals(v)) {
                    ok = true;
                }
            }
            if (!ok) {
                return f.getLabel() + " has an invalid value.";
            }
        } else if ("ref".equals(type)) {
            if (!v.matches("\\d+")) {
                return f.getLabel() + " is invalid.";
            }
        } else if ("textarea".equals(type)) {
            if (v.length() > 4000) {
                return f.getLabel() + " is too long (max 4000 characters).";
            }
        } else if (v.length() > 255) {
            return f.getLabel() + " is too long (max 255 characters).";
        }
        return null;
    }

    // ------------------------------------------------------- delete / pay

    private void delete(HttpServletRequest req, HttpServletResponse resp, Module m, AppUser u, String scopeCol, String scopeVal)
            throws SQLException, IOException {
        String id = t(req.getParameter("id"));
        if (!id.matches("\\d+") || loadById(m, id, scopeCol, scopeVal) == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if ("users".equals(m.getKey()) && id.equals(u.getId())) {
            req.getSession().setAttribute("flash", "You cannot delete your own account.");
            req.getSession().setAttribute("flashType", "error");
        } else {
            try {
                DBUtil.update("DELETE FROM " + m.getTable() + " WHERE id=?", id);
                req.getSession().setAttribute("flash", m.getSingular() + " deleted.");
            } catch (SQLException e) {
                if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                    req.getSession().setAttribute("flash", "Cannot delete: this record is used by other records.");
                    req.getSession().setAttribute("flashType", "error");
                } else {
                    throw e;
                }
            }
        }
        resp.sendRedirect(req.getContextPath() + "/module?m=" + m.getKey());
    }

    private void pay(HttpServletRequest req, HttpServletResponse resp, Module m) throws SQLException, IOException {
        String id = t(req.getParameter("id"));
        if (!id.matches("\\d+")) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        DBUtil.update("UPDATE bills SET status='Paid' WHERE id=?", id);
        req.getSession().setAttribute("flash", "Bill #" + id + " marked as paid.");
        resp.sendRedirect(req.getContextPath() + "/module?m=" + m.getKey());
    }

    // ------------------------------------------------------------ helpers

    private void showForm(HttpServletRequest req, HttpServletResponse resp, Module m, Map<String, String> row)
            throws SQLException, ServletException, IOException {
        Map<String, List<Map<String, String>>> refs = new LinkedHashMap<String, List<Map<String, String>>>();
        for (Field f : m.getFields()) {
            if ("ref".equals(f.getType())) {
                refs.put(f.getName(), DBUtil.query("SELECT id, " + f.getRefLabel() + " AS label FROM "
                    + f.getRefTable() + " ORDER BY id"));
            }
        }
        req.setAttribute("refs", refs);
        req.setAttribute("row", row);
        req.getRequestDispatcher(FORM).forward(req, resp);
    }

    /** SELECT t.*, plus a "<field>_label" column for every reference field. */
    private StringBuilder selectClause(Module m) {
        StringBuilder sql = new StringBuilder("SELECT t.*");
        for (Field f : m.getFields()) {
            if ("ref".equals(f.getType())) {
                sql.append(", (SELECT ").append(f.getRefLabel()).append(" FROM ").append(f.getRefTable())
                   .append(" x WHERE x.id=t.").append(f.getName()).append(") AS ").append(f.getLabelKey());
            }
        }
        return sql;
    }

    private Map<String, String> loadRow(HttpServletRequest req, Module m, String scopeCol, String scopeVal) throws SQLException {
        String id = t(req.getParameter("id"));
        if (!id.matches("\\d+")) {
            return null;
        }
        return loadById(m, id, scopeCol, scopeVal);
    }

    private Map<String, String> loadById(Module m, String id, String scopeCol, String scopeVal) throws SQLException {
        StringBuilder sql = selectClause(m);
        sql.append(" FROM ").append(m.getTable()).append(" t WHERE t.id=?");
        List<Object> p = new ArrayList<Object>();
        p.add(id);
        if (scopeCol != null) {
            sql.append(" AND t.").append(scopeCol).append("=?");
            p.add(scopeVal);
        }
        return DBUtil.one(sql.toString(), p.toArray());
    }

    private static String t(String s) {
        return s == null ? "" : s.trim();
    }

    private static int toInt(String s) {
        try {
            return (int) Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private static double num(Object o) {
        try {
            return o == null ? 0 : Double.parseDouble(o.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
