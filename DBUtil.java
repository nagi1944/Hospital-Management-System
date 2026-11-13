package com.hms.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Small JDBC helper. All queries use PreparedStatement (no SQL injection). */
public class DBUtil {

    private static final Object LOCK = new Object();
    private static final long AUTH_PAUSE_MS = 30000L;

    private static volatile boolean loaded = false;
    private static String url;
    private static String user;
    private static String password;
    private static final List<String> FALLBACKS = new ArrayList<String>();
    private static volatile String activeUrl;
    private static volatile long authBlockedUntil = 0L;
    private static volatile SQLException authBlockError;

    private DBUtil() { }

    /** Environment variable / JVM property override, else the value from db.properties. */
    private static String setting(Properties p, String key, String envName, String sysName) {
        String v = System.getProperty(sysName);
        if (v == null || v.trim().isEmpty()) {
            v = System.getenv(envName);
        }
        if (v == null || v.trim().isEmpty()) {
            v = p.getProperty(key);
        }
        return v == null ? null : v.trim();
    }

    private static void load() throws SQLException {
        if (loaded) {
            return;
        }
        synchronized (LOCK) {
            if (loaded) {
                return;
            }
            Properties p = new Properties();
            try (InputStream in = DBUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (in == null) {
                    throw new SQLException("db.properties was not found (it must be in src/main/resources).", "HMS02");
                }
                p.load(in);
            } catch (java.io.IOException e) {
                throw new SQLException("Cannot read db.properties: " + e, "HMS02", e);
            }
            String driver = p.getProperty("db.driver", "oracle.jdbc.OracleDriver").trim();
            try {
                Class.forName(driver);
            } catch (ClassNotFoundException e) {
                throw new SQLException("The Oracle JDBC driver (" + driver + ") is not on the classpath. "
                    + "Its jar (ojdbc8) must be inside WEB-INF/lib: in Eclipse use Maven > Update Project (Force Update), "
                    + "check the pom.xml has no errors, then Server > Clean and republish.", "HMS01", e);
            }
            url = setting(p, "db.url", "HMS_DB_URL", "hms.db.url");
            user = setting(p, "db.user", "HMS_DB_USER", "hms.db.user");
            password = setting(p, "db.password", "HMS_DB_PASSWORD", "hms.db.password");
            if (password == null) {
                password = "";
            }
            if (url == null || url.isEmpty() || user == null || user.isEmpty()) {
                throw new SQLException("db.url and db.user must be set in db.properties.", "HMS02");
            }
            FALLBACKS.clear();
            String fb = p.getProperty("db.url.fallbacks");
            if (fb != null) {
                for (String f : fb.split(",")) {
                    if (!f.trim().isEmpty()) {
                        FALLBACKS.add(f.trim());
                    }
                }
            }
            loaded = true;
        }
    }

    /** True for errors meaning "could not reach the listener / service", where trying another URL makes sense. */
    private static boolean isConnectivityError(SQLException e) {
        switch (e.getErrorCode()) {
            case 12514: case 12505: case 12541: case 12154: case 12528: case 12537:
            case 12170: case 12500: case 12519: case 17002: case 17410:
                return true;
            default:
                break;
        }
        String m = String.valueOf(e.getMessage());
        return m.contains("IO Error") || m.contains("Listener refused") || m.contains("Network Adapter")
            || m.contains("Connection refused");
    }

    /** Wrong password / locked / expired: identical on every URL, and repeating it can lock the account. */
    private static boolean isAuthError(SQLException e) {
        int c = e.getErrorCode();
        return c == 1017 || c == 28000 || c == 28001 || c == 1045;
    }

    public static Connection get() throws SQLException {
        load();
        if (System.currentTimeMillis() < authBlockedUntil && authBlockError != null) {
            // Do not hammer Oracle with a bad password: ORA-28000 (account locked) follows after ~10 failures.
            throw authBlockError;
        }
        List<String> candidates = new ArrayList<String>();
        if (activeUrl != null) {
            candidates.add(activeUrl);
        }
        if (!candidates.contains(url)) {
            candidates.add(url);
        }
        for (String f : FALLBACKS) {
            if (!candidates.contains(f)) {
                candidates.add(f);
            }
        }
        SQLException first = null;
        for (String candidate : candidates) {
            try {
                Connection c = DriverManager.getConnection(candidate, user, password);
                if (!candidate.equals(activeUrl)) {
                    activeUrl = candidate;
                    System.out.println("[HMS] Connected to Oracle using " + candidate + " as " + user);
                }
                authBlockedUntil = 0L;
                authBlockError = null;
                return c;
            } catch (SQLException e) {
                if (isAuthError(e)) {
                    authBlockError = e;
                    authBlockedUntil = System.currentTimeMillis() + AUTH_PAUSE_MS;
                    throw e;
                }
                if (first == null) {
                    first = e;
                }
                if (!isConnectivityError(e)) {
                    throw e;
                }
            }
        }
        throw first;
    }

