package com.hms.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Runs once when the web application starts. Connects with the settings in db.properties and makes sure the
 * HMS tables exist in THAT schema (the one the application really uses), repairs auto-numbering, and loads the
 * sample data (including the admin login) when the core tables are empty.
 *
 * Primary keys come from sequences (hms_seq_<TABLE>) that the Java code reads itself, so no database triggers are
 * needed. Broken HMS triggers left by an earlier script are dropped (an invalid trigger makes every INSERT fail
 * with ORA-04098). Nothing else is ever dropped, and a problem here never stops the application from starting:
 * it is logged and shown on /diag.jsp and in the login / sign-up error messages.
 */
@WebListener
public class DbInit implements ServletContextListener {

    private static final String[] TABLES = { "DOCTORS", "PATIENTS", "USERS", "APPOINTMENTS", "MEDICAL_RECORDS",
        "PRESCRIPTIONS", "BILLS", "MEDICINES", "LAB_TESTS", "MESSAGES" };

    private static final Pattern CREATE_TABLE = Pattern.compile("^\\s*CREATE\\s+TABLE\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern INSERT = Pattern.compile("^\\s*INSERT\\s+INTO\\s+(\\w+)\\s*\\((.*?)\\)\\s*VALUES\\s*\\((.*)\\)\\s*$",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final StringBuilder REPORT = new StringBuilder();
    private static volatile String status = "Not run yet.";

    /** Result of the last initialisation attempt (shown on /diag.jsp). */
    public static String getStatus() {
        return status;
    }

    public static String getReport() {
        synchronized (REPORT) {
            return REPORT.toString();
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            run();
        } catch (SQLException e) {
            status = "FAILED: " + DbErrors.explain(e);
            log(status);
        } catch (Throwable t) {          // never block application start-up
            status = "FAILED: " + t;
            log("Initialisation failed: " + t);
            t.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) { }

    /** Safe to run repeatedly. */
    public static synchronized void run() throws SQLException, IOException {
        synchronized (REPORT) {
            REPORT.setLength(0);
        }
        List<String> statements = readStatements();
        try (Connection c = DBUtil.get()) {
            log("Connected: " + DBUtil.describe().replace('\n', ' '));

            // 1. tables (only the missing ones)
            Set<String> existing = existingTables(c);
            int created = 0;
            try (Statement st = c.createStatement()) {
                st.setEscapeProcessing(false);
                for (String sql : statements) {
                    Matcher m = CREATE_TABLE.matcher(sql);
                    if (m.find()) {
                        String t = m.group(1).toUpperCase();
                        if (!existing.contains(t)) {
                            exec(st, sql, "create table " + t);
                            existing.add(t);
                            created++;
                        }
                    }
                }
            }

            // 2. invalid HMS triggers would make every INSERT fail (ORA-04098): remove them
            int droppedTriggers = dropBrokenTriggers(c);

            // 3. sequences + sample data
            String seed;
            if (coreTablesEmpty(c)) {
                resetSequences(c);
                seed = loadSampleData(c, statements) ? "sample data loaded" : "sample data NOT loaded (see log)";
            } else {
                ensureSequences(c);
                seed = "existing data kept";
            }

            // 4. there must always be a way in: an empty users table gets the default admin
            boolean admin = ensureAdmin(c);

            status = "OK - tables created: " + created + ", broken triggers removed: " + droppedTriggers
                + ", " + seed + (admin ? ", default admin created." : ".");
            if (seed.contains("NOT")) {
                status = "PARTIAL - " + status;
            }
            log(status);
        }
    }

    // ------------------------------------------------------------------ steps

    private static int dropBrokenTriggers(Connection c) throws SQLException {
        List<String> bad = new ArrayList<String>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT object_name FROM user_objects WHERE object_type='TRIGGER' "
                + "AND object_name LIKE 'HMS\\_TRG\\_%' ESCAPE '\\' AND status='INVALID'");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                bad.add(rs.getString(1));
            }
        }
        for (String name : bad) {
            try (Statement st = c.createStatement()) {
                try {
                    st.execute("ALTER TRIGGER " + name + " COMPILE");      // fills USER_ERRORS with the reason
                } catch (SQLException ignore) { /* a warning, not a failure */ }
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT line, text FROM user_errors WHERE name=? AND type='TRIGGER' ORDER BY sequence")) {
                    ps.setString(1, name);
                    try (ResultSet rs = ps.executeQuery()) {
                        int n = 0;
                        while (rs.next() && n++ < 3) {
                            log("Trigger " + name + " is invalid - line " + rs.getInt(1) + ": " + rs.getString(2).trim());
                        }
                    }
                }
                st.execute("DROP TRIGGER " + name);
                log("Dropped invalid trigger " + name + " (ids now come from sequences).");
            }
        }
        return bad.size();
    }

    private static boolean coreTablesEmpty(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT (SELECT COUNT(*) FROM users) + (SELECT COUNT(*) FROM doctors) + (SELECT COUNT(*) FROM patients) FROM dual");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1) == 0;
        }
    }

    /** Empty install: drop leftover HMS sequences and start every one at 1, so the sample rows get ids 1,2,3... */
    private static void resetSequences(Connection c) throws SQLException {
        Set<String> have = existingSequences(c);
        for (String t : TABLES) {
            if (have.contains("HMS_SEQ_" + t)) {
                try (Statement st = c.createStatement()) {
                    st.execute("DROP SEQUENCE hms_seq_" + t);
                }
            }
            DBUtil.createSequence(c, t, true);
        }
        log("Sequences reset to 1.");
    }

    /** Existing data: create only the sequences that are missing, starting after the highest id in use. */
    private static void ensureSequences(Connection c) throws SQLException {
        Set<String> have = existingSequences(c);
        for (String t : TABLES) {
            if (!have.contains("HMS_SEQ_" + t)) {
                DBUtil.createSequence(c, t, false);
                log("Created missing sequence hms_seq_" + t);
            }
        }
    }

    private static boolean loadSampleData(Connection c, List<String> statements) {
        try {
            c.setAutoCommit(false);
            int rows = 0;
            try (Statement st = c.createStatement()) {
                st.setEscapeProcessing(false);
                for (String sql : statements) {
                    String withId = withId(sql);
                    if (withId != null) {
                        st.executeUpdate(withId);
                        rows++;
                    }
                }
            }
            c.commit();
            log("Sample data loaded (" + rows + " rows).");
            return true;
        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException ignore) { /* nothing more to do */ }
            log("Sample data NOT loaded: " + DbErrors.explain(e));
            return false;
        } finally {
            try {
                c.setAutoCommit(true);
            } catch (SQLException ignore) { /* connection is closed right after */ }
        }
    }

    /** INSERT INTO t (a,b) VALUES (x,y)  ->  INSERT INTO t (id,a,b) VALUES (hms_seq_T.NEXTVAL,x,y); null for non-INSERTs. */
    static String withId(String sql) {
        Matcher m = INSERT.matcher(sql);
        if (!m.matches()) {
            return null;
        }
        String t = m.group(1);
        return "INSERT INTO " + t + " (id, " + m.group(2) + ") VALUES (hms_seq_" + t.toUpperCase() + ".NEXTVAL, " + m.group(3) + ")";
    }

    private static boolean ensureAdmin(Connection c) throws SQLException {
        int n;
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            n = rs.getInt(1);
        }
        if (n > 0) {
            return false;
        }
        long id = DBUtil.nextId(c, "USERS");
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO users (id, username, password_hash, full_name, email, role, doctor_id) VALUES (?,?,?,?,?,?,NULL)")) {
            ps.setLong(1, id);
            ps.setString(2, "admin");
            ps.setString(3, Security.sha256("admin123"));
            ps.setString(4, "System Administrator");
            ps.setString(5, "admin@medicare.example");
            ps.setString(6, "admin");
            ps.executeUpdate();
        }
        log("users table was empty: default admin / admin123 created.");
        return true;
    }

    // ------------------------------------------------------------------ helpers

    private static Set<String> existingTables(Connection c) throws SQLException {
        Set<String> wanted = new HashSet<String>();
        for (String t : TABLES) {
            wanted.add(t);
        }
        Set<String> found = new HashSet<String>();
        try (PreparedStatement ps = c.prepareStatement("SELECT table_name FROM user_tables");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString(1).toUpperCase();
                if (wanted.contains(name)) {
                    found.add(name);
                }
            }
        }
        return found;
    }

    private static Set<String> existingSequences(Connection c) throws SQLException {
        Set<String> found = new HashSet<String>();
        try (PreparedStatement ps = c.prepareStatement("SELECT sequence_name FROM user_sequences WHERE sequence_name LIKE 'HMS\\_SEQ\\_%' ESCAPE '\\'");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                found.add(rs.getString(1).toUpperCase());
            }
        }
        return found;
    }

    private static void exec(Statement st, String sql, String what) throws SQLException {
        try {
            st.execute(sql);
            log("OK: " + what);
        } catch (SQLException e) {
            log("FAILED: " + what + " -> " + e.getMessage().trim());
            throw e;
        }
    }

    /** Splits schema.sql into statements; a line containing only "/" ends a statement, "--" lines are comments. */
    static List<String> readStatements() throws IOException {
        List<String> out = new ArrayList<String>();
        try (InputStream in = DbInit.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (in == null) {
                throw new IOException("schema.sql was not found on the classpath (src/main/resources).");
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder cur = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                String t = line.trim();
                if (t.equals("/")) {
                    if (cur.toString().trim().length() > 0) {
                        out.add(cur.toString().trim());
                    }
                    cur.setLength(0);
                } else if (!t.startsWith("--")) {
                    cur.append(line).append('\n');
                }
            }
            if (cur.toString().trim().length() > 0) {
                out.add(cur.toString().trim());
            }
        }
        return out;
    }

    private static void log(String msg) {
        System.out.println("[HMS] " + msg);
        synchronized (REPORT) {
            REPORT.append(msg).append('\n');
        }
    }
}