    /** Human-readable summary of the connection settings (never includes the password). */
    public static String describe() {
        try {
            load();
        } catch (SQLException e) {
            return "Not loaded: " + e.getMessage();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("User            : ").append(user).append('\n');
        sb.append("Primary URL     : ").append(url).append('\n');
        sb.append("Fallback URLs   : ").append(FALLBACKS.isEmpty() ? "(none)" : FALLBACKS.toString()).append('\n');
        sb.append("Active URL      : ").append(activeUrl == null ? "(not connected yet)" : activeUrl);
        return sb.toString();
    }


    // ---------------------------------------------------------------- ids from sequences (no triggers needed)

    private static final java.util.regex.Pattern TABLE_NAME = java.util.regex.Pattern.compile("[A-Za-z][A-Za-z0-9_]*");

    private static String seqName(String table) {
        if (table == null || !TABLE_NAME.matcher(table).matches()) {
            throw new IllegalArgumentException("Bad table name: " + table);
        }
        return "hms_seq_" + table.toUpperCase();
    }

    private static long fetchNext(Connection c, String seq) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT " + seq + ".NEXTVAL FROM dual")) {
            rs.next();
            return rs.getLong(1);
        }
    }

    /**
     * Next primary-key value for a table, taken from its hms_seq_<TABLE> sequence. The sequence is created
     * automatically (starting after the highest existing id) if it does not exist yet.
     */
    public static long nextId(Connection c, String table) throws SQLException {
        String seq = seqName(table);
        try {
            return fetchNext(c, seq);
        } catch (SQLException e) {
            if (e.getErrorCode() != 2289) {            // ORA-02289: sequence does not exist
                throw e;
            }
            createSequence(c, table, false);
            return fetchNext(c, seq);
        }
    }

    public static long nextId(String table) throws SQLException {
        try (Connection c = get()) {
            return nextId(c, table);
        }
    }

    /** Creates hms_seq_<TABLE>; starts at 1, or after the table's current maximum id. Ignores "already exists". */
    public static void createSequence(Connection c, String table, boolean fromOne) throws SQLException {
        String seq = seqName(table);
        long start = 1;
        if (!fromOne) {
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT NVL(MAX(id),0)+1 FROM " + table)) {
                rs.next();
                start = Math.max(1, rs.getLong(1));
            }
        }
        try (Statement st = c.createStatement()) {
            st.execute("CREATE SEQUENCE " + seq + " START WITH " + start + " INCREMENT BY 1 NOCACHE");
        } catch (SQLException e) {
            if (e.getErrorCode() != 955) {             // ORA-00955: name already used
                throw e;
            }
        }
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                ps.setNull(i + 1, java.sql.Types.VARCHAR);   // Oracle accepts this for any column type
            } else {
                ps.setObject(i + 1, params[i]);
            }
        }
    }

    public static List<Map<String, String>> query(String sql, Object... params) throws SQLException {
        try (Connection c = get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                List<Map<String, String>> out = new ArrayList<Map<String, String>>();
                while (rs.next()) {
                    Map<String, String> row = new LinkedHashMap<String, String>();
                    for (int i = 1; i <= n; i++) {
                        row.put(md.getColumnLabel(i).toLowerCase(), read(rs, md, i));
                    }
                    out.add(row);
                }
                return out;
            }
        }
    }

    /** Oracle returns upper-case labels and "2026-10-25 00:00:00" for DATE, so values are normalised here. */
    private static String read(ResultSet rs, ResultSetMetaData md, int i) throws SQLException {
        String type = md.getColumnTypeName(i);
        if (type == null) {
            return rs.getString(i);
        }
        type = type.toUpperCase();
        if (type.equals("DATE")) {
            java.sql.Timestamp ts = rs.getTimestamp(i);
            return ts == null ? null : ts.toString().substring(0, 10);          // yyyy-MM-dd
        }
        if (type.startsWith("TIMESTAMP")) {
            java.sql.Timestamp ts = rs.getTimestamp(i);
            return ts == null ? null : ts.toString().substring(0, 16);          // yyyy-MM-dd HH:mm
        }
        if (type.equals("NUMBER") || type.equals("FLOAT")) {
            java.math.BigDecimal b = rs.getBigDecimal(i);
            return b == null ? null : (b.signum() == 0 ? "0" : b.stripTrailingZeros().toPlainString());
        }
        return rs.getString(i);
    }

    public static Map<String, String> one(String sql, Object... params) throws SQLException {
        List<Map<String, String>> rows = query(sql, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** First column of first row, or "0" when nothing is returned / value is NULL. */
    public static String scalar(String sql, Object... params) throws SQLException {
        Map<String, String> row = one(sql, params);
        if (row == null) {
            return "0";
        }
        String v = row.values().iterator().next();
        return v == null ? "0" : v;
    }

    public static int update(String sql, Object... params) throws SQLException {
        try (Connection c = get(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }
}
